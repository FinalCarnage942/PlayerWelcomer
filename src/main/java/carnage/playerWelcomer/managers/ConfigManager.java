package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Manages plugin configuration, handling loading and retrieval of settings.
 */
public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.yml";
    private final PlayerWelcomer plugin;
    private FileConfiguration config;

    public ConfigManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the configuration file, creating it from defaults if necessary.
     * @throws RuntimeException if loading fails
     */
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            plugin.saveResource(CONFIG_FILE_NAME, false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Saves the configuration file.
     */
    public void saveConfig() {
        File configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Checks if the first-join message is enabled.
     * @return true if enabled, false otherwise
     */
    public boolean isFirstJoinMessageEnabled() {
        return config.getBoolean("first-join.enabled", true);
    }

    /**
     * Grabs the multi-line first-join message with placeholders.
     * @return array of message lines
     */
    public String[] getFirstJoinMessage() {
        return new String[] {
                config.getString("first-join.message.line1", "&8&m===================="),
                config.getString("first-join.message.line2", "&a&lWelcome &e%player_name% &7to the server!"),
                config.getString("first-join.message.line3", "&7First join! &8[&c%unique_join_count%&8]"),
                config.getString("first-join.message.line4", "&8&m====================")
        };
    }

    /**
     * Checks if the welcome command is enabled.
     * @return true if enabled, false otherwise
     */
    public boolean isWelcomeCommandEnabled() {
        return config.getBoolean("welcome-command.enabled", true);
    }

    /**
     * Gets the cooldown duration for the welcome command in seconds.
     * @return cooldown duration
     */
    public int getWelcomeCooldown() {
        return config.getInt("welcome-command.cooldown", 60);
    }

    /**
     * Gets the reward amount for the welcome command.
     * @return reward amount
     */
    public double getWelcomeReward() {
        return config.getDouble("welcome-command.reward-amount", 100.0);
    }

    /**
     * Gets the welcome message for the /welcome command.
     * @return welcome message with placeholders
     */
    public String getWelcomeMessage() {
        return config.getString("welcome-command.welcome-message", "&aWelcome to the server, &e%target_name%&a! Welcomed by &e%player_name%");
    }

    /**
     * Gets the success message for the welcome command.
     * @return success message with placeholders
     */
    public String getWelcomeSuccessMessage() {
        return config.getString("welcome-command.success-message", "&aYou welcomed a new player and received %reward_amount% coins!");
    }

    /**
     * Gets the message for when no new players are available.
     * @return no new players message
     */
    public String getNoNewPlayersMessage() {
        return config.getString("welcome-command.no-new-players", "&cThat player has already been welcomed!");
    }

    /**
     * Gets the cooldown message for the welcome command.
     * @return cooldown message with placeholders
     */
    public String getCooldownMessage() {
        return config.getString("welcome-command.cooldown-message", "&cPlease wait %seconds% seconds before using this command again!");
    }
}