/*
 * Copyright (c) 2026 ItzUsman (itzusm.netlify.app)
 * All rights reserved. This plugin and its source code are protected.
 * Unauthorized modification, redistribution or rebranding is strictly prohibited.
 */
package me.itzusman.velotab.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class DatabaseManager {

    private HikariDataSource dataSource;
    private final Logger logger;

    public DatabaseManager(Logger logger) {
        this.logger = logger;
    }

    public void init(String host, int port, String database, String user, String password, boolean useSSL) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(user);
            config.setPassword(password);
            config.addDataSourceProperty("useSSL", String.valueOf(useSSL));
            config.addDataSourceProperty("characterEncoding", "utf8");
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(5000);

            dataSource = new HikariDataSource(config);
            createTables();
            logger.info("Conexión a MySQL establecida correctamente.");
        } catch (Exception e) {
            logger.severe("No se pudo conectar a la base de datos MySQL: " + e.getMessage());
        }
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS velotab_data (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "scoreboard_enabled BOOLEAN DEFAULT TRUE," +
                "current_tag VARCHAR(32) DEFAULT NULL" +
                ");";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) return null;
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public boolean isScoreboardEnabled(UUID uuid) {
        String sql = "SELECT scoreboard_enabled FROM velotab_data WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (conn == null) return true;
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean("scoreboard_enabled");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void setScoreboardEnabled(UUID uuid, boolean enabled) {
        String sql = "INSERT INTO velotab_data (uuid, scoreboard_enabled) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE scoreboard_enabled = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (conn == null) return;
            stmt.setString(1, uuid.toString());
            stmt.setBoolean(2, enabled);
            stmt.setBoolean(3, enabled);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
