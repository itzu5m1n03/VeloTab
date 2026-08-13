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
    private boolean enabled = false;

    public DatabaseManager(Logger logger) {
        this.logger = logger;
    }

    public void init(String host, int port, String database, String user, String password, boolean useSSL) {
        if (host == null || host.isEmpty() || host.equals("localhost")) {
            logger.info("Base de datos no configurada o usando valores por defecto. MySQL desactivado.");
            return;
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(user);
            config.setPassword(password);
            config.addDataSourceProperty("useSSL", String.valueOf(useSSL));
            config.addDataSourceProperty("characterEncoding", "utf8");
            config.addDataSourceProperty("serverTimezone", "UTC");
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(5000);

            dataSource = new HikariDataSource(config);
            enabled = true;
            createTables();
            logger.info("Conexión a MySQL establecida correctamente.");
        } catch (Exception e) {
            enabled = false;
            logger.severe("No se pudo conectar a la base de datos MySQL: " + e.getMessage());
        }
    }

    private void createTables() {
        if (!enabled) return;
        String sql = "CREATE TABLE IF NOT EXISTS velotab_data (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "scoreboard_enabled BOOLEAN DEFAULT TRUE," +
                "current_tag VARCHAR(32) DEFAULT NULL" +
                ");";
        try (Connection conn = getConnection()) {
            if (conn != null) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.execute();
                }
            }
        } catch (SQLException e) {
            logger.warning("Error al crear tablas de base de datos: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (!enabled || dataSource == null) return null;
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
        enabled = false;
    }

    public boolean isScoreboardEnabled(UUID uuid) {
        if (!enabled) return true;
        String sql = "SELECT scoreboard_enabled FROM velotab_data WHERE uuid = ?";
        try (Connection conn = getConnection()) {
            if (conn == null) return true;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) return rs.getBoolean("scoreboard_enabled");
                }
            }
        } catch (SQLException e) {
            logger.warning("Error al consultar estado de scoreboard en DB: " + e.getMessage());
        }
        return true;
    }

    public void setScoreboardEnabled(UUID uuid, boolean enabled) {
        if (!this.enabled) return;
        String sql = "INSERT INTO velotab_data (uuid, scoreboard_enabled) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE scoreboard_enabled = ?";
        try (Connection conn = getConnection()) {
            if (conn == null) return;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setBoolean(2, enabled);
                stmt.setBoolean(3, enabled);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.warning("Error al guardar estado de scoreboard en DB: " + e.getMessage());
        }
    }
}
