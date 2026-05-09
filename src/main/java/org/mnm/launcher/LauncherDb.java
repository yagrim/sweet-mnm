package org.mnm.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLite connector for the Launcher database.
 */
class LauncherDb implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(LauncherDb.class);

    private static final String SELECT_FROM_SETTINGS = "select * from settings;";

    final Connection connection;

    LauncherDb(Path dbFile) throws FileNotFoundException {
        if (!dbFile.toFile().exists()) {
            throw new FileNotFoundException(dbFile.toString());
        }
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath().toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Map<String, String> getSettings() {
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery(SELECT_FROM_SETTINGS);

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

    public void updateSettings(String key, Object value) {
        try (PreparedStatement statement = connection.prepareStatement("update settings set value = ? where variable = ?;")) {
            statement.setObject(1, value);
            statement.setString(2, key);
            logger.debug("Updated settings rows: {}", statement.executeUpdate());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

}
