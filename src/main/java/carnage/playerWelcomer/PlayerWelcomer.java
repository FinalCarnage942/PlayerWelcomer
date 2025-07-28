package carnage.playerWelcomer;

import carnage.playerWelcomer.commands.WelcomeCommand;
import carnage.playerWelcomer.listeners.PlayerJoinListener;
import carnage.playerWelcomer.managers.ConfigManager;
import carnage.playerWelcomer.managers.DataManager;
import carnage.playerWelcomer.managers.EconomyManager;
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
    private EconomyManager economyManager;
    private Logger logger;
    private BukkitScheduler scheduler;

    @Override
    public void onEnable() {
        initializeComponents();
        if (!loadConfiguration()) {
            disablePlugin();
            return;
        }
        setupEconomy();
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
        economyManager = new EconomyManager(this);
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
     * Sets up the economy integration only if enabled
     */
    private void setupEconomy() {
        if (configManager.isWelcomeCommandEnabled() && !economyManager.setupEconomy()) {
            logger.warning("Economy plugin not found! Reward system disabled.");
        }
    }

    /**
     * Registers event listeners and commands based on the config file.
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

    private void disablePlugin() {
        setEnabled(false);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public Logger getPluginLogger() {
        return logger;
    }

    public BukkitScheduler getScheduler() {
        return scheduler;
    }
}