package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Manages economy integration using Vault for rewarding players.
 */
public class EconomyManager {
    private final PlayerWelcomer plugin;
    private Economy economy;

    public EconomyManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets up the Vault economy integration.
     * @return true if setup is successful, false otherwise
     */
    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Deposits a reward to a player's account asynchronously.
     * @param player the player to reward
     * @param amount the amount to deposit
     */
    public void giveReward(Player player, double amount) {
        if (economy == null) return;
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> economy.depositPlayer(player, amount));
    }
}