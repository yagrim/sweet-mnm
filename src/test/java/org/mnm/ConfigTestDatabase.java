package org.mnm;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;

import org.mnm.config.Client;
import org.mnm.config.Token;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTestDatabase {

    public static TestDatabase open(Path dbFile) {
        try {
            var connection = buildConnection(dbFile);
            return new TestDatabase(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public record TestDatabase(Connection connection) implements AutoCloseable {

        public List<String> getTables() throws SQLException {
            final List<String> tableNames = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME"));
                }
            }
            return tableNames;
        }

        @Override
        public void close() {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public TableAsserter assertThatTable(String name) {
            return new TableAsserter(connection, name);
        }

        public record TableAsserter(Connection connection, String tableName) {

            public TableAsserter isEmpty() {
                assertThat(count()).isZero();
                return this;
            }

            private int count() {
                try (var st = connection.prepareStatement("select count(*) from %s;".formatted(tableName));
                     var rs = st.executeQuery()) {

                    int count = 0;
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                    return count;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            public TableAsserter containsClient(Client expected) {
                try (PreparedStatement st = connection.prepareStatement("select * from %s where slug = ?;".formatted(tableName));) {
                    st.setString(1, expected.slug());
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            Client actual = new Client(
                                rs.getString("slug"),
                                rs.getString("version"),
                                Client.Status.valueOf(rs.getString("status")),
                                Path.of(rs.getString("path")));
                            assertThat(actual).isEqualTo(expected);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return this;
            }

            public TableAsserter containsToken(Token expected) {
                try (PreparedStatement st = connection.prepareStatement("select * from %s where id = ? and slug = ? and token = ?;".formatted(tableName))) {
                    st.setInt(1, expected.id());
                    st.setString(2, expected.slug());
                    st.setString(3, expected.token());
                    try (ResultSet resultSet = st.executeQuery()) {
                        boolean found = false;
                        while (resultSet.next()) {
                            Token actual = new Token(
                                resultSet.getInt("id"),
                                resultSet.getString("slug"),
                                resultSet.getString("token"));
                            assertThat(actual).isEqualTo(expected);
                            found = true;
                        }
                        if (!found) {
                            Assertions.fail("Token not found: " + expected);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return this;
            }

            public TableAsserter hasRows(int expected) {
                assertThat(count()).isEqualTo(expected);
                return this;
            }
        }
    }

    private static Connection buildConnection(Path path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
    }
}
