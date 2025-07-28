package carnage.playerWelcomer.listeners;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listens for player join events and handles first-join messages.
 */
public class PlayerJoinListener implements Listener {
    private final PlayerWelcomer plugin;

    public PlayerJoinListener(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player join events, broadcasting a first-join message for new players.
     * Empty lines are rendered as blank lines in the chat.
     * @param event the player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.getDataManager().isNewPlayer(event.getPlayer().getUniqueId())) {
                return;
            }
            String[] messages = plugin.getConfigManager().getFirstJoinMessage();
            for (String message : messages) {
                String formattedMessage = message.replace("%player_name%", event.getPlayer().getName())
                        .replace("%unique_join_count%", String.valueOf(plugin.getDataManager().getUniqueJoinCount() + 1));
                plugin.getServer().broadcastMessage(formattedMessage);
            }
            plugin.getDataManager().recordJoinTime(event.getPlayer().getUniqueId());
        });
    }
}