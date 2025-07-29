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
        setupRewards();
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
     * Sets up the reward system (currency and crate keys) if enabled.
     */
    private void setupRewards() {
        if (configManager.isWelcomeCommandEnabled()) {
            String rewardType = configManager.getWelcomeRewardType();
            if (rewardType.equalsIgnoreCase("currency")) {
                if (!rewardManager.setupEconomy()) {
                    logger.warning("Vault or economy plugin not found! Currency rewards will be disabled. Ensure Vault and an economy plugin (e.g., EssentialsX) are installed.");
                } else {
                    logger.info("Currency rewards enabled with Vault and economy provider.");
                }
            } else if (rewardType.equalsIgnoreCase("crate_key") && !rewardManager.isExcellentCratesAvailable()) {
                logger.warning("Excellent Crates not found! Crate key rewards will be disabled. Ensure Excellent Crates is installed.");
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