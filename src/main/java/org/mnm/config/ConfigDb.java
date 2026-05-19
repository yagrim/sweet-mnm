package org.mnm.config;

import org.mnm.tools.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.mnm.config.Mappers.mapClient;
import static org.mnm.tools.StringUtils.isEmpty;

public class ConfigDb implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ConfigDb.class);

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
        boolean dbFileExists = dbFile.toFile().exists();
        logger.debug("Opening ConfigDB: {}", dbFile.toAbsolutePath());
        if (!dbFileExists) {
            FileUtils.createDirectories(dbFile);
        }
        try {
            var connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
            return new ConfigDb(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public List<Client> getClients() {
        return select("clients", "", Mappers::mapClient);
    }

    public Client getClient(String slug) {
        try (PreparedStatement ps = connection.prepareStatement("select * from clients where slug = ?")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapClient(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Session> getSessions(String slug) {
        return select("sessions", "slug = '%s'".formatted(slug), Mappers::mapSession);
    }

    private <T> List<T> select(String table, String where, Function<ResultSet, T> mapper) {
        final String query = "select * from %s%s".formatted(table, isEmpty(where) ? "" : " where " + where);
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            final List<T> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.apply(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateClient(String slug, String version, Client.Status status, String path) {
        try (PreparedStatement ps = connection.prepareStatement("update clients set version = ?, status = ?, path = ? where slug = ?")) {
            ps.setString(1, version);
            ps.setString(2, status.name());
            ps.setString(3, path);
            ps.setString(4, slug);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
