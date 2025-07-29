package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages rewards for the welcome command, supporting currency (Vault) and crate keys (Excellent Crates).
 * Adheres to SRP by focusing on reward distribution logic.
 */
public class RewardManager {
    private final PlayerWelcomer plugin;
    private Economy economy;
    private boolean isExcellentCratesAvailable;
    private boolean isEconomySetupAttempted;

    public RewardManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
        this.isExcellentCratesAvailable = plugin.getServer().getPluginManager().getPlugin("ExcellentCrates") != null;
        this.isEconomySetupAttempted = false;
    }

    /**
     * Sets up the Vault economy integration if required.
     * Attempts setup with a delay if initial attempt fails to account for plugin load order.
     * @return true if economy setup is successful, false otherwise
     */
    public boolean setupEconomy() {
        if (isEconomySetupAttempted) {
            return economy != null;
        }
        isEconomySetupAttempted = true;

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getPluginLogger().warning("Vault plugin not found! Currency rewards will not work. Ensure Vault.jar is in the plugins folder.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getPluginLogger().warning("No economy provider registered with Vault. Retrying in 5 seconds...");
            // Retry after a delay to account for late plugin loading
            new BukkitRunnable() {
                @Override
                public void run() {
                    RegisteredServiceProvider<Economy> retryRsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
                    if (retryRsp == null) {
                        plugin.getPluginLogger().warning("Retry failed: No economy provider registered with Vault. Currency rewards disabled.");
                    } else {
                        economy = retryRsp.getProvider();
                        if (economy != null) {
                            plugin.getPluginLogger().info("Economy provider successfully initialized after retry: " + economy.getName());
                        } else {
                            plugin.getPluginLogger().warning("Retry failed: Economy provider is null. Currency rewards disabled.");
                        }
                    }
                }
            }.runTaskLater(plugin, 100L); // 5-second delay (100 ticks)
            return false;
        }

        economy = rsp.getProvider();
        if (economy == null) {
            plugin.getPluginLogger().warning("Failed to initialize economy provider. Currency rewards disabled.");
            return false;
        }
        plugin.getPluginLogger().info("Economy provider initialized: " + economy.getName());
        return true;
    }

    /**
     * Checks if Excellent Crates is available.
     * @return true if Excellent Crates is available, false otherwise
     */
    public boolean isExcellentCratesAvailable() {
        return isExcellentCratesAvailable;
    }

    /**
     * Gives a reward to a player based on the reward type.
     * @param player the player to reward
     * @param rewardType the type of reward ("currency" or "crate_key")
     * @param amount the amount of reward
     * @param crateKeyName the crate key name for crate_key rewards
     * @return true if the reward was given successfully, false otherwise
     */
    public boolean giveReward(Player player, String rewardType, double amount, String crateKeyName) {
        if (rewardType.equalsIgnoreCase("currency")) {
            if (economy == null && !setupEconomy()) {
                plugin.getPluginLogger().warning("Economy plugin not found! Currency reward skipped for " + player.getName());
                return false;
            }
            plugin.getScheduler().runTaskAsynchronously(plugin, () -> economy.depositPlayer(player, amount));
            return true;
        } else if (rewardType.equalsIgnoreCase("crate_key")) {
            if (!isExcellentCratesAvailable) {
                plugin.getPluginLogger().warning("Excellent Crates not found! Crate key reward skipped for " + player.getName());
                return false;
            }
            if (crateKeyName == null || crateKeyName.trim().isEmpty()) {
                plugin.getPluginLogger().warning("Invalid or missing crate key name in config! Crate key reward skipped for " + player.getName());
                return false;
            }
            int intAmount = (int) amount; // Crate keys are integers
            if (intAmount < 1) {
                plugin.getPluginLogger().warning("Invalid crate key amount: " + amount + " for " + player.getName());
                return false;
            }
            String command = String.format("crate key give %s %s %d", player.getName(), crateKeyName, intAmount);
            try {
                plugin.getScheduler().runTask(plugin, () -> {
                    boolean success = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                    if (!success) {
                        plugin.getPluginLogger().warning("Failed to dispatch crate key command: " + command + ". Check if key ID '" + crateKeyName + "' exists in Excellent Crates.");
                    }
                });
                return true;
            } catch (Exception e) {
                plugin.getPluginLogger().severe("Error dispatching crate key command '" + command + "': " + e.getMessage());
                return false;
            }
        }
        plugin.getPluginLogger().warning("Invalid reward type: " + rewardType + " for " + player.getName());
        return false;
    }

    /**
     * Gets the display name for the reward based on the reward type.
     * @param rewardType the type of reward ("currency" or "crate_key")
     * @param crateKeyName the crate key name for crate_key rewards
     * @return display name for the reward
     */
    public String getRewardDisplay(String rewardType, String crateKeyName) {
        if (rewardType.equalsIgnoreCase("crate_key")) {
            return crateKeyName + " key(s)";
        }
        return "coins";
    }
}