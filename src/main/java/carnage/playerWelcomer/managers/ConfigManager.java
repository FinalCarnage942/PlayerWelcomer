package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages plugin configuration, handling loading and retrieval of settings.
 * Adheres to SRP by focusing solely on configuration management.
 */
public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final Pattern HEX_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");
    private final PlayerWelcomer plugin;
    private FileConfiguration config;

    public ConfigManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the configuration file, creating it from defaults if necessary.
     * Validates the number of first-join message lines against line_count.
     * @throws RuntimeException if loading or validation fails
     */
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            plugin.saveResource(CONFIG_FILE_NAME, false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        validateFirstJoinMessageLines();
        validateRewardSettings();
    }

    /**
     * Validates that the number of configured first-join message lines matches line_count.
     * Empty lines ("") are valid and counted as lines.
     * @throws RuntimeException if validation fails
     */
    private void validateFirstJoinMessageLines() {
        int expectedLineCount = config.getInt("first-join.line_count", 5);
        String[] messages = getFirstJoinMessageRaw();
        if (messages.length != expectedLineCount) {
            throw new RuntimeException("First-join message has " + messages.length + " lines, but line_count is set to " + expectedLineCount);
        }
    }

    /**
     * Validates reward settings in the configuration.
     * @throws RuntimeException if validation fails
     */
    private void validateRewardSettings() {
        String rewardType = getWelcomeRewardType();
        if (!rewardType.equalsIgnoreCase("currency") && !rewardType.equalsIgnoreCase("crate_key")) {
            throw new RuntimeException("Invalid reward-type: " + rewardType + ". Must be 'currency' or 'crate_key'.");
        }
        if (rewardType.equalsIgnoreCase("currency")) {
            String currencyType = getWelcomeCurrencyType();
            if (currencyType == null || currencyType.trim().isEmpty()) {
                throw new RuntimeException("Currency type is missing or empty for reward-type 'currency'.");
            }
            if (currencyType.toLowerCase().startsWith("coinsengine:") && currencyType.length() <= "coinsengine:".length()) {
                throw new RuntimeException("Invalid CoinsEngine currency ID in reward-currency: " + currencyType);
            }
        }
        double rewardAmount = getWelcomeRewardAmount();
        if (rewardAmount <= 0) {
            throw new RuntimeException("Reward amount must be positive: " + rewardAmount);
        }
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
     * Processes a message to translate hex color codes and legacy color codes.
     * @param message the input message, may be null
     * @return processed message with translated color codes
     */
    public String processMessage(String message) {
        if (message == null) return "";
        // Translate hex color codes first
        Matcher matcher = HEX_PATTERN.matcher(message);
        String processed = matcher.replaceAll(match -> ChatColor.COLOR_CHAR + "x" +
                match.group().substring(1).replaceAll("(.)", ChatColor.COLOR_CHAR + "$1"));
        // Then translate legacy color codes
        return ChatColor.translateAlternateColorCodes('&', processed);
    }

    /**
     * Checks if the first-join message is enabled.
     * @return true if enabled, false otherwise
     */
    public boolean isFirstJoinMessageEnabled() {
        return config.getBoolean("first-join.enabled", true);
    }

    /**
     * Retrieves the raw multi-line first-join message without processing.
     * @return array of raw message lines
     */
    private String[] getFirstJoinMessageRaw() {
        return new String[] {
                config.getString("first-join.message.line1", "#808080&m===================="),
                config.getString("first-join.message.line2", ""),
                config.getString("first-join.message.line3", "#00FF00&lWelcome #FFFF00%player_name% #808080to the server! #808080[&f#%unique_join_count%#808080]"),
                config.getString("first-join.message.line4", ""),
                config.getString("first-join.message.line5", "#808080&m====================")
        };
    }

    /**
     * Retrieves the multi-line first-join message with processed color codes.
     * Empty lines ("") are preserved as blank lines in the output.
     * @return array of processed message lines
     */
    public String[] getFirstJoinMessage() {
        return Arrays.stream(getFirstJoinMessageRaw())
                .map(this::processMessage)
                .toArray(String[]::new);
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
     * Gets the reward type for the welcome command ("currency" or "crate_key").
     * @return reward type
     */
    public String getWelcomeRewardType() {
        return config.getString("welcome-command.reward-type", "currency");
    }

    /**
     * Gets the currency type for currency rewards (e.g., "vault", "playerpoints", "coinsengine:gold").
     * @return currency type
     */
    public String getWelcomeCurrencyType() {
        return config.getString("welcome-command.reward-currency", "vault");
    }

    /**
     * Gets the reward amount for the welcome command.
     * @return reward amount
     */
    public double getWelcomeRewardAmount() {
        return config.getDouble("welcome-command.reward-amount", 100.0);
    }

    /**
     * Gets the crate key name for the welcome command when reward-type is "crate_key".
     * @return crate key name
     */
    public String getWelcomeCrateKeyName() {
        return config.getString("welcome-command.crate-key-name", "Test Key");
    }

    /**
     * Gets the welcome message for the /welcome command with processed color codes.
     * @return welcome message with placeholders
     */
    public String getWelcomeMessage() {
        return processMessage(config.getString("welcome-command.welcome-message", "#00FF00Welcome to the server, #FFFF00%target_name%#00FF00! Welcomed by #FFFF00%player_name%"));
    }

    /**
     * Gets the success message for the welcome command with processed color codes.
     * Dynamically adjusts based on reward-type.
     * @param rewardType the type of reward ("currency" or "crate_key")
     * @return success message with placeholders
     */
    public String getWelcomeSuccessMessage(String rewardType) {
        String defaultMessage = rewardType.equalsIgnoreCase("crate_key") ?
                "#00FF00You welcomed a new player and received #ADD8E6%reward_amount% %reward_display%!" :
                "#00FF00You welcomed a new player and received #ADD8E6%reward_amount% %reward_display%!";
        return processMessage(config.getString("welcome-command.success-message", defaultMessage));
    }

    /**
     * Gets the message for when no new players are available with processed color codes.
     * @return no new players message
     */
    public String getNoNewPlayersMessage() {
        return processMessage(config.getString("welcome-command.no-new-players", "#FF0000That player has already been welcomed!"));
    }

    /**
     * Gets the cooldown message for the welcome command with processed color codes.
     * @return cooldown message with placeholders
     */
    public String getCooldownMessage() {
        return processMessage(config.getString("welcome-command.cooldown-message", "#FF0000Please wait %seconds% seconds before using this command again!"));
    }
}