package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

import java.util.logging.Logger;

/**
 * Manages currency transactions for multiple currency plugins (Vault, PlayerPoints, CoinsEngine).
 * Adheres to SRP by focusing on currency initialization and reward distribution.
 */
public class CurrencyManager {
    private final PlayerWelcomer plugin;
    private final Logger logger;
    private Economy vaultEconomy;
    private PlayerPointsAPI playerPointsAPI;
    private boolean isVaultAvailable;
    private boolean isPlayerPointsAvailable;
    private boolean isCoinsEngineAvailable;
    private boolean isVaultSetupAttempted;

    public CurrencyManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPluginLogger();
        initializePlugins();
    }

    /**
     * Initializes currency plugins and checks their availability.
     */
    private void initializePlugins() {
        isVaultAvailable = plugin.getServer().getPluginManager().getPlugin("Vault") != null;
        isPlayerPointsAvailable = plugin.getServer().getPluginManager().getPlugin("PlayerPoints") != null;
        isCoinsEngineAvailable = plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null;
        if (isVaultAvailable) {
            logger.info("Vault detected. Currency rewards via Vault are available.");
        }
        if (isPlayerPointsAvailable) {
            PlayerPoints playerPoints = (PlayerPoints) plugin.getServer().getPluginManager().getPlugin("PlayerPoints");
            playerPointsAPI = playerPoints != null ? playerPoints.getAPI() : null;
            if (playerPointsAPI != null) {
                logger.info("PlayerPoints detected. Currency rewards via PlayerPoints are available.");
            } else {
                isPlayerPointsAvailable = false;
                logger.warning("Failed to initialize PlayerPoints API.");
            }
        }
        if (isCoinsEngineAvailable) {
            logger.info("CoinsEngine detected. Currency rewards via CoinsEngine are available.");
        }
    }

    /**
     * Sets up the Vault economy provider with a retry mechanism.
     * @return true if setup is successful, false otherwise
     */
    private boolean setupVaultEconomy() {
        if (isVaultSetupAttempted) {
            return vaultEconomy != null;
        }
        isVaultSetupAttempted = true;

        if (!isVaultAvailable) {
            logger.warning("Vault plugin not found! Currency rewards via Vault disabled.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warning("No economy provider registered with Vault. Retrying in 5 seconds...");
            new BukkitRunnable() {
                @Override
                public void run() {
                    RegisteredServiceProvider<Economy> retryRsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
                    if (retryRsp == null) {
                        logger.warning("Retry failed: No economy provider registered with Vault. Currency rewards via Vault disabled.");
                    } else {
                        vaultEconomy = retryRsp.getProvider();
                        if (vaultEconomy != null) {
                            logger.info("Vault economy provider initialized: " + vaultEconomy.getName());
                        } else {
                            logger.warning("Retry failed: Vault economy provider is null. Currency rewards via Vault disabled.");
                        }
                    }
                }
            }.runTaskLater(plugin, 100L); // 5-second delay (100 ticks)
            return false;
        }

        vaultEconomy = rsp.getProvider();
        if (vaultEconomy == null) {
            logger.warning("Failed to initialize Vault economy provider. Currency rewards via Vault disabled.");
            return false;
        }
        logger.info("Vault economy provider initialized: " + vaultEconomy.getName());
        return true;
    }

    /**
     * Gives currency to a player based on the currency type.
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

        if (currencyType.equalsIgnoreCase("vault")) {
            if (vaultEconomy == null && !setupVaultEconomy()) {
                logger.warning("Vault economy not available! Currency reward skipped for " + player.getName());
                return false;
            }
            plugin.getScheduler().runTaskAsynchronously(plugin, () -> vaultEconomy.depositPlayer(player, amount));
            return true;
        } else if (currencyType.equalsIgnoreCase("playerpoints")) {
            if (!isPlayerPointsAvailable || playerPointsAPI == null) {
                logger.warning("PlayerPoints not available! Currency reward skipped for " + player.getName());
                return false;
            }
            int intAmount = (int) amount; // PlayerPoints uses integers
            return playerPointsAPI.give(player.getUniqueId(), intAmount);
        } else if (currencyType.toLowerCase().startsWith("coinsengine:")) {
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
            plugin.getScheduler().runTaskAsynchronously(plugin, () -> CoinsEngineAPI.addBalance(player, currency, amount));
            return true;
        }

        logger.warning("Invalid currency type: " + currencyType + " for " + player.getName());
        return false;
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
        if (currencyType.equalsIgnoreCase("vault")) {
            return vaultEconomy != null ? vaultEconomy.currencyNamePlural() : "coins";
        } else if (currencyType.equalsIgnoreCase("playerpoints")) {
            return "points";
        } else if (currencyType.toLowerCase().startsWith("coinsengine:")) {
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
        if (currencyType.equalsIgnoreCase("vault")) {
            return isVaultAvailable && (vaultEconomy != null || setupVaultEconomy());
        } else if (currencyType.equalsIgnoreCase("playerpoints")) {
            return isPlayerPointsAvailable && playerPointsAPI != null;
        } else if (currencyType.toLowerCase().startsWith("coinsengine:")) {
            if (!isCoinsEngineAvailable) {
                return false;
            }
            String currencyId = currencyType.substring("coinsengine:".length()).trim();
            return !currencyId.isEmpty() && CoinsEngineAPI.getCurrency(currencyId) != null;
        }
        return false;
    }
}