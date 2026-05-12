package org.mnm;

import org.assertj.core.api.Assertions;
import org.mnm.client.Client;
import org.mnm.client.Session;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public record TestDatabase(Connection connection) {

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
                    try (ResultSet resultSet = st.executeQuery()) {
                        while (resultSet.next()) {
                            Client actual = new Client(
                                    resultSet.getString("slug"),
                                    resultSet.getString("version"),
                                    Client.Status.valueOf(resultSet.getString("status")),
                                    resultSet.getString("path"));
                            assertThat(actual).isEqualTo(expected);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return this;
            }

            public TableAsserter containsSession(int id, Session expected) {
                try (PreparedStatement st = connection.prepareStatement("select * from %s where id = ? and slug = ? and token = ?;".formatted(tableName));) {
                    st.setInt(1, id);
                    st.setString(2, expected.slug());
                    st.setString(3, expected.token());
                    try (ResultSet resultSet = st.executeQuery()) {
                        boolean found = false;
                        while (resultSet.next()) {
                            Session actual = new Session(
                                    resultSet.getString("slug"),
                                    resultSet.getString("token"));
                            assertThat(actual).isEqualTo(expected);
                            found = true;
                        }
                        if (!found) {
                            Assertions.fail("Session not found: " + expected);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                return this;
            }

            public TableAsserter containsRows(int expected) {
                assertThat(count()).isEqualTo(expected);
                return this;
            }
        }
    }

    private static Connection buildConnection(Path path) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
    }
}
