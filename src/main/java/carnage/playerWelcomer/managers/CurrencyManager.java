package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages currency transactions for multiple currency plugins (Vault, PlayerPoints, CoinsEngine).
 * Optimized with proper async handling and event-driven Vault initialization.
 */
public class CurrencyManager implements Listener {
    private final PlayerWelcomer plugin;
    private final Logger logger;
    private volatile Economy vaultEconomy;
    private volatile PlayerPointsAPI playerPointsAPI;
    private final AtomicBoolean isVaultSetupComplete = new AtomicBoolean(false);
    private boolean isVaultAvailable;
    private boolean isPlayerPointsAvailable;
    private boolean isCoinsEngineAvailable;

    public CurrencyManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPluginLogger();
    }

    /**
     * Initializes currency plugins and checks their availability.
     * Call this from onEnable.
     */
    public void initialize() {
        isVaultAvailable = plugin.getServer().getPluginManager().getPlugin("Vault") != null;
        isPlayerPointsAvailable = plugin.getServer().getPluginManager().getPlugin("PlayerPoints") != null;
        isCoinsEngineAvailable = plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null;

        if (isVaultAvailable) {
            logger.info("Vault detected. Attempting to hook economy provider...");
            if (!setupVaultEconomy()) {
                // Register event listener to catch late economy provider registration
                plugin.getServer().getPluginManager().registerEvents(this, plugin);
                logger.info("Waiting for economy provider to register with Vault...");
            }
        }

        if (isPlayerPointsAvailable) {
            setupPlayerPoints();
        }

        if (isCoinsEngineAvailable) {
            logger.info("CoinsEngine detected. Currency rewards via CoinsEngine are available.");
        }
    }

    /**
     * Sets up PlayerPoints API.
     */
    private void setupPlayerPoints() {
        PlayerPoints playerPoints = (PlayerPoints) plugin.getServer().getPluginManager().getPlugin("PlayerPoints");
        if (playerPoints != null) {
            playerPointsAPI = playerPoints.getAPI();
            if (playerPointsAPI != null) {
                logger.info("PlayerPoints detected. Currency rewards via PlayerPoints are available.");
            } else {
                isPlayerPointsAvailable = false;
                logger.warning("Failed to initialize PlayerPoints API.");
            }
        } else {
            isPlayerPointsAvailable = false;
        }
    }

    /**
     * Sets up the Vault economy provider.
     * @return true if setup is successful, false otherwise
     */
    private boolean setupVaultEconomy() {
        if (isVaultSetupComplete.get()) {
            return vaultEconomy != null;
        }

        if (!isVaultAvailable) {
            logger.warning("Vault plugin not found! Currency rewards via Vault disabled.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        vaultEconomy = rsp.getProvider();
        if (vaultEconomy == null) {
            logger.warning("Vault economy provider is null. Currency rewards via Vault disabled.");
            isVaultSetupComplete.set(true);
            return false;
        }

        logger.info("Vault economy provider initialized: " + vaultEconomy.getName());
        isVaultSetupComplete.set(true);
        return true;
    }

    /**
     * Event handler to catch when economy provider registers with Vault.
     * This handles cases where the economy plugin loads after this plugin.
     */
    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == Economy.class) {
            if (setupVaultEconomy()) {
                logger.info("Late economy provider detected and hooked successfully!");
                // Unregister this listener once setup is complete
                ServiceRegisterEvent.getHandlerList().unregister(this);
            }
        }
    }

    /**
     * Gives currency to a player based on the currency type.
     * Handles async operations appropriately.
     *
     * @param player the player to reward
     * @param currencyType the currency type (e.g., "vault", "playerpoints", "coinsengine:<currency_id>")
     * @param amount the amount of currency
     * @return true if the reward was given successfully, false otherwise
     */
    public boolean giveCurrency(Player player, String currencyType, double amount) {
        if (currencyType == null) {
            logger.warning("Currency type is null for player " + player.getName());
            return false;
        }

        String lowerType = currencyType.toLowerCase();

        if (lowerType.equals("vault")) {
            return giveVaultCurrency(player, amount);
        } else if (lowerType.equals("playerpoints")) {
            return givePlayerPoints(player, amount);
        } else if (lowerType.startsWith("coinsengine:")) {
            return giveCoinsEngineCurrency(player, currencyType, amount);
        }

        logger.warning("Invalid currency type: " + currencyType + " for " + player.getName());
        return false;
    }

    /**
     * Gives Vault currency (runs async via Vault).
     */
    private boolean giveVaultCurrency(Player player, double amount) {
        if (vaultEconomy == null) {
            logger.warning("Vault economy not available! Currency reward skipped for " + player.getName());
            return false;
        }

        // Vault's depositPlayer is already thread-safe and can be called async
        // Run on main thread to be safe with some economy plugins
        plugin.getScheduler().runTask(plugin, () -> {
            try {
                vaultEconomy.depositPlayer(player, amount);
            } catch (Exception e) {
                logger.severe("Error depositing Vault currency for " + player.getName() + ": " + e.getMessage());
            }
        });
        return true;
    }

    /**
     * Gives PlayerPoints (thread-safe).
     */
    private boolean givePlayerPoints(Player player, double amount) {
        if (!isPlayerPointsAvailable || playerPointsAPI == null) {
            logger.warning("PlayerPoints not available! Currency reward skipped for " + player.getName());
            return false;
        }

        int intAmount = (int) amount; // PlayerPoints uses integers
        try {
            return playerPointsAPI.give(player.getUniqueId(), intAmount);
        } catch (Exception e) {
            logger.severe("Error giving PlayerPoints to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Gives CoinsEngine currency (runs async via API).
     */
    private boolean giveCoinsEngineCurrency(Player player, String currencyType, double amount) {
        if (!isCoinsEngineAvailable) {
            logger.warning("CoinsEngine not available! Currency reward skipped for " + player.getName());
            return false;
        }

        String currencyId = currencyType.substring("coinsengine:".length()).trim();
        if (currencyId.isEmpty()) {
            logger.warning("Invalid CoinsEngine currency ID for player " + player.getName());
            return false;
        }

        Currency currency = CoinsEngineAPI.getCurrency(currencyId);
        if (currency == null) {
            logger.warning("CoinsEngine currency '" + currencyId + "' not found! Currency reward skipped for " + player.getName());
            return false;
        }

        // CoinsEngine API is thread-safe for balance operations
        try {
            CoinsEngineAPI.addBalance(player, currency, amount);
            return true;
        } catch (Exception e) {
            logger.severe("Error adding CoinsEngine currency for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the display name for the currency.
     * @param currencyType the currency type
     * @return display name for the currency
     */
    public String getCurrencyDisplay(String currencyType) {
        if (currencyType == null) {
            return "unknown";
        }

        String lowerType = currencyType.toLowerCase();

        if (lowerType.equals("vault")) {
            return vaultEconomy != null ? vaultEconomy.currencyNamePlural() : "coins";
        } else if (lowerType.equals("playerpoints")) {
            return "points";
        } else if (lowerType.startsWith("coinsengine:")) {
            String currencyId = currencyType.substring("coinsengine:".length()).trim();
            if (!isCoinsEngineAvailable || currencyId.isEmpty()) {
                return "unknown currency";
            }
            Currency currency = CoinsEngineAPI.getCurrency(currencyId);
            return currency != null ? currency.getName() : currencyId;
        }

        return "unknown";
    }

    /**
     * Validates if the currency type is available.
     * @param currencyType the currency type
     * @return true if available, false otherwise
     */
    public boolean isCurrencyAvailable(String currencyType) {
        if (currencyType == null) {
            return false;
        }

        String lowerType = currencyType.toLowerCase();

        if (lowerType.equals("vault")) {
            return isVaultAvailable && vaultEconomy != null;
        } else if (lowerType.equals("playerpoints")) {
            return isPlayerPointsAvailable && playerPointsAPI != null;
        } else if (lowerType.startsWith("coinsengine:")) {
            if (!isCoinsEngineAvailable) {
                return false;
            }
            String currencyId = currencyType.substring("coinsengine:".length()).trim();
            return !currencyId.isEmpty() && CoinsEngineAPI.getCurrency(currencyId) != null;
        }

        return false;
    }
}