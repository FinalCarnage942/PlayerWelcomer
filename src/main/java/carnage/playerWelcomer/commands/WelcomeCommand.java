package carnage.playerWelcomer.commands;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles the /welcome command, allowing players to welcome new players with rewards.
 */
public class WelcomeCommand implements CommandExecutor {
    private static final String USAGE_MESSAGE = ChatColor.RED + "Usage: /welcome <player>";
    private static final String PLAYER_ONLY_MESSAGE = ChatColor.RED + "This command can only be used by players!";
    private static final String PLAYER_NOT_FOUND_MESSAGE = ChatColor.RED + "Player not found!";
    private static final String SELF_WELCOME_MESSAGE = ChatColor.RED + "You cannot welcome yourself!";
    private static final String WELCOME_EXPIRED_MESSAGE = ChatColor.RED + "This player can no longer be welcomed!";
    private final PlayerWelcomer plugin;

    public WelcomeCommand(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PLAYER_ONLY_MESSAGE);
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(USAGE_MESSAGE);
            return true;
        }

        Player player = (Player) sender;
        executeWelcomeAsync(player, args[0]);
        return true;
    }

    /**
     * Executes the welcome command logic asynchronously.
     * @param player the command sender
     * @param targetName the name of the target player
     */
    private void executeWelcomeAsync(Player player, String targetName) {
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.getConfigManager().isWelcomeCommandEnabled()) {
                player.sendMessage(ChatColor.RED + "The welcome command is disabled!");
                return;
            }

            Player target = plugin.getServer().getPlayer(targetName);
            if (target == null) {
                player.sendMessage(PLAYER_NOT_FOUND_MESSAGE);
                return;
            }

            if (target.equals(player)) {
                player.sendMessage(SELF_WELCOME_MESSAGE);
                return;
            }

            UUID targetId = target.getUniqueId();
            if (!plugin.getDataManager().isNewPlayer(targetId)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getNoNewPlayersMessage()));
                return;
            }

            if (!plugin.getDataManager().isWithinWelcomeWindow(targetId)) {
                player.sendMessage(WELCOME_EXPIRED_MESSAGE);
                return;
            }

            if (plugin.getDataManager().isOnCooldown(player.getUniqueId())) {
                long remaining = plugin.getDataManager().getRemainingCooldown(player.getUniqueId());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getCooldownMessage()
                                .replace("%seconds%", String.valueOf(remaining))));
                return;
            }

            broadcastWelcomeMessage(player, target);
            rewardAndUpdatePlayer(player, targetId);
        });
    }

    /**
     * Broadcasts the welcome message to all players.
     * @param sender the player who executed the command
     * @param target the welcomed player
     */
    private void broadcastWelcomeMessage(Player sender, Player target) {
        String welcomeMessage = plugin.getConfigManager().getWelcomeMessage()
                .replace("%target_name%", target.getName())
                .replace("%player_name%", sender.getName());
        plugin.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', welcomeMessage));
    }

    /**
     * Rewards the sender and updates data for the welcomed player.
     * @param sender the player who executed the command
     * @param targetId the UUID of the welcomed player
     */
    private void rewardAndUpdatePlayer(Player sender, UUID targetId) {
        double reward = plugin.getConfigManager().getWelcomeReward();
        plugin.getEconomyManager().giveReward(sender, reward);
        plugin.getDataManager().addWelcomedPlayer(targetId);
        plugin.getDataManager().setCooldown(sender.getUniqueId());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getWelcomeSuccessMessage()
                        .replace("%reward_amount%", String.valueOf(reward))));
    }
}