package org.mnm.config;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.ConfigTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.config.Client.Status.INSTALLING;
import static org.mnm.config.ConfigDbTest.Clients.testConfigDatabase;

class ConfigDbTest {


    @Test
    void shouldInitialize(@TempDir Path tempDir) throws SQLException {
        final Path dbFile = testConfigDatabase(tempDir);

        try (ConfigDb config = ConfigDb.open(dbFile)) {
            config.initialize();
        }

        try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
            assertThat(testDatabase.getTables()).containsExactlyInAnyOrder("clients", "token");
            testDatabase.assertThatTable("clients").isEmpty();
            testDatabase.assertThatTable("token").isEmpty();
        }
    }

    @Test
    void shouldNotFailWhenInitializingMultipleTimes(@TempDir Path tempDir) throws SQLException {
        final Path dbFile = testConfigDatabase(tempDir);

        try (ConfigDb config = ConfigDb.open(dbFile)) {
            config.initialize();
            config.initialize();
            config.initialize();
        }

        try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
            assertThat(testDatabase.getTables()).containsExactlyInAnyOrder("clients", "token");
            testDatabase.assertThatTable("clients").isEmpty();
            testDatabase.assertThatTable("token").isEmpty();
        }
    }

    @Nested
    class Clients {

        @Test
        void shouldAddANewClient(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            Client client = testClient();

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(client);
            }

            try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
                testDatabase.assertThatTable("clients")
                    .containsClient(testClient("mnm"))
                    .hasRows(1);
            }
        }

        @Test
        void shouldAddMultipleClients(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            Client client1 = testClient("1");
            Client client2 = testClient("22");
            Client client3 = testClient("333");

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(client1);
                config.addClient(client2);
                config.addClient(client3);
            }

            try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
                testDatabase.assertThatTable("clients")
                    .containsClient(client1)
                    .containsClient(client2)
                    .containsClient(client3)
                    .hasRows(3);
            }
        }

        @Test
        void shouldGetClientBySlug(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);
            final String slug = "slugslug";

            Client initClient = testClient(slug);
            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(initClient);

                Client client = config.getClient(slug);
                assertThat(client).isEqualTo(initClient);
            }
        }

        @Test
        void shouldGetAllClients(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(testClient("slug_1"));
                config.addClient(testClient("slug_2"));
                config.addClient(testClient("slug_3"));

                List<Client> clients = config.getClients();
                assertThat(clients)
                    .containsExactlyInAnyOrder(testClient("slug_1"), testClient("slug_2"), testClient("slug_3"))
                    .hasSize(3);
            }
        }

        @Test
        void shouldReturnNullWhenClientBySlug(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                Client client = config.getClient("slugslug");
                assertThat(client).isNull();
            }
        }

        @Test
        void shouldUpdateClientStatus(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);
            final String slug = "mnm";

            Client initClient = testClient(slug);
            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(initClient);

                config.updateClient(slug, "1.2.3-e42", INSTALLING, Path.of("/some/location"));

                Client client = config.getClient(slug);
                assertThat(client.version()).isEqualTo("1.2.3-e42");
                assertThat(client.status()).isEqualTo(INSTALLING);

                assertThat(client.path().toString()).isEqualTo(File.separator + String.join(File.separator, "some", "location"));
            }
        }

        @Test
        void shouldFailIfSlugExists(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            Throwable t;
            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                Client client = testClient("my-slug");
                config.addClient(client);

                t = catchThrowable(() -> config.addClient(client));
            }

            assertThat(t)
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(SQLException.class)
                .hasMessage("[SQLITE_CONSTRAINT_PRIMARYKEY] A PRIMARY KEY constraint failed (UNIQUE constraint failed: clients.slug)");
        }

        static Path testConfigDatabase(Path tempDir) {
            return tempDir.resolve("sweet-test.db");
        }
    }

    @Nested
    class Tokens {

        @Test
        void shouldAddNewToken(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            Client client = testClient();
            Token token = testToken(client.slug());

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(client);
                config.addToken(token);
            }

            try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
                testDatabase.assertThatTable("token")
                    .containsToken(1, new Token(token.slug(), token.token()))
                    .hasRows(1);
            }
        }

        @Test
        void shouldGetTokenById(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            Client client = testClient();
            Token token = testToken(client.slug());

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(client);
                config.addToken(token);

                Token actual = config.getToken(1);
                assertThat(actual).isEqualTo(new Token(1, token.slug(), token.token()));
            }
        }

        @Test
        void shouldUpdateExistingToken(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            Client client = testClient();
            Token token = testToken(client.slug());

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(client);
                config.addToken(token);

                config.updateToken(1, "new-token");
            }

            try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
                testDatabase.assertThatTable("token")
                    .containsToken(1, new Token(token.slug(), "new-token"))
                    .hasRows(1);
            }
        }

        @Test
        void shouldNotFailUpdatingMissingTokenAndNotAddNewRow(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(testClient());

                config.updateToken(1, "new-token");
            }

            try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
                testDatabase.assertThatTable("token")
                    .hasRows(0);
            }
        }

        @Test
        void shouldAddMultipleTokensForTheSameClient(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            Client client = testClient();
            Token token = testToken(client.slug());

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(client);
                config.addToken(token);
                config.addToken(token);
                config.addToken(token);
            }

            try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
                testDatabase.assertThatTable("token")
                    .containsToken(1, new Token(token.slug(), token.token()))
                    .containsToken(2, new Token(token.slug(), token.token()))
                    .containsToken(3, new Token(token.slug(), token.token()))
                    .hasRows(3);
            }
        }

        @Test
        void shouldDeleteTokensBySlug(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                Stream.of("mnm-1", "mnm-2")
                    .forEach(slug -> config.addClient(testClient(slug)));

                config.addToken(new Token("mnm-1", "1"));
                config.addToken(new Token("mnm-1", "11"));
                config.addToken(new Token("mnm-2", "2"));

                int deleted = config.deleteTokens("mnm-1");

                assertThat(deleted).isEqualTo(2);
            }

            try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
                testDatabase.assertThatTable("token")
                    .hasRows(1);
            }
        }

        @Test
        void shouldGetAllTokensBySlug(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                Stream.of("mnm-1", "mnm-2", "mnm-3")
                    .forEach(slug -> config.addClient(testClient(slug)));

                Token token1 = new Token("mnm-1", "1");
                Token token2 = new Token("mnm-2", "2");
                Token token3 = new Token("mnm-3", "3");
                Token token4 = new Token("mnm-1", "11");
                Token token5 = new Token("mnm-2", "22");

                config.addToken(token1);
                config.addToken(token2);
                config.addToken(token3);
                config.addToken(token4);
                config.addToken(token5);

                assertThat(config.getTokens("mnm-1"))
                    .containsExactlyInAnyOrder(
                        new Token(1, "mnm-1", "1"),
                        new Token(4, "mnm-1", "11"));
                assertThat(config.getTokens("mnm-2"))
                    .containsExactlyInAnyOrder(
                        new Token(2, "mnm-2", "2"),
                        new Token(5, "mnm-2", "22"));
                assertThat(config.getTokens("mnm-3"))
                    .containsExactlyInAnyOrder(new Token(3, "mnm-3", "3"));
            }
        }

        @Test
        void shouldGetAllTokens(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                Stream.of("mnm-1", "mnm-2", "mnm-3")
                    .forEach(slug -> config.addClient(testClient(slug)));

                Token token1 = new Token("mnm-1", "1");
                Token token2 = new Token("mnm-2", "2");
                Token token3 = new Token("mnm-3", "3");
                Token token4 = new Token("mnm-1", "11");
                Token token5 = new Token("mnm-2", "22");

                config.addToken(token1);
                config.addToken(token2);
                config.addToken(token3);
                config.addToken(token4);
                config.addToken(token5);

                assertThat(config.getTokens())
                    .containsExactlyInAnyOrder(
                        new Token(1, "mnm-1", "1"),
                        new Token(2, "mnm-2", "2"),
                        new Token(3, "mnm-3", "3"),
                        new Token(4, "mnm-1", "11"),
                        new Token(5, "mnm-2", "22"));
            }
        }
    }

    private static Client testClient() {
        return testClient("mnm");
    }

    private static Client testClient(String slug) {
        return new Client(slug, "1.0.0-patch", Client.Status.COMPLETED, Path.of(""));
    }

    private static Token testToken(String slug) {
        return new Token(slug, "123456789.123456789.123456789");
    }

}
