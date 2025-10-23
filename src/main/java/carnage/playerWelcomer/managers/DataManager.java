package carnage.playerWelcomer.managers;

import carnage.playerWelcomer.PlayerWelcomer;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages persistent data for welcomed players using SQLite database.
 * Uses thread-safe collections for performance in a multi-threaded environment.
 * Implements automatic cleanup to prevent memory leaks.
 */
public class DataManager {
    private static final long WELCOME_WINDOW_MS = 60_000L; // 60 seconds
    private static final long CLEANUP_INTERVAL_TICKS = 1200L; // 1 minute
    private static final long COOLDOWN_CLEANUP_THRESHOLD_MS = 300_000L; // 5 minutes

    private final PlayerWelcomer plugin;
    private final File databaseFile;
    private Connection connection;

    // In-memory caches for fast access
    private final ConcurrentHashMap<UUID, Long> cooldowns;
    private final ConcurrentHashMap<UUID, Long> joinTimes;

    private BukkitTask cleanupTask;

    public DataManager(PlayerWelcomer plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
        this.joinTimes = new ConcurrentHashMap<>();
        this.databaseFile = new File(plugin.getDataFolder(), "playerdata.db");

        initializeDatabase();
        startCleanupTask();
    }

    /**
     * Initializes the SQLite database and creates tables if they don't exist.
     */
    private void initializeDatabase() {
        try {
            // Create data folder if it doesn't exist
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Create connection
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // Create tables
            try (Statement stmt = connection.createStatement()) {
                // Table for welcomed players
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS welcomed_players (" +
                                "uuid TEXT PRIMARY KEY, " +
                                "welcomed_at INTEGER NOT NULL" +
                                ")"
                );

                // Table for plugin metadata
                stmt.execute(
                        "CREATE TABLE IF NOT EXISTS metadata (" +
                                "key TEXT PRIMARY KEY, " +
                                "value TEXT NOT NULL" +
                                ")"
                );

                // Initialize unique join count if not exists
                stmt.execute(
                        "INSERT OR IGNORE INTO metadata (key, value) VALUES ('unique_join_count', '0')"
                );
            }

            plugin.getPluginLogger().info("SQLite database initialized successfully");
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Starts the automatic cleanup task to prevent memory leaks.
     */
    private void startCleanupTask() {
        cleanupTask = plugin.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::performCleanup,
                CLEANUP_INTERVAL_TICKS,
                CLEANUP_INTERVAL_TICKS
        );
    }

    /**
     * Performs cleanup of expired data to prevent memory leaks.
     */
    private void performCleanup() {
        long now = System.currentTimeMillis();

        // Clean up expired join times
        long joinTimeExpiry = now - WELCOME_WINDOW_MS;
        int removedJoinTimes = (int) joinTimes.entrySet().stream()
                .filter(entry -> entry.getValue() < joinTimeExpiry)
                .peek(entry -> joinTimes.remove(entry.getKey()))
                .count();

        // Clean up old cooldowns
        long cooldownExpiry = now - COOLDOWN_CLEANUP_THRESHOLD_MS;
        int removedCooldowns = (int) cooldowns.entrySet().stream()
                .filter(entry -> entry.getValue() < cooldownExpiry)
                .peek(entry -> cooldowns.remove(entry.getKey()))
                .count();

        if (removedJoinTimes > 0 || removedCooldowns > 0) {
            plugin.getPluginLogger().fine(
                    "Cleaned up " + removedJoinTimes + " expired join times and " +
                            removedCooldowns + " old cooldowns"
            );
        }
    }

    /**
     * Stops the cleanup task and closes database connection.
     */
    public void shutdown() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }

        if (connection != null) {
            try {
                connection.close();
                plugin.getPluginLogger().info("Database connection closed");
            } catch (SQLException e) {
                plugin.getPluginLogger().warning("Error closing database: " + e.getMessage());
            }
        }
    }

    /**
     * Resets all data in the database.
     */
    public void resetDataAsync() {
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM welcomed_players");
                stmt.execute("UPDATE metadata SET value = '0' WHERE key = 'unique_join_count'");

                cooldowns.clear();
                joinTimes.clear();

                plugin.getPluginLogger().info("Database reset successfully");
            } catch (SQLException e) {
                plugin.getPluginLogger().severe("Failed to reset database: " + e.getMessage());
            }
        });
    }

    /**
     * Saves data to the database (for compatibility, but SQLite auto-commits).
     */
    public void saveDataAsync() {
        // SQLite auto-commits by default, so this is mostly for compatibility
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    // Force any pending writes
                    connection.commit();
                }
            } catch (SQLException e) {
                plugin.getPluginLogger().warning("Error during save: " + e.getMessage());
            }
        });
    }

    /**
     * Checks if a player is new (not yet welcomed).
     * Thread-safe with database lookup.
     */
    public boolean isNewPlayer(UUID playerId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM welcomed_players WHERE uuid = ? LIMIT 1"
        )) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();
            return !rs.next(); // Returns true if no record found (new player)
        } catch (SQLException e) {
            plugin.getPluginLogger().warning("Error checking if player is new: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks a player as welcomed and increments the join count.
     */
    public void addWelcomedPlayer(UUID playerId) {
        plugin.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Add to welcomed players
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT OR REPLACE INTO welcomed_players (uuid, welcomed_at) VALUES (?, ?)"
                )) {
                    stmt.setString(1, playerId.toString());
                    stmt.setLong(2, System.currentTimeMillis());
                    stmt.executeUpdate();
                }

                // Increment unique join count
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(
                            "UPDATE metadata SET value = CAST((CAST(value AS INTEGER) + 1) AS TEXT) " +
                                    "WHERE key = 'unique_join_count'"
                    );
                }

                // Remove from join times cache
                joinTimes.remove(playerId);
            } catch (SQLException e) {
                plugin.getPluginLogger().severe("Error adding welcomed player: " + e.getMessage());
            }
        });
    }

    /**
     * Gets the total number of unique joins.
     */
    public int getUniqueJoinCount() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT value FROM metadata WHERE key = 'unique_join_count'"
        )) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
        } catch (SQLException | NumberFormatException e) {
            plugin.getPluginLogger().warning("Error getting unique join count: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Checks if a player is on cooldown for the welcome command.
     * Thread-safe using in-memory cache.
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
     * Thread-safe using in-memory cache.
     */
    public long getRemainingCooldown(UUID playerId) {
        Long lastUsed = cooldowns.get(playerId);
        if (lastUsed == null) return 0;

        long cooldownDuration = plugin.getConfigManager().getWelcomeCooldown() * 1000L;
        long remaining = lastUsed + cooldownDuration - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    /**
     * Sets a cooldown for the player.
     * Thread-safe using in-memory cache.
     */
    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Records the system time at which a player joined.
     * Thread-safe using in-memory cache.
     */
    public void recordJoinTime(UUID playerId) {
        joinTimes.put(playerId, System.currentTimeMillis());
    }

    /**
     * Checks if the player joined recently enough to still be welcomed.
     * Thread-safe using in-memory cache.
     */
    public boolean isWithinWelcomeWindow(UUID playerId) {
        Long joinTime = joinTimes.get(playerId);
        if (joinTime == null) return false;

        return System.currentTimeMillis() < joinTime + WELCOME_WINDOW_MS;
    }
}