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
        validateRewardSystem();
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

    private void initializeComponents() {
        logger = getLogger();
        scheduler = getServer().getScheduler();
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        rewardManager = new RewardManager(this);
    }

    private boolean loadConfiguration() {
        try {
            configManager.loadConfig();
            return true;
        } catch (Exception e) {
            logger.severe("Failed to load configuration: " + e.getMessage());
            return false;
        }
    }

    private void validateRewardSystem() {
        if (!configManager.isWelcomeCommandEnabled()) {
            return;
        }

        String rewardType = configManager.getWelcomeRewardType();
        String currencyType = configManager.getWelcomeCurrencyType();

        if (!rewardManager.isRewardAvailable(rewardType, currencyType)) {
            logger.warning(String.format("Reward type '%s' with currency '%s' is unavailable", rewardType, currencyType));
        } else {
            logger.info(String.format("Reward type '%s' with currency '%s' is available", rewardType, currencyType));
        }
    }

    private void registerComponents() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        logger.info(configManager.isFirstJoinMessageEnabled()
                ? "First-join messages are enabled"
                : "First-join messages are disabled; only join times will be recorded");

        if (configManager.isWelcomeCommandEnabled()) {
            registerCommands();
            logger.info("Registered welcome and reload commands");
        }
    }

    private void registerCommands() {
        getCommand("welcome").setExecutor(new WelcomeCommand(this));
        getCommand("welcomereload").setExecutor(new ReloadCommand(this));
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