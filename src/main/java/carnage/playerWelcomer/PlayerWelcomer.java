package carnage.playerWelcomer;

import carnage.playerWelcomer.commands.WelcomeCommand;
import carnage.playerWelcomer.listeners.PlayerJoinListener;
import carnage.playerWelcomer.managers.ConfigManager;
import carnage.playerWelcomer.managers.DataManager;
import carnage.playerWelcomer.managers.RewardManager;
import carnage.playerWelcomer.commands.ReloadCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

/**
 * Main plugin class for PlayerWelcomer, responsible for initializing and managing plugin components.
 */
public final class PlayerWelcomer extends JavaPlugin {
    private ConfigManager configManager;
    private DataManager dataManager;
    private RewardManager rewardManager;
    private Logger logger;
    private BukkitScheduler scheduler;

    @Override
    public void onEnable() {
        initializeComponents();
        if (!loadConfiguration()) {
            disablePlugin();
            return;
        }
        validateRewardAvailability();
        registerComponents();
        logger.info("PlayerWelcomer enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveDataAsync();
        }
        logger.info("PlayerWelcomer disabled successfully!");
    }

    /**
     * Initializes core plugin components.
     */
    private void initializeComponents() {
        logger = getLogger();
        scheduler = getServer().getScheduler();
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        rewardManager = new RewardManager(this);
    }

    /**
     * Loads the plugin configuration.
     * @return true if configuration loaded successfully, false otherwise
     */
    private boolean loadConfiguration() {
        try {
            configManager.loadConfig();
            return true;
        } catch (Exception e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validates the availability of the configured reward system.
     */
    private void validateRewardAvailability() {
        if (configManager.isWelcomeCommandEnabled()) {
            String rewardType = configManager.getWelcomeRewardType();
            String currencyType = configManager.getWelcomeCurrencyType();
            if (!rewardManager.isRewardAvailable(rewardType, currencyType)) {
                logger.warning("Configured reward type '" + rewardType + "' with currency '" + currencyType + "' is not available. Rewards may fail.");
            } else {
                logger.info("Reward type '" + rewardType + "' with currency '" + currencyType + "' is available.");
            }
        }
    }

    /**
     * Registers event listeners and commands based on configuration.
     */
    private void registerComponents() {
        if (configManager.isFirstJoinMessageEnabled()) {
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        }
        if (configManager.isWelcomeCommandEnabled()) {
            getCommand("welcome").setExecutor(new WelcomeCommand(this));
            getCommand("welcomereload").setExecutor(new ReloadCommand(this));
        }
    }

    /**
     * Disables the plugin due to critical failure.
     */
    private void disablePlugin() {
        setEnabled(false);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public Logger getPluginLogger() {
        return logger;
    }

    public BukkitScheduler getScheduler() {
        return scheduler;
    }
}