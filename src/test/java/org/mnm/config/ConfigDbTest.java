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

        var testDatabase = ConfigTestDatabase.open(dbFile);
        assertThat(testDatabase.getTables())
            .containsExactlyInAnyOrder("clients", "sessions");
        testDatabase.assertThatTable("clients").isEmpty();
        testDatabase.assertThatTable("sessions").isEmpty();

        testDatabase.close();
    }

    @Test
    void shouldNotFailWhenInitializingMultipleTimes(@TempDir Path tempDir) throws SQLException {
        final Path dbFile = testConfigDatabase(tempDir);

        try (ConfigDb config = ConfigDb.open(dbFile)) {
            config.initialize();
            config.initialize();
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
                .hasRows(1);

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
                .hasRows(3);

            testDatabase.close();
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
                .hasRows(1);

            testDatabase.close();
        }

        @Test
        void shouldGetSessionById(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            Client client = testClient();
            Session session = testSession(client.slug());

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                config.addClient(client);
                config.addSession(session);

                Session actual = config.getSession(1);
                assertThat(actual).isEqualTo(new Session(1, session.slug(), session.token()));
            }
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
                .hasRows(3);

            testDatabase.close();
        }

        @Test
        void shouldGetAllSessionsBySlug(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                Stream.of("mnm-1", "mnm-2", "mnm-3")
                    .forEach(slug -> config.addClient(testClient(slug)));

                Session session1 = new Session("mnm-1", "1");
                Session session2 = new Session("mnm-2", "2");
                Session session3 = new Session("mnm-3", "3");
                Session session4 = new Session("mnm-1", "11");
                Session session5 = new Session("mnm-2", "22");

                config.addSession(session1);
                config.addSession(session2);
                config.addSession(session3);
                config.addSession(session4);
                config.addSession(session5);

                assertThat(config.getSessions("mnm-1"))
                    .containsExactlyInAnyOrder(
                        new Session(1, "mnm-1", "1"),
                        new Session(4, "mnm-1", "11"));
                assertThat(config.getSessions("mnm-2"))
                    .containsExactlyInAnyOrder(
                        new Session(2, "mnm-2", "2"),
                        new Session(5, "mnm-2", "22"));
                assertThat(config.getSessions("mnm-3"))
                    .containsExactlyInAnyOrder(new Session(3, "mnm-3", "3"));
            }
        }

        @Test
        void shouldGetAllSessions(@TempDir Path tempDir) {
            final Path dbFile = testConfigDatabase(tempDir);

            try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
                Stream.of("mnm-1", "mnm-2", "mnm-3")
                    .forEach(slug -> config.addClient(testClient(slug)));

                Session session1 = new Session("mnm-1", "1");
                Session session2 = new Session("mnm-2", "2");
                Session session3 = new Session("mnm-3", "3");
                Session session4 = new Session("mnm-1", "11");
                Session session5 = new Session("mnm-2", "22");

                config.addSession(session1);
                config.addSession(session2);
                config.addSession(session3);
                config.addSession(session4);
                config.addSession(session5);

                assertThat(config.getSessions())
                    .containsExactlyInAnyOrder(
                        new Session(1, "mnm-1", "1"),
                        new Session(2, "mnm-2", "2"),
                        new Session(3, "mnm-3", "3"),
                        new Session(4, "mnm-1", "11"),
                        new Session(5, "mnm-2", "22"));
            }
        }
    }

    private static Client testClient() {
        return testClient("mnm");
    }

    private static Client testClient(String slug) {
        return new Client(slug, "1.0.0-patch", Client.Status.COMPLETED, Path.of(""));
    }

    private static Session testSession(String slug) {
        return new Session(slug, "123456789.123456789.123456789");
    }

}
