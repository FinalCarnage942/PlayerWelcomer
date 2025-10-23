package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.entity.Player;

/**
 * Manages rewards for the welcome command, supporting currency and crate keys.
 * Optimized with better error handling and validation.
 */
public class RewardManager {
    private final PlayerWelcomer plugin;
    private final CurrencyManager currencyManager;
    private final boolean isExcellentCratesAvailable;

    public RewardManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
        this.currencyManager = new CurrencyManager(plugin);
        this.isExcellentCratesAvailable = plugin.getServer().getPluginManager().getPlugin("ExcellentCrates") != null;

        // Initialize currency manager
        currencyManager.initialize();
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
        if (rewardType == null) {
            plugin.getPluginLogger().warning("Reward type is null for " + player.getName());
            return false;
        }

        if (rewardType.equalsIgnoreCase("currency")) {
            return giveCurrencyReward(player, currencyType, amount);
        } else if (rewardType.equalsIgnoreCase("crate_key")) {
            return giveCrateKeyReward(player, crateKeyName, amount);
        }

        plugin.getPluginLogger().warning("Invalid reward type: " + rewardType + " for " + player.getName());
        return false;
    }

    /**
     * Gives currency reward to a player.
     */
    private boolean giveCurrencyReward(Player player, String currencyType, double amount) {
        if (!currencyManager.isCurrencyAvailable(currencyType)) {
            plugin.getPluginLogger().warning(
                    "Currency type '" + currencyType + "' not available! Reward skipped for " + player.getName()
            );
            return false;
        }

        return currencyManager.giveCurrency(player, currencyType, amount);
    }

    /**
     * Gives crate key reward to a player.
     */
    private boolean giveCrateKeyReward(Player player, String crateKeyName, double amount) {
        if (!isExcellentCratesAvailable) {
            plugin.getPluginLogger().warning(
                    "Excellent Crates not found! Crate key reward skipped for " + player.getName()
            );
            return false;
        }

        if (crateKeyName == null || crateKeyName.trim().isEmpty()) {
            plugin.getPluginLogger().warning(
                    "Invalid or missing crate key name in config! Crate key reward skipped for " + player.getName()
            );
            return false;
        }

        int keyAmount = (int) amount;
        if (keyAmount < 1) {
            plugin.getPluginLogger().warning(
                    "Invalid crate key amount: " + amount + " for " + player.getName()
            );
            return false;
        }

        return executeCrateKeyCommand(player, crateKeyName, keyAmount);
    }

    /**
     * Executes the crate key command on the main thread.
     */
    private boolean executeCrateKeyCommand(Player player, String crateKeyName, int amount) {
        String command = String.format("crate key give %s %s %d", player.getName(), crateKeyName, amount);

        try {
            // Must run on main thread
            plugin.getScheduler().runTask(plugin, () -> {
                boolean success = plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        command
                );

                if (!success) {
                    plugin.getPluginLogger().warning(
                            "Failed to dispatch crate key command: " + command +
                                    ". Check if key ID '" + crateKeyName + "' exists in Excellent Crates."
                    );
                }
            });
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe(
                    "Error dispatching crate key command '" + command + "': " + e.getMessage()
            );
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
        if (rewardType == null) {
            return "unknown";
        }

        if (rewardType.equalsIgnoreCase("currency")) {
            return currencyManager.getCurrencyDisplay(currencyType);
        } else if (rewardType.equalsIgnoreCase("crate_key")) {
            return crateKeyName != null ? crateKeyName + " key(s)" : "unknown key(s)";
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
        if (rewardType == null) {
            return false;
        }

        if (rewardType.equalsIgnoreCase("currency")) {
            return currencyManager.isCurrencyAvailable(currencyType);
        } else if (rewardType.equalsIgnoreCase("crate_key")) {
            return isExcellentCratesAvailable;
        }

        return false;
    }

    /**
     * Gets the currency manager instance.
     * @return the currency manager
     */
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }
}