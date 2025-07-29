package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.entity.Player;

/**
 * Manages rewards for the welcome command, supporting currency and crate keys.
 * Adheres to SRP by focusing on reward distribution logic.
 */
public class RewardManager {
    private final PlayerWelcomer plugin;
    private final CurrencyManager currencyManager;
    private final boolean isExcellentCratesAvailable;

    public RewardManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
        this.currencyManager = new CurrencyManager(plugin);
        this.isExcellentCratesAvailable = plugin.getServer().getPluginManager().getPlugin("ExcellentCrates") != null;
    }

    /**
     * Gives a reward to a player based on the reward type.
     * @param player the player to reward
     * @param rewardType the type of reward ("currency" or "crate_key")
     * @param currencyType the currency type for currency rewards (e.g., "vault", "playerpoints", "coinsengine:gold")
     * @param amount the amount of reward
     * @param crateKeyName the crate key name for crate_key rewards
     * @return true if the reward was given successfully, false otherwise
     */
    public boolean giveReward(Player player, String rewardType, String currencyType, double amount, String crateKeyName) {
        if (rewardType.equalsIgnoreCase("currency")) {
            if (!currencyManager.isCurrencyAvailable(currencyType)) {
                plugin.getPluginLogger().warning("Currency type '" + currencyType + "' not available! Reward skipped for " + player.getName());
                return giveFallbackReward(player, crateKeyName, (int) amount);
            }
            return currencyManager.giveCurrency(player, currencyType, amount);
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
     * Gives a fallback crate key reward if the primary currency reward fails.
     * @param player the player to reward
     * @param crateKeyName the crate key name
     * @param amount the amount of keys
     * @return true if the fallback reward was given, false otherwise
     */
    private boolean giveFallbackReward(Player player, String crateKeyName, int amount) {
        if (!isExcellentCratesAvailable || crateKeyName == null || crateKeyName.trim().isEmpty()) {
            plugin.getPluginLogger().warning("No fallback reward available! Reward skipped for " + player.getName());
            return false;
        }
        String command = String.format("crate key give %s %s %d", player.getName(), crateKeyName, amount);
        try {
            plugin.getScheduler().runTask(plugin, () -> {
                boolean success = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                if (!success) {
                    plugin.getPluginLogger().warning("Failed to dispatch fallback crate key command: " + command);
                }
            });
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error dispatching fallback crate key command '" + command + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the display name for the reward.
     * @param rewardType the type of reward ("currency" or "crate_key")
     * @param currencyType the currency type for currency rewards
     * @param crateKeyName the crate key name for crate_key rewards
     * @return display name for the reward
     */
    public String getRewardDisplay(String rewardType, String currencyType, String crateKeyName) {
        if (rewardType.equalsIgnoreCase("currency")) {
            return currencyManager.getCurrencyDisplay(currencyType);
        } else if (rewardType.equalsIgnoreCase("crate_key")) {
            return crateKeyName + " key(s)";
        }
        return "unknown";
    }

    /**
     * Checks if the reward type and currency are available.
     * @param rewardType the reward type
     * @param currencyType the currency type
     * @return true if available, false otherwise
     */
    public boolean isRewardAvailable(String rewardType, String currencyType) {
        if (rewardType.equalsIgnoreCase("currency")) {
            return currencyManager.isCurrencyAvailable(currencyType);
        } else if (rewardType.equalsIgnoreCase("crate_key")) {
            return isExcellentCratesAvailable;
        }
        return false;
    }
}