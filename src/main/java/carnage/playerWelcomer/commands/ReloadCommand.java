package carnage.playerWelcomer.commands;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles the /welcomereload command to reload plugin configuration and data.
 */
public class ReloadCommand implements CommandExecutor {
    private static final String NO_PERMISSION = ChatColor.RED + "You lack permission to use this command!";
    private static final String RELOAD_SUCCESS = ChatColor.GREEN + "Configuration and data reloaded successfully!";
    private static final String RELOAD_FAILED = ChatColor.RED + "Failed to reload: ";

    private final PlayerWelcomer plugin;

    public ReloadCommand(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playerwelcomer.admin")) {
            sender.sendMessage(NO_PERMISSION);
            return true;
        }

        plugin.getScheduler().runTaskAsynchronously(plugin, () -> reloadPlugin(sender));
        return true;
    }

    private void reloadPlugin(CommandSender sender) {
        try {
            plugin.getConfigManager().loadConfig();
            plugin.getDataManager().resetDataAsync();
            sender.sendMessage(RELOAD_SUCCESS);
            plugin.getPluginLogger().info("Reloaded by " + sender.getName());
        } catch (Exception e) {
            String errorMessage = RELOAD_FAILED + e.getMessage();
            sender.sendMessage(errorMessage);
            plugin.getPluginLogger().severe(errorMessage);
        }
    }
}