package com.faboit.cheeseutils.data;

import java.sql.Connection;
import java.sql.Statement;

public final class MigrationManager {
    public void run(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS schema_meta (id INTEGER PRIMARY KEY CHECK(id=1), version INTEGER NOT NULL)");
            statement.executeUpdate("INSERT INTO schema_meta(id, version) VALUES(1,1) ON CONFLICT(id) DO NOTHING");
        }
    }

    public void runMysql(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS schema_meta (id INT PRIMARY KEY, version INT NOT NULL)");
            statement.executeUpdate("INSERT IGNORE INTO schema_meta(id, version) VALUES(1,1)");
        }
    }
}
