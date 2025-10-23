package carnage.playerWelcomer;

import carnage.playerWelcomer.commands.ReloadCommand;
import carnage.playerWelcomer.commands.WelcomeCommand;
import carnage.playerWelcomer.listeners.PlayerJoinListener;
import carnage.playerWelcomer.managers.ConfigManager;
import carnage.playerWelcomer.managers.DataManager;
import carnage.playerWelcomer.managers.RewardManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import java.util.logging.Logger;

/**
 * Main plugin class for PlayerWelcomer, coordinating plugin initialization and component management.
 * Optimized with proper lifecycle management and error handling.
 */
public final class PlayerWelcomer extends JavaPlugin {
    private ConfigManager configManager;
    private DataManager dataManager;
    private RewardManager rewardManager;
    private Logger logger;
    private BukkitScheduler scheduler;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        initializeComponents();

        if (!loadConfiguration()) {
            logger.severe("Failed to load configuration! Disabling plugin...");
            disablePlugin();
            return;
        }

        validateRewardSystem();
        registerComponents();

        long loadTime = System.currentTimeMillis() - startTime;
        logger.info("PlayerWelcomer enabled successfully in " + loadTime + "ms!");
    }

    @Override
    public void onDisable() {
        logger.info("Shutting down PlayerWelcomer...");

        // Proper shutdown sequence
        shutdownComponents();

        logger.info("PlayerWelcomer disabled successfully!");
    }

    /**
     * Initializes all plugin components.
     */
    private void initializeComponents() {
        logger = getLogger();
        scheduler = getServer().getScheduler();

        try {
            configManager = new ConfigManager(this);
            dataManager = new DataManager(this);
            rewardManager = new RewardManager(this);
        } catch (Exception e) {
            logger.severe("Failed to initialize components: " + e.getMessage());
            throw new RuntimeException("Component initialization failed", e);
        }
    }

    /**
     * Loads and validates the configuration.
     * @return true if successful, false otherwise
     */
    private boolean loadConfiguration() {
        try {
            configManager.loadConfig();
            return true;
        } catch (Exception e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Validates that the reward system is properly configured.
     */
    private void validateRewardSystem() {
        if (!configManager.isWelcomeCommandEnabled()) {
            logger.info("Welcome command is disabled in config");
            return;
        }

        String rewardType = configManager.getWelcomeRewardType();
        String currencyType = configManager.getWelcomeCurrencyType();

        if (!rewardManager.isRewardAvailable(rewardType, currencyType)) {
            logger.warning(String.format(
                    "Reward type '%s' with currency '%s' is not available! " +
                            "Ensure required plugins are installed.",
                    rewardType, currencyType
            ));
        } else {
            logger.info(String.format(
                    "Reward system validated: %s rewards using %s",
                    rewardType, currencyType
            ));
        }
    }

    /**
     * Registers all event listeners and commands.
     */
    private void registerComponents() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Log first-join status
        if (configManager.isFirstJoinMessageEnabled()) {
            logger.info("First-join messages are enabled");
        } else {
            logger.info("First-join messages are disabled; only join times will be recorded");
        }

        // Register commands if welcome command is enabled
        if (configManager.isWelcomeCommandEnabled()) {
            registerCommands();
            logger.info("Registered /welcome and /welcomereload commands");
        } else {
            logger.info("Welcome command is disabled; commands not registered");
        }
    }

    /**
     * Registers plugin commands.
     */
    private void registerCommands() {
        try {
            getCommand("welcome").setExecutor(new WelcomeCommand(this));
            getCommand("welcomereload").setExecutor(new ReloadCommand(this));
        } catch (Exception e) {
            logger.severe("Failed to register commands: " + e.getMessage());
        }
    }

    /**
     * Properly shuts down all plugin components.
     */
    private void shutdownComponents() {
        // Cancel all running tasks
        if (scheduler != null) {
            scheduler.cancelTasks(this);
        }

        // Shutdown data manager (saves data and stops cleanup tasks)
        if (dataManager != null) {
            try {
                dataManager.shutdown();
                dataManager.saveDataAsync();
                // Give it a moment to save
                Thread.sleep(100);
            } catch (Exception e) {
                logger.warning("Error during data manager shutdown: " + e.getMessage());
            }
        }
    }

    /**
     * Disables the plugin safely.
     */
    private void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    // Getters for managers
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