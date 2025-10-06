package carnage.playerWelcomer.commands;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles the /welcome command for welcoming new players with rewards.
 */
public class WelcomeCommand implements CommandExecutor {
    private static final String USAGE = "#FF0000Usage: /welcome <player>";
    private static final String PLAYER_ONLY = "#FF0000This command is for players only!";
    private static final String PLAYER_NOT_FOUND = "#FF0000Player not found!";
    private static final String SELF_WELCOME = "#FF0000You cannot welcome yourself!";
    private static final String WELCOME_EXPIRED = "#FF0000This player's welcome period has expired!";
    private static final String COMMAND_DISABLED = "#FF0000The welcome command is disabled!";
    private static final String REWARD_FAILED = "#FF0000Failed to give reward. Contact an administrator.";

    private final PlayerWelcomer plugin;

    public WelcomeCommand(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().processMessage(PLAYER_ONLY));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(plugin.getConfigManager().processMessage(USAGE));
            return true;
        }

        plugin.getScheduler().runTaskAsynchronously(plugin, () -> handleWelcome(player, args[0]));
        return true;
    }

    private void handleWelcome(Player sender, String targetName) {
        if (!plugin.getConfigManager().isWelcomeCommandEnabled()) {
            sendMessage(sender, COMMAND_DISABLED);
            return;
        }

        plugin.getScheduler().runTask(plugin, () -> validateTarget(sender, targetName));
    }

    private void validateTarget(Player sender, String targetName) {
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            sendMessage(sender, PLAYER_NOT_FOUND);
            return;
        }
        if (target.equals(sender)) {
            sendMessage(sender, SELF_WELCOME);
            return;
        }
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> processWelcomeChecks(sender, target));
    }

    private void processWelcomeChecks(Player sender, Player target) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (!plugin.getDataManager().isNewPlayer(targetId)) {
            sendMessage(sender, plugin.getConfigManager().getNoNewPlayersMessage());
            return;
        }

        if (!plugin.getDataManager().isWithinWelcomeWindow(targetId)) {
            sendMessage(sender, WELCOME_EXPIRED);
            return;
        }

        if (plugin.getDataManager().isOnCooldown(senderId)) {
            long remaining = plugin.getDataManager().getRemainingCooldown(senderId);
            String cooldownMsg = plugin.getConfigManager().getCooldownMessage()
                    .replace("%seconds%", String.valueOf(remaining));
            sendMessage(sender, cooldownMsg);
            return;
        }

        executeWelcome(sender, target, senderId, targetId);
    }

    private void executeWelcome(Player sender, Player target, UUID senderId, UUID targetId) {
        broadcastWelcomeMessage(sender, target);
        processReward(sender, targetId, senderId);
    }

    /**
     * Broadcasts the welcome message to all players.
     */
    @SuppressWarnings("deprecation")
    private void broadcastWelcomeMessage(Player sender, Player target) {
        plugin.getScheduler().runTask(plugin, () -> {
            String message = plugin.getConfigManager().getWelcomeMessage()
                    .replace("%target_name%", target.getName())
                    .replace("%player_name%", sender.getName());
            plugin.getServer().broadcastMessage(message);
        });
    }

    private void processReward(Player sender, UUID targetId, UUID senderId) {
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            String rewardType = plugin.getConfigManager().getWelcomeRewardType();
            String currencyType = plugin.getConfigManager().getWelcomeCurrencyType();
            double rewardAmount = plugin.getConfigManager().getWelcomeRewardAmount();
            String crateKeyName = plugin.getConfigManager().getWelcomeCrateKeyName();

            boolean success = plugin.getRewardManager().giveReward(
                    sender, rewardType, currencyType, rewardAmount, crateKeyName);

            plugin.getDataManager().addWelcomedPlayer(targetId);
            plugin.getDataManager().setCooldown(senderId);

            sendRewardResult(sender, success, rewardType, rewardAmount, currencyType, crateKeyName);
        });
    }

    private void sendRewardResult(Player sender, boolean success, String rewardType,
                                  double rewardAmount, String currencyType, String crateKeyName) {
        if (success) {
            String rewardDisplay = plugin.getRewardManager().getRewardDisplay(rewardType, currencyType, crateKeyName);
            String message = plugin.getConfigManager().getWelcomeSuccessMessage(rewardType)
                    .replace("%reward_amount%", String.valueOf((int) rewardAmount))
                    .replace("%reward_display%", rewardDisplay);
            sendMessage(sender, message);
        } else {
            sendMessage(sender, REWARD_FAILED);
        }
    }

    private void sendMessage(Player player, String message) {
        plugin.getScheduler().runTask(plugin, () ->
                player.sendMessage(plugin.getConfigManager().processMessage(message)));
    }
}