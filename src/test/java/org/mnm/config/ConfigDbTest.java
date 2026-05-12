package org.mnm.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.ConfigTestDatabase;
import org.mnm.client.Client;
import org.mnm.client.Session;

import java.nio.file.Path;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.config.ConfigDbTest.Clients.testConfigDatabase;

class ConfigDbTest {


    @Test
    void shouldInitialize(@TempDir Path tempDir) throws SQLException {
        final Path dbFile = testConfigDatabase(tempDir);

        try (ConfigDb config = ConfigDb.open(dbFile)) {
            config.initialize();
        }

        var testDatabase = ConfigTestDatabase.open(dbFile);
        assertThat(testDatabase.getTables())
                .containsExactlyInAnyOrder("clients", "sessions");
        testDatabase.assertThatTable("clients").isEmpty();
        testDatabase.assertThatTable("sessions").isEmpty();

        testDatabase.close();
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

            var testDatabase = ConfigTestDatabase.open(dbFile);
            testDatabase.assertThatTable("clients")
                    .containsClient(testClient("mnm"))
                    .containsRows(1);

            testDatabase.close();
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

            var testDatabase = ConfigTestDatabase.open(dbFile);
            testDatabase.assertThatTable("clients")
                    .containsClient(client1)
                    .containsClient(client2)
                    .containsClient(client3)
                    .containsRows(3);

            testDatabase.close();
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

        @Nested
        class Sessions {

            @Test
            void shouldAddANewSession(@TempDir Path tempDir) {
                final Path dbFile = testConfigDatabase(tempDir);

                Client client = testClient();
                Session session = testSession(client.slug());

                try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                    config.addClient(client);
                    config.addSession(session);
                }

                var testDatabase = ConfigTestDatabase.open(dbFile);
                testDatabase.assertThatTable("sessions")
                        .containsSession(1, new Session(session.slug(), session.token()))
                        .containsRows(1);

                testDatabase.close();
            }

            @Test
            void shouldAddMultipleSessionsForTheSameClient(@TempDir Path tempDir) {
                final Path dbFile = testConfigDatabase(tempDir);

                Client client = testClient();
                Session session = testSession(client.slug());

                try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                    config.addClient(client);
                    config.addSession(session);
                    config.addSession(session);
                    config.addSession(session);
                }

                var testDatabase = ConfigTestDatabase.open(dbFile);
                testDatabase.assertThatTable("sessions")
                        .containsSession(1, new Session(session.slug(), session.token()))
                        .containsSession(2, new Session(session.slug(), session.token()))
                        .containsSession(3, new Session(session.slug(), session.token()))
                        .containsRows(3);

                testDatabase.close();
            }
        }

        static Path testConfigDatabase(Path tempDir) {
            return tempDir.resolve("sweet-test.db");
        }

        private static Client testClient() {
            return testClient("mnm");
        }

        private static Client testClient(String slug) {
            return new Client(slug, "1.0.0-patch", Client.Status.COMPLETED, "");
        }

        private static Session testSession(String slug) {
            return new Session(slug, "123456789.123456789.123456789");
        }
    }
}
