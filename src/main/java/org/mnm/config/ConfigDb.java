package org.mnm.config;

import org.mnm.client.Client;
import org.mnm.client.Session;
import org.mnm.tools.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigDb implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ConfigDb.class);

    // status: installing, completed

    private static String CLIENTS = """
            CREATE TABLE clients (
                slug text NOT NULL PRIMARY KEY,
                version text NOT NULL,
                status text NOT NULL,
                path text NOT NULL
            );
            """;

    private static String SESSIONS = """
            CREATE TABLE sessions (
                id INTEGER PRIMARY KEY,
                slug text NOT NULL,
                token text,
                FOREIGN KEY (slug) REFERENCES clients(slug)
            );
            """;

    private final Connection connection;

    private ConfigDb(Connection connection) {
        this.connection = connection;
    }

    public static final ConfigDb open(Path dbFile) {
//        String home = System.getProperty("user.home");
//        var dbFile = Path.of(home, ".local/share/sweetmnm/", "sweet.db");
        boolean dbFileExists = dbFile.toFile().exists();
        logger.debug("Sweet DB found? {}", dbFileExists);
        if (!dbFileExists) {
            FileUtils.createDirectories(dbFile);
        }
        try {
            var connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
            return new ConfigDb(connection);
        } catch (SQLException e) {
            System.out.println("Can't connect to database");
        }
        return null;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ConfigDb initialize() {
        try {
            final List<String> tableNames = getTableNames(connection);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            if (!tableNames.contains("clients")) {
                createTableSchema(connection, CLIENTS);
            }
            if (!tableNames.contains("sessions")) {
                createTableSchema(connection, SESSIONS);
            }
            return this;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createTableSchema(Connection connection, String createTableSql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTableSql);
        }
    }

    public static List<String> getTableNames(Connection connection) throws SQLException {
        final List<String> tableNames = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tableNames.add(rs.getString("TABLE_NAME"));
            }
        }
        return tableNames;
    }

    private static final String INSERT_CLIENT_QUERY = "INSERT INTO clients (slug, version, status, path) VALUES (?, ?, ?, ?)";

    public void addClient(Client client) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_CLIENT_QUERY)) {
            ps.setString(1, client.slug());
            ps.setString(2, client.version());
            ps.setString(3, client.status().name());
            ps.setString(4, client.path());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String INSERT_SESSION_QUERY = "INSERT INTO sessions (slug, token) VALUES (?, ?)";

    public void addSession(Session session) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_SESSION_QUERY)) {
            ps.setString(1, session.slug());
            ps.setString(2, session.token());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
