package org.mnm.launcher;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQLite connector for the Launcher database.
 */
class LauncherDb implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LauncherDb.class);

    private static final String SELECT_FROM_SETTINGS = "select * from settings;";

    final Connection connection;

    LauncherDb(Path dbFile) throws FileNotFoundException {
        logger.debug("Opening Launcher DB: {}", dbFile.toAbsolutePath());
        if (!dbFile.toFile().exists()) {
            throw new FileNotFoundException(dbFile.toString());
        }
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Map<String, String> getSettings() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_FROM_SETTINGS)) {

            Map<String, String> settings = new HashMap<>();
            while (rs.next()) {
                String key = rs.getString("variable");
                String value = rs.getString("value");
                settings.put(key, value);
            }
            return settings;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateSetting(String key, Object value) {
        try (PreparedStatement statement = connection.prepareStatement("update settings set value = ? where variable = ?;")) {
            statement.setString(1, value != null ? value.toString() : null);
            statement.setString(2, key);
            logger.debug("Updated settings rows: {}", statement.executeUpdate());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
            logger.debug("Launcher DB closed");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
