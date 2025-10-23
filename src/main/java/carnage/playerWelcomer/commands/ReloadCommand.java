package carnage.playerWelcomer.commands;

import carnage.playerWelcomer.PlayerWelcomer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles the /welcomereload command to reload plugin configuration and data.
 * Optimized with better error handling and user feedback.
 * Uses modern Adventure API for text components.
 */
public class ReloadCommand implements CommandExecutor {
    private static final Component NO_PERMISSION = Component.text("You lack permission to use this command!", NamedTextColor.RED);
    private static final Component RELOAD_STARTED = Component.text("Reloading configuration and data...", NamedTextColor.YELLOW);
    private static final Component RELOAD_SUCCESS = Component.text("Configuration and data reloaded successfully!", NamedTextColor.GREEN);

    private final PlayerWelcomer plugin;

    public ReloadCommand(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("playerwelcomer.admin")) {
            sender.sendMessage(NO_PERMISSION);
            return true;
        }

        // Notify sender that reload has started
        sender.sendMessage(RELOAD_STARTED);

        // Perform reload asynchronously
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> reloadPlugin(sender));

        return true;
    }

    /**
     * Performs the plugin reload operation.
     * Runs asynchronously to avoid blocking the main thread.
     */
    private void reloadPlugin(CommandSender sender) {
        try {
            // Reload configuration
            plugin.getConfigManager().loadConfig();

            // Reset data (this clears all runtime data)
            plugin.getDataManager().resetDataAsync();

            // Send success message on main thread
            plugin.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(RELOAD_SUCCESS);
                plugin.getPluginLogger().info("Configuration and data reloaded by " + sender.getName());
            });

        } catch (Exception e) {
            // Handle errors gracefully
            Component errorMessage = Component.text("Failed to reload: " + e.getMessage(), NamedTextColor.RED);
            Component detailsMessage = Component.text("Check console for details.", NamedTextColor.RED);

            plugin.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(errorMessage);
                sender.sendMessage(detailsMessage);
            });

            plugin.getPluginLogger().severe("Reload failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}