package carnage.playerWelcomer.commands;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles the /welcomereload command to reload the plugin's configuration and reset data.
 */
public class ReloadCommand implements CommandExecutor {
    private static final String NO_PERMISSION_MESSAGE = ChatColor.RED + "You do not have permission to use this command!";
    private static final String RELOAD_SUCCESS_MESSAGE = ChatColor.GREEN + "PlayerWelcomer configuration and data reloaded successfully!";
    private static final String RELOAD_FAILED_MESSAGE = ChatColor.RED + "Failed to reload configuration or data: ";
    private final PlayerWelcomer plugin;

    public ReloadCommand(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playerwelcomer.admin")) {
            sender.sendMessage(NO_PERMISSION_MESSAGE);
            return true;
        }

        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getConfigManager().loadConfig();
                plugin.getDataManager().resetDataAsync();
                sender.sendMessage(RELOAD_SUCCESS_MESSAGE);
                plugin.getPluginLogger().info("Configuration and data reloaded by " + sender.getName());
            } catch (Exception e) {
                sender.sendMessage(RELOAD_FAILED_MESSAGE + e.getMessage());
                plugin.getPluginLogger().severe("Failed to reload configuration or data: " + e.getMessage());
            }
        });

        return true;
    }
}