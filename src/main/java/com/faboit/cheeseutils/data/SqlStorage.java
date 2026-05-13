package com.faboit.cheeseutils.data;

import com.faboit.cheeseutils.util.AsyncExecutor;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class SqlStorage implements Storage {
    public record DatabaseSettings(String mode, String host, int port, String database, String user, String password) {
    }

    private final File dataFolder;
    private final AsyncExecutor executor;
    private final Logger logger;
    private final DatabaseSettings settings;
    private final MigrationManager migrationManager = new MigrationManager();

    public SqlStorage(File dataFolder, AsyncExecutor executor, Logger logger, DatabaseSettings settings) {
        this.dataFolder = dataFolder;
        this.executor = executor;
        this.logger = logger;
        this.settings = settings;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = open()) {
                migrationManager.run(connection);
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS player_settings (
                            uuid TEXT NOT NULL,
                            setting_key TEXT NOT NULL,
                            state_key TEXT NOT NULL,
                            PRIMARY KEY(uuid, setting_key))
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS homes (
                            uuid TEXT NOT NULL,
                            home_name TEXT NOT NULL,
                            world TEXT NOT NULL,
                            x REAL NOT NULL,
                            y REAL NOT NULL,
                            z REAL NOT NULL,
                            yaw REAL NOT NULL,
                            pitch REAL NOT NULL,
                            icon TEXT NOT NULL,
                            PRIMARY KEY(uuid, home_name))
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS warps (
                            warp_name TEXT PRIMARY KEY,
                            world TEXT NOT NULL,
                            x REAL NOT NULL,
                            y REAL NOT NULL,
                            z REAL NOT NULL,
                            yaw REAL NOT NULL,
                            pitch REAL NOT NULL,
                            category TEXT NOT NULL,
                            permission TEXT NOT NULL,
                            hidden INTEGER NOT NULL,
                            admin_only INTEGER NOT NULL)
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS spawn_point (
                            id INTEGER PRIMARY KEY CHECK(id=1),
                            world TEXT NOT NULL,
                            x REAL NOT NULL,
                            y REAL NOT NULL,
                            z REAL NOT NULL,
                            yaw REAL NOT NULL,
                            pitch REAL NOT NULL)
                            """);
                    statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS daily_claims (
                            uuid TEXT PRIMARY KEY,
                            last_claim INTEGER NOT NULL,
                            streak INTEGER NOT NULL)
                            """);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to initialize database", exception);
            }
            logger.info("CheeseUtils storage initialized using " + settings.mode());
        }, executor.io());
    }

    private Connection open() throws Exception {
        if ("mysql".equalsIgnoreCase(settings.mode())) {
            String url = "jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.database() + "?useSSL=false&allowPublicKeyRetrieval=true";
            return DriverManager.getConnection(url, settings.user(), settings.password());
        }
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Unable to create plugin data folder");
        }
        return DriverManager.getConnection("jdbc:sqlite:" + new File(dataFolder, "cheeseutils.db").getAbsolutePath());
    }

    @Override
    public CompletableFuture<Map<String, String>> loadSettings(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> output = new HashMap<>();
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("SELECT setting_key, state_key FROM player_settings WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        output.put(resultSet.getString(1), resultSet.getString(2));
                    }
                }
                return output;
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load settings", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Void> saveSettings(UUID uuid, Map<String, String> states) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = open()) {
                connection.setAutoCommit(false);
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM player_settings WHERE uuid=?")) {
                    delete.setString(1, uuid.toString());
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO player_settings(uuid, setting_key, state_key) VALUES(?,?,?)")) {
                    for (Map.Entry<String, String> entry : states.entrySet()) {
                        insert.setString(1, uuid.toString());
                        insert.setString(2, entry.getKey());
                        insert.setString(3, entry.getValue());
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save settings", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Void> saveHome(UUID uuid, String homeName, SerializedLocation location, String icon) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO homes(uuid, home_name, world, x, y, z, yaw, pitch, icon)
                         VALUES(?,?,?,?,?,?,?,?,?)
                         ON CONFLICT(uuid, home_name) DO UPDATE SET
                         world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z,
                         yaw=excluded.yaw, pitch=excluded.pitch, icon=excluded.icon""")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, homeName.toLowerCase());
                statement.setString(3, location.world());
                statement.setDouble(4, location.x());
                statement.setDouble(5, location.y());
                statement.setDouble(6, location.z());
                statement.setFloat(7, location.yaw());
                statement.setFloat(8, location.pitch());
                statement.setString(9, icon);
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save home", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Void> deleteHome(UUID uuid, String homeName) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM homes WHERE uuid=? AND home_name=?")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, homeName.toLowerCase());
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to delete home", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Map<String, HomeRecord>> loadHomes(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, HomeRecord> output = new HashMap<>();
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("SELECT home_name, world, x, y, z, yaw, pitch, icon FROM homes WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        SerializedLocation location = new SerializedLocation(resultSet.getString(2), resultSet.getDouble(3), resultSet.getDouble(4), resultSet.getDouble(5), resultSet.getFloat(6), resultSet.getFloat(7));
                        output.put(resultSet.getString(1), new HomeRecord(location, resultSet.getString(8)));
                    }
                }
                return output;
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load homes", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Void> saveWarp(String warpName, SerializedLocation location, String category, String permission, boolean hidden, boolean adminOnly) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO warps(warp_name, world, x, y, z, yaw, pitch, category, permission, hidden, admin_only)
                         VALUES(?,?,?,?,?,?,?,?,?,?,?)
                         ON CONFLICT(warp_name) DO UPDATE SET
                         world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z,
                         yaw=excluded.yaw, pitch=excluded.pitch, category=excluded.category,
                         permission=excluded.permission, hidden=excluded.hidden, admin_only=excluded.admin_only""")) {
                statement.setString(1, warpName.toLowerCase());
                statement.setString(2, location.world());
                statement.setDouble(3, location.x());
                statement.setDouble(4, location.y());
                statement.setDouble(5, location.z());
                statement.setFloat(6, location.yaw());
                statement.setFloat(7, location.pitch());
                statement.setString(8, category);
                statement.setString(9, permission);
                statement.setInt(10, hidden ? 1 : 0);
                statement.setInt(11, adminOnly ? 1 : 0);
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save warp", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Void> deleteWarp(String warpName) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM warps WHERE warp_name=?")) {
                statement.setString(1, warpName.toLowerCase());
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to delete warp", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Map<String, WarpRecord>> loadWarps() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, WarpRecord> output = new HashMap<>();
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("SELECT warp_name, world, x, y, z, yaw, pitch, category, permission, hidden, admin_only FROM warps")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        SerializedLocation location = new SerializedLocation(resultSet.getString(2), resultSet.getDouble(3), resultSet.getDouble(4), resultSet.getDouble(5), resultSet.getFloat(6), resultSet.getFloat(7));
                        output.put(resultSet.getString(1), new WarpRecord(location, resultSet.getString(8), resultSet.getString(9), resultSet.getInt(10) == 1, resultSet.getInt(11) == 1));
                    }
                }
                return output;
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load warps", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Void> saveSpawn(SerializedLocation location) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO spawn_point(id, world, x, y, z, yaw, pitch)
                         VALUES(1,?,?,?,?,?,?)
                         ON CONFLICT(id) DO UPDATE SET world=excluded.world, x=excluded.x,
                         y=excluded.y, z=excluded.z, yaw=excluded.yaw, pitch=excluded.pitch""")) {
                statement.setString(1, location.world());
                statement.setDouble(2, location.x());
                statement.setDouble(3, location.y());
                statement.setDouble(4, location.z());
                statement.setFloat(5, location.yaw());
                statement.setFloat(6, location.pitch());
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to save spawn", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<SerializedLocation> loadSpawn() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("SELECT world, x, y, z, yaw, pitch FROM spawn_point WHERE id=1")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new SerializedLocation(resultSet.getString(1), resultSet.getDouble(2), resultSet.getDouble(3), resultSet.getDouble(4), resultSet.getFloat(5), resultSet.getFloat(6));
                    }
                    return null;
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to load spawn", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Long> getDailyLastClaim(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("SELECT last_claim FROM daily_claims WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getLong(1) : 0L;
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to get daily claim state", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<Void> setDailyLastClaim(UUID uuid, long epochMillis, int streak) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO daily_claims(uuid, last_claim, streak)
                         VALUES(?,?,?)
                         ON CONFLICT(uuid) DO UPDATE SET last_claim=excluded.last_claim, streak=excluded.streak""")) {
                statement.setString(1, uuid.toString());
                statement.setLong(2, epochMillis);
                statement.setInt(3, streak);
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to set daily claim state", exception);
            }
        }, executor.io());
    }

    @Override
    public CompletableFuture<DailyState> getDailyState(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = open();
                 PreparedStatement statement = connection.prepareStatement("SELECT last_claim, streak FROM daily_claims WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new DailyState(resultSet.getLong(1), resultSet.getInt(2));
                    }
                    return new DailyState(0, 0);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to get daily state", exception);
            }
        }, executor.io());
    }

    @Override
    public void close() {
    }
}
