package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages plugin configuration, handling loading and retrieval of settings.
 * All methods are thread-safe for reading after initial load.
 */
public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final Pattern HEX_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");

    private final PlayerWelcomer plugin;
    private volatile FileConfiguration config;

    public ConfigManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the configuration file, creating it from defaults if necessary.
     * Must be called from main thread.
     */
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);

        if (!configFile.exists()) {
            plugin.saveResource(CONFIG_FILE_NAME, false);
            plugin.getPluginLogger().info("Created default config.yml");
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        boolean firstJoinEnabled = isFirstJoinMessageEnabled();
        plugin.getPluginLogger().info("First-join message enabled: " + firstJoinEnabled);

        if (firstJoinEnabled) {
            validateFirstJoinMessageLines();
        }

        validateRewardSettings();
    }

    private void validateFirstJoinMessageLines() {
        int expectedLineCount = config.getInt("first-join.line_count", 5);
        String[] messages = getFirstJoinMessageRaw();

        if (messages.length != expectedLineCount) {
            throw new RuntimeException(
                    "First-join message has " + messages.length +
                            " lines, but line_count is set to " + expectedLineCount
            );
        }
    }

    private void validateRewardSettings() {
        String rewardType = getWelcomeRewardType();

        if (!rewardType.equalsIgnoreCase("currency") &&
                !rewardType.equalsIgnoreCase("crate_key")) {
            throw new RuntimeException(
                    "Invalid reward-type: " + rewardType +
                            ". Must be 'currency' or 'crate_key'."
            );
        }

        if (rewardType.equalsIgnoreCase("currency")) {
            validateCurrencySettings();
        }

        double rewardAmount = getWelcomeRewardAmount();
        if (rewardAmount <= 0) {
            throw new RuntimeException("Reward amount must be positive: " + rewardAmount);
        }
    }

    private void validateCurrencySettings() {
        String currencyType = getWelcomeCurrencyType();

        if (currencyType == null || currencyType.trim().isEmpty()) {
            throw new RuntimeException(
                    "Currency type is missing or empty for reward-type 'currency'."
            );
        }

        if (currencyType.toLowerCase().startsWith("coinsengine:") &&
                currencyType.length() <= "coinsengine:".length()) {
            throw new RuntimeException(
                    "Invalid CoinsEngine currency ID in reward-currency: " + currencyType
            );
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
     * Processes a message to translate hex and legacy color codes.
     * Uses BungeeCord's ChatColor for hex support (deprecated but functional).
     */
    @SuppressWarnings("deprecation")
    public String processMessage(String message) {
        if (message == null) {
            return "";
        }

        // First translate hex color codes using BungeeCord's method
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group();
            try {
                ChatColor color = ChatColor.of(hexCode);
                matcher.appendReplacement(buffer, color.toString());
            } catch (IllegalArgumentException e) {
                // Invalid hex code, leave as is
                matcher.appendReplacement(buffer, hexCode);
            }
        }
        matcher.appendTail(buffer);

        // Then translate legacy color codes using Bukkit's method
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public boolean isFirstJoinMessageEnabled() {
        return config.getBoolean("first-join.enabled", false);
    }

    private String[] getFirstJoinMessageRaw() {
        return new String[] {
                config.getString("first-join.message.line1", "#808080&m===================="),
                config.getString("first-join.message.line2", ""),
                config.getString("first-join.message.line3",
                        "#00FF00&lWelcome #FFFF00%player_name% #808080to the server! #808080[&f#%unique_join_count%#808080]"),
                config.getString("first-join.message.line4", ""),
                config.getString("first-join.message.line5", "#808080&m====================")
        };
    }

    public String[] getFirstJoinMessage() {
        return Arrays.stream(getFirstJoinMessageRaw())
                .map(this::processMessage)
                .toArray(String[]::new);
    }

    public boolean isWelcomeCommandEnabled() {
        return config.getBoolean("welcome-command.enabled", true);
    }

    public int getWelcomeCooldown() {
        return config.getInt("welcome-command.cooldown", 60);
    }

    public String getWelcomeRewardType() {
        return config.getString("welcome-command.reward-type", "currency");
    }

    public String getWelcomeCurrencyType() {
        return config.getString("welcome-command.reward-currency", "vault");
    }

    public double getWelcomeRewardAmount() {
        return config.getDouble("welcome-command.reward-amount", 100.0);
    }

    public String getWelcomeCrateKeyName() {
        return config.getString("welcome-command.crate-key-name", "Test Key");
    }

    public String getWelcomeMessage() {
        return processMessage(config.getString(
                "welcome-command.welcome-message",
                "#00FF00Welcome to the server, #FFFF00%target_name%#00FF00! Welcomed by #FFFF00%player_name%"
        ));
    }

    public String getWelcomeSuccessMessage(String rewardType) {
        String defaultMessage = rewardType.equalsIgnoreCase("crate_key") ?
                "#00FF00You welcomed a new player and received #ADD8E6%reward_amount% %reward_display%!" :
                "#00FF00You welcomed a new player and received #ADD8E6%reward_amount% %reward_display%!";

        return processMessage(config.getString("welcome-command.success-message", defaultMessage));
    }

    public String getNoNewPlayersMessage() {
        return processMessage(config.getString(
                "welcome-command.no-new-players",
                "#FF0000That player has already been welcomed!"
        ));
    }

    public String getCooldownMessage() {
        return processMessage(config.getString(
                "welcome-command.cooldown-message",
                "#FF0000Please wait %seconds% seconds before using this command again!"
        ));
    }
}