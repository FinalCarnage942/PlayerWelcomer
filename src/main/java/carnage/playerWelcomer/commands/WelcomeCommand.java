package carnage.playerWelcomer.commands;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles the /welcome command for welcoming new players with rewards.
 * Optimized to minimize thread switches and improve performance.
 */
public class WelcomeCommand implements CommandExecutor {
    private static final String USAGE = "#FF0000Usage: /welcome <player>";
    private static final String PLAYER_ONLY = "#FF0000This command is for players only!";
    private static final String PLAYER_NOT_FOUND = "#FF0000Player not found!";
    private static final String SELF_WELCOME = "#FF0000You cannot welcome yourself!";
    private static final String WELCOME_EXPIRED = "#FF0000This player's welcome period has expired!";
    private static final String COMMAND_DISABLED = "#FF0000The welcome command is disabled!";
    private static final String REWARD_FAILED = "#FF0000Failed to give reward. Contact an administrator.";
    private static final String RATE_LIMIT = "#FF0000Please slow down! You're using this command too quickly.";

    private final PlayerWelcomer plugin;
    private final RateLimiter rateLimiter;

    public WelcomeCommand(PlayerWelcomer plugin) {
        this.plugin = plugin;
        this.rateLimiter = new RateLimiter(3, 1000L); // 3 commands per second per player
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Validate sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().processMessage(PLAYER_ONLY));
            return true;
        }

        // Validate command arguments
        if (args.length != 1) {
            player.sendMessage(plugin.getConfigManager().processMessage(USAGE));
            return true;
        }

        // Rate limiting check
        if (!rateLimiter.tryAcquire(player.getUniqueId().toString())) {
            player.sendMessage(plugin.getConfigManager().processMessage(RATE_LIMIT));
            return true;
        }

        // Check if command is enabled (cached, fast check)
        if (!plugin.getConfigManager().isWelcomeCommandEnabled()) {
            player.sendMessage(plugin.getConfigManager().processMessage(COMMAND_DISABLED));
            return true;
        }

        // Get target player (must be on main thread for Bukkit API)
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().processMessage(PLAYER_NOT_FOUND));
            return true;
        }

        // Validate not welcoming self
        if (target.equals(player)) {
            player.sendMessage(plugin.getConfigManager().processMessage(SELF_WELCOME));
            return true;
        }

        // All validations passed - process async in a single task
        final String targetName = target.getName();
        final UUID senderId = player.getUniqueId();
        final UUID targetId = target.getUniqueId();

        plugin.getScheduler().runTaskAsynchronously(plugin, () ->
                processWelcome(player, target, senderId, targetId, targetName));

        return true;
    }

    /**
     * Processes the welcome command entirely in async context.
     * Only switches to main thread once for broadcast and reward giving.
     */
    private void processWelcome(Player sender, Player target, UUID senderId, UUID targetId, String targetName) {
        // All data checks in async (these are thread-safe)
        if (!plugin.getDataManager().isNewPlayer(targetId)) {
            sendMessageAsync(sender, plugin.getConfigManager().getNoNewPlayersMessage());
            return;
        }

        if (!plugin.getDataManager().isWithinWelcomeWindow(targetId)) {
            sendMessageAsync(sender, WELCOME_EXPIRED);
            return;
        }

        if (plugin.getDataManager().isOnCooldown(senderId)) {
            long remaining = plugin.getDataManager().getRemainingCooldown(senderId);
            String cooldownMsg = plugin.getConfigManager().getCooldownMessage()
                    .replace("%seconds%", String.valueOf(remaining));
            sendMessageAsync(sender, cooldownMsg);
            return;
        }

        // Prepare reward data (all config access is thread-safe)
        String rewardType = plugin.getConfigManager().getWelcomeRewardType();
        String currencyType = plugin.getConfigManager().getWelcomeCurrencyType();
        double rewardAmount = plugin.getConfigManager().getWelcomeRewardAmount();
        String crateKeyName = plugin.getConfigManager().getWelcomeCrateKeyName();

        // Update data (thread-safe operations)
        plugin.getDataManager().addWelcomedPlayer(targetId);
        plugin.getDataManager().setCooldown(senderId);

        // Switch to main thread ONCE for broadcast and reward
        plugin.getScheduler().runTask(plugin, () ->
                executeWelcomeSync(sender, target, senderId, rewardType, currencyType, rewardAmount, crateKeyName));
    }

    /**
     * Executes welcome actions that require main thread (broadcast, rewards).
     */
    private void executeWelcomeSync(Player sender, Player target, UUID senderId,
                                    String rewardType, String currencyType,
                                    double rewardAmount, String crateKeyName) {
        // Broadcast welcome message using Adventure API
        String message = plugin.getConfigManager().getWelcomeMessage()
                .replace("%target_name%", target.getName())
                .replace("%player_name%", sender.getName());

        // Use Adventure API for modern message broadcasting
        net.kyori.adventure.text.Component component =
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message);
        plugin.getServer().broadcast(component);

        // Give reward
        boolean success = plugin.getRewardManager().giveReward(
                sender, rewardType, currencyType, rewardAmount, crateKeyName);

        // Send result message
        if (success) {
            String rewardDisplay = plugin.getRewardManager().getRewardDisplay(rewardType, currencyType, crateKeyName);
            String successMsg = plugin.getConfigManager().getWelcomeSuccessMessage(rewardType)
                    .replace("%reward_amount%", String.valueOf((int) rewardAmount))
                    .replace("%reward_display%", rewardDisplay);
            sender.sendMessage(successMsg);
        } else {
            sender.sendMessage(plugin.getConfigManager().processMessage(REWARD_FAILED));
        }
    }

    /**
     * Sends a message to a player, switching to main thread if needed.
     */
    private void sendMessageAsync(Player player, String message) {
        String processed = plugin.getConfigManager().processMessage(message);
        plugin.getScheduler().runTask(plugin, () -> player.sendMessage(processed));
    }

    /**
     * Simple rate limiter to prevent command spam.
     */
    private static class RateLimiter {
        private final ConcurrentHashMap<String, Queue<Long>> requests = new ConcurrentHashMap<>();
        private final int maxRequests;
        private final long windowMs;

        public RateLimiter(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
        }

        public boolean tryAcquire(String key) {
            long now = System.currentTimeMillis();
            Queue<Long> timestamps = requests.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());

            // Remove expired timestamps
            timestamps.removeIf(time -> now - time > windowMs);

            // Check if limit exceeded
            if (timestamps.size() >= maxRequests) {
                return false;
            }

            timestamps.offer(now);
            return true;
        }
    }
}