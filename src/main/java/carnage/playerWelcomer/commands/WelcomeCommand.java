package carnage.playerWelcomer.commands;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handles the /welcome command, allowing players to welcome new players with rewards.
 */
public class WelcomeCommand implements CommandExecutor {
    private static final String USAGE_MESSAGE = "&cUsage: /welcome <player>";
    private static final String PLAYER_ONLY_MESSAGE = "&cThis command can only be used by players!";
    private static final String PLAYER_NOT_FOUND_MESSAGE = "&cPlayer not found!";
    private static final String SELF_WELCOME_MESSAGE = "&cYou cannot welcome yourself!";
    private static final String WELCOME_EXPIRED_MESSAGE = "&cThis player can no longer be welcomed!";
    private final PlayerWelcomer plugin;

    public WelcomeCommand(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().processMessage(PLAYER_ONLY_MESSAGE));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(plugin.getConfigManager().processMessage(USAGE_MESSAGE));
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
                player.sendMessage(plugin.getConfigManager().processMessage("&cThe welcome command is disabled!"));
                return;
            }

            Player target = plugin.getServer().getPlayer(targetName);
            if (target == null) {
                player.sendMessage(plugin.getConfigManager().processMessage(PLAYER_NOT_FOUND_MESSAGE));
                return;
            }

            if (target.equals(player)) {
                player.sendMessage(plugin.getConfigManager().processMessage(SELF_WELCOME_MESSAGE));
                return;
            }

            UUID targetId = target.getUniqueId();
            if (!plugin.getDataManager().isNewPlayer(targetId)) {
                player.sendMessage(plugin.getConfigManager().getNoNewPlayersMessage());
                return;
            }

            if (!plugin.getDataManager().isWithinWelcomeWindow(targetId)) {
                player.sendMessage(plugin.getConfigManager().processMessage(WELCOME_EXPIRED_MESSAGE));
                return;
            }

            if (plugin.getDataManager().isOnCooldown(player.getUniqueId())) {
                long remaining = plugin.getDataManager().getRemainingCooldown(player.getUniqueId());
                player.sendMessage(plugin.getConfigManager().getCooldownMessage()
                        .replace("%seconds%", String.valueOf(remaining)));
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
        plugin.getServer().broadcastMessage(welcomeMessage);
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
        sender.sendMessage(plugin.getConfigManager().getWelcomeSuccessMessage()
                .replace("%reward_amount%", String.valueOf(reward)));
    }
}