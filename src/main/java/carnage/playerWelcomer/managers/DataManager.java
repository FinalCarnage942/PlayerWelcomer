package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages persistent data for welcomed players and command cooldowns.
 * Uses thread-safe collections for performance in a multi-threaded environment.
 */
public class DataManager {
    private static final String DATA_FILE_NAME = "data.yml";
    private static final long WELCOME_WINDOW_MS = 60_000L; // 60 seconds
    private final PlayerWelcomer plugin;
    private final File dataFile;
    private final FileConfiguration data;
    private final Set<UUID> welcomedPlayers;
    private final ConcurrentHashMap<UUID, Long> cooldowns;
    private final ConcurrentHashMap<UUID, Long> joinTimes;
    private int uniqueJoinCount;

    public DataManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
        this.welcomedPlayers = new HashSet<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.joinTimes = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        this.data = initializeDataFile();
        loadData();
    }

    /**
     * Initializes the data file. creating it from defaults if needed.
     * @return loaded FileConfiguration
     */
    private FileConfiguration initializeDataFile() {
        if (!dataFile.exists()) {
            plugin.saveResource(DATA_FILE_NAME, false);
        }
        return YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Loads data from the data file into memory.
     */
    private void loadData() {
        uniqueJoinCount = data.getInt("unique-join-count", 0);
        if (data.contains("welcomed-players")) {
            welcomedPlayers.addAll(data.getStringList("welcomed-players").stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet()));
        }
    }

    /**
     * Resets the data file to its default state and reloads it.
     */
    public void resetDataAsync() {
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (data) {
                welcomedPlayers.clear();
                uniqueJoinCount = 0;
                cooldowns.clear();
                joinTimes.clear();
                data.set("unique-join-count", 0);
                data.set("welcomed-players", new java.util.ArrayList<String>());
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    plugin.getPluginLogger().severe("Failed to reset data: " + e.getMessage());
                }
                loadData();
            }
        });
    }

    /**
     * Saves data to the file asynchronously.
     */
    public void saveDataAsync() {
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (data) {
                data.set("unique-join-count", uniqueJoinCount);
                data.set("welcomed-players", welcomedPlayers.stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList()));
                try {
                    data.save(dataFile);
                } catch (IOException e) {
                    plugin.getPluginLogger().severe("Failed to save data: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Checks if a player is new (not yet welcomed).
     * @param playerId the player's UUID
     * @return true if the player is new
     */
    public boolean isNewPlayer(UUID playerId) {
        return !welcomedPlayers.contains(playerId);
    }

    /**
     * Marks a player as welcomed and increments the join count.
     * @param playerId the player's UUID
     */
    public void addWelcomedPlayer(UUID playerId) {
        welcomedPlayers.add(playerId);
        uniqueJoinCount++;
        joinTimes.remove(playerId);
        saveDataAsync();
    }

    /**
     * Gets the total number of unique joins.
     * @return unique join count
     */
    public int getUniqueJoinCount() {
        return uniqueJoinCount;
    }

    /**
     * Checks if a player is on cooldown for the welcome command.
     * @param playerId the player's UUID
     * @return true if on cooldown
     */
    public boolean isOnCooldown(UUID playerId) {
        Long lastUsed = cooldowns.get(playerId);
        if (lastUsed == null) return false;
        long currentTime = System.currentTimeMillis();
        long cooldownDuration = plugin.getConfigManager().getWelcomeCooldown() * 1000L;
        return currentTime < lastUsed + cooldownDuration;
    }

    /**
     * Gets the remaining cooldown time in seconds.
     * @param playerId the player's UUID
     * @return remaining cooldown in seconds
     */
    public long getRemainingCooldown(UUID playerId) {
        Long lastUsed = cooldowns.get(playerId);
        if (lastUsed == null) return 0;
        long cooldownDuration = plugin.getConfigManager().getWelcomeCooldown() * 1000L;
        return Math.max(0, (lastUsed + cooldownDuration - System.currentTimeMillis()) / 1000);
    }

    /**
     * Sets a cooldown for the player.
     * @param playerId the player's UUID
     */
    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Records the join time for a player.
     * @param playerId the player's UUID
     */
    public void recordJoinTime(UUID playerId) {
        joinTimes.put(playerId, System.currentTimeMillis());
    }

    /**
     * Checks if a player is within the welcome time window.
     * @param playerId the player's UUID
     * @return true if within the window
     */
    public boolean isWithinWelcomeWindow(UUID playerId) {
        Long joinTime = joinTimes.get(playerId);
        if (joinTime == null) return false;
        return System.currentTimeMillis() < joinTime + WELCOME_WINDOW_MS;
    }
}