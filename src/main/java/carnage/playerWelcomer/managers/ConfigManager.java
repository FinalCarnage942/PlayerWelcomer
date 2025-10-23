package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages plugin configuration, handling loading and retrieval of settings.
 * All methods are thread-safe for reading after initial load.
 * Implements message caching for improved performance.
 */
public class ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.yml";
    private static final Pattern HEX_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");

    private final PlayerWelcomer plugin;
    private volatile FileConfiguration config;

    // Caching for processed messages
    private final ConcurrentHashMap<String, String> messageCache = new ConcurrentHashMap<>(32);

    // Cache for first-join messages (processed once)
    private volatile String[] cachedFirstJoinMessages;

    // Cache for frequently accessed config values
    private volatile boolean welcomeCommandEnabled;
    private volatile boolean firstJoinEnabled;
    private volatile int welcomeCooldown;
    private volatile String rewardType;
    private volatile String currencyType;
    private volatile double rewardAmount;
    private volatile String crateKeyName;
    private volatile String welcomeMessage;
    private volatile String noNewPlayersMessage;
    private volatile String cooldownMessage;

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

        // Clear caches
        messageCache.clear();
        cachedFirstJoinMessages = null;

        // Load and cache all config values
        loadConfigValues();

        // Validate configuration
        validateConfiguration();
    }

    /**
     * Loads all config values into memory for fast access.
     */
    private void loadConfigValues() {
        firstJoinEnabled = config.getBoolean("first-join.enabled", false);
        welcomeCommandEnabled = config.getBoolean("welcome-command.enabled", true);
        welcomeCooldown = config.getInt("welcome-command.cooldown", 60);
        rewardType = config.getString("welcome-command.reward-type", "currency");
        currencyType = config.getString("welcome-command.reward-currency", "vault");
        rewardAmount = config.getDouble("welcome-command.reward-amount", 100.0);
        crateKeyName = config.getString("welcome-command.crate-key-name", "Test Key");

        // Pre-process and cache messages
        welcomeMessage = processMessageInternal(config.getString(
                "welcome-command.welcome-message",
                "#00FF00Welcome to the server, #FFFF00%target_name%#00FF00! Welcomed by #FFFF00%player_name%"
        ));

        noNewPlayersMessage = processMessageInternal(config.getString(
                "welcome-command.no-new-players",
                "#FF0000That player has already been welcomed!"
        ));

        cooldownMessage = processMessageInternal(config.getString(
                "welcome-command.cooldown-message",
                "#FF0000Please wait %seconds% seconds before using this command again!"
        ));

        plugin.getPluginLogger().info("First-join message enabled: " + firstJoinEnabled);
    }

    /**
     * Validates the configuration for errors.
     */
    private void validateConfiguration() {
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

        if (rewardAmount <= 0) {
            throw new RuntimeException("Reward amount must be positive: " + rewardAmount);
        }
    }

    private void validateCurrencySettings() {
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
     * Uses caching for repeated messages to improve performance.
     */
    public String processMessage(String message) {
        if (message == null) {
            return "";
        }

        // Try to get from cache first
        return messageCache.computeIfAbsent(message, this::processMessageInternal);
    }

    /**
     * Internal method that actually processes the message.
     * Uses BungeeCord's ChatColor for hex support (deprecated but functional).
     */
    @SuppressWarnings("deprecation")
    private String processMessageInternal(String message) {
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

    // Fast cached getters for frequently accessed values
    public boolean isFirstJoinMessageEnabled() {
        return firstJoinEnabled;
    }

    public boolean isWelcomeCommandEnabled() {
        return welcomeCommandEnabled;
    }

    public int getWelcomeCooldown() {
        return welcomeCooldown;
    }

    public String getWelcomeRewardType() {
        return rewardType;
    }

    public String getWelcomeCurrencyType() {
        return currencyType;
    }

    public double getWelcomeRewardAmount() {
        return rewardAmount;
    }

    public String getWelcomeCrateKeyName() {
        return crateKeyName;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public String getNoNewPlayersMessage() {
        return noNewPlayersMessage;
    }

    public String getCooldownMessage() {
        return cooldownMessage;
    }

    /**
     * Gets the first join messages, processing and caching them on first access.
     */
    public String[] getFirstJoinMessage() {
        if (cachedFirstJoinMessages == null) {
            synchronized (this) {
                if (cachedFirstJoinMessages == null) {
                    cachedFirstJoinMessages = Arrays.stream(getFirstJoinMessageRaw())
                            .map(this::processMessageInternal)
                            .toArray(String[]::new);
                }
            }
        }
        return cachedFirstJoinMessages;
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

    public String getWelcomeSuccessMessage(String rewardType) {
        String defaultMessage = rewardType.equalsIgnoreCase("crate_key") ?
                "#00FF00You welcomed a new player and received #ADD8E6%reward_amount% %reward_display%!" :
                "#00FF00You welcomed a new player and received #ADD8E6%reward_amount% %reward_display%!";

        return processMessage(config.getString("welcome-command.success-message", defaultMessage));
    }
}