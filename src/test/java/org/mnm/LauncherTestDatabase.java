package org.mnm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.TestUtils.classpathFile;

public class LauncherTestDatabase {

    private static final String LAUNCHER_TEST_DB = "launcher_test.db";

    public static final String INITIAL_TOKEN = "123.456.789";

    public static Path fromClasspath() {
        return classpathFile(LAUNCHER_TEST_DB);
    }

    /**
     * Returns a modifiable {@link TestDatabase} with pre-initialized 'settings' table.
     */
    public static TestDatabase withSettings(Path tempDir) {
        final Path dbCopy = tempDir.resolve(LAUNCHER_TEST_DB);
        try {
            Files.copy(classpathFile(LAUNCHER_TEST_DB), dbCopy);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new TestDatabase(dbCopy);
    }

    /**
     * Returns a modifiable {@link TestDatabase} with empty 'settings' and
     * 'game_versions' table.
     */
    public static TestDatabase withSchema(Path tempDir) {
        final Path dbFile = tempDir.resolve("%s.db".formatted(UUID.randomUUID()));
        try {
            dbFile.toFile().createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (var connection = buildConnection(dbFile);
             var st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE settings (variable text primary key,value text)");
            st.executeUpdate("CREATE TABLE game_versions (slug text primary key,version text)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new TestDatabase(dbFile);
    }

    public record TestDatabase(Path path) {

        public TokenAsserter assertThatToken() {
            return new TokenAsserter(path);
        }

        public void clearSettingsToken() {
            updateSettingsToken("");
        }

        public void updateSettingsToken(String token) {
            try (var connection = buildConnection(path);
                 var st = connection.prepareStatement("update settings set value = ? where variable = ?;")) {
                st.setString(1, token);
                st.setString(2, "token");
                st.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public void insertSettingsToken(String value) {
            insertSettings("token", value);
        }

        public void insertSettings(String variable, String value) {
            try (var connection = buildConnection(path);
                 PreparedStatement st = connection.prepareStatement("insert into settings(variable, value) values (?,?);")) {

                st.setString(1, variable);
                st.setString(2, value);
                st.executeUpdate();

            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public void updateSettings(String variable, String value) {
            try (var connection = buildConnection(path);
                 var st = connection.prepareStatement("update settings set value = ? where variable = ?;")) {
                st.setString(1, value);
                st.setString(2, variable);
                st.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public SettingsAsserter assertThatSettings() {
            return new SettingsAsserter(path);
        }

        public record TokenAsserter(Path path) {

            public TokenAsserter isEqualTo(String expected) {
                try (var connection = buildConnection(path);
                     var st = connection.prepareStatement("select value from settings where variable = '%s';".formatted("token"))) {

                    try (ResultSet resultSet = st.executeQuery()) {
                        String actual = resultSet.getString("value");
                        assertThat(actual).isEqualTo(expected);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return this;
            }

        }

        public record SettingsAsserter(Path path) {

            public SettingsAsserter isEmpty() {
                try (var connection = buildConnection(path);
                     var st = connection.prepareStatement("select count(variable) from settings;");
                     var rs = st.executeQuery()) {

                    int count = 0;
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                    assertThat(count).isZero();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return this;
            }

            public SettingsAsserter contains(String variable, String value) {
                try (var connection = buildConnection(path);
                     var st = connection.prepareStatement("select value from settings where variable = '%s';".formatted(variable));
                     var rs = st.executeQuery()) {

                    assertThat(rs.getString(1)).isEqualTo(value);

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return this;
            }
        }
    }

    private static Connection buildConnection(Path path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
    }
}
