package org.mnm.config;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.tools.FileUtils;

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

    private static String TOKENS = """
        CREATE TABLE tokens (
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

    // TODO handle multiple & concurrent openings
    public static final ConfigDb open(Path dbFile) {
        boolean dbFileExists = dbFile.toFile().exists();
        logger.debug("Opening Config DB: {}", dbFile.toAbsolutePath());
        if (!dbFileExists) {
            FileUtils.createDirectories(dbFile);
        }
        try {
            var connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
            return new ConfigDb(connection).initialize();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
            logger.debug("Config DB closed");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ConfigDb initialize() {
        try {
            final List<String> tableNames = getTableNames(connection);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            if (!tableNames.contains("clients")) {
                createTableSchema(connection, CLIENTS);
            }
            if (!tableNames.contains("tokens")) {
                createTableSchema(connection, TOKENS);
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
            ps.setString(4, client.path().toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String INSERT_TOKEN_QUERY = "INSERT INTO tokens (slug, token) VALUES (?, ?)";

    public int addToken(Token token) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_TOKEN_QUERY)) {
            ps.setString(1, token.slug());
            ps.setString(2, token.token());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    logger.debug("Inserted new token: {}, {}", id, token.slug());
                    return id;
                } else {
                    throw new IllegalStateException("No generated key found");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Client> getClients() {
        return select("clients", null, Mappers::mapClient);
    }

    public Client getClient(String slug) {
        try (PreparedStatement ps = connection.prepareStatement("select * from clients where slug = ? group by slug")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Client client = mapClient(rs);
                    logger.debug("Client: {}", client);
                    return client;
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Token> getTokens(String slug) {
        return select("tokens", "slug = '%s'".formatted(slug), Mappers::mapToken);
    }

    public List<Token> getTokens() {
        return select("tokens", null, Mappers::mapToken);
    }

    public Token getToken(int id) {
        try (PreparedStatement ps = connection.prepareStatement("select * from tokens where id = ? order by id")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Mappers.mapToken(rs);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public void updateClient(String slug, String version, Client.Status status, Path path) {
        try (PreparedStatement ps = connection.prepareStatement("update clients set version = ?, status = ?, path = ? where slug = ?")) {
            ps.setString(1, version);
            ps.setString(2, status.name());
            ps.setString(3, path.toString());
            ps.setString(4, slug);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateClientStatus(String slug, Client.Status status) {
        try (PreparedStatement ps = connection.prepareStatement("update clients set status = ? where slug = ?")) {
            ps.setString(1, status.name());
            ps.setString(2, slug);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateToken(Integer id, String token) {
        try (PreparedStatement ps = connection.prepareStatement("update tokens set token = ? where id = ?")) {
            ps.setString(1, token);
            ps.setInt(2, id);
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int deleteTokens(String slug) {
        try (PreparedStatement ps = connection.prepareStatement("delete from tokens where slug = ?")) {
            ps.setString(1, slug);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
