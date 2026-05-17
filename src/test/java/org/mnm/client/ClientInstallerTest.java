package org.mnm.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.ConfigTestDatabase;
import org.mnm.client.ClientInstaller.InstallationResult;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Environment;
import org.mnm.config.Session;
import org.mnm.tools.PanicException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.ApiServerStubs.*;
import static org.mnm.TestUtils.*;
import static org.mnm.config.Client.Status.COMPLETED;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WireMockTest(extensionScanningEnabled = true)
class ClientInstallerTest {

    private static final String VALID_TEST_TOKEN = testToken(Instant.now().plus(1, ChronoUnit.MINUTES));
    private static final String EXPIRED_TEST_TOKEN = testToken(Instant.ofEpochSecond(1000));

    @TempDir
    private static Path tempDir;

    private static Path dbFile;

    @BeforeAll
    static void beforeEach() throws IOException {
        try {
            FileUtils.forceDelete(Environment.downloads.toFile());
            FileUtils.forceDelete(testInstallationPath().toFile());
        } catch (FileNotFoundException e) {
        }
        dbFile = testConfigDatabase(tempDir);
    }

    @Test
    void shouldFailWithoutCredentials(WireMockRuntimeInfo wiremock) {
        final ClientInstaller installer = new ClientInstaller(null);
        InstallOptions options = new InstallOptions("", "", null);

        assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock)))
                .isInstanceOf(PanicException.class)
                .hasMessage("Username or password is empty");
    }

    @Test
    void shouldFailWithOnlyUsername(WireMockRuntimeInfo wiremock) {
        final ClientInstaller installer = new ClientInstaller(null);
        InstallOptions options = new InstallOptions("username", null, null);

        assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock)))
                .isInstanceOf(PanicException.class)
                .hasMessage("Username or password is empty");
    }

    @Test
    void shouldFailWhenStoredSessionTokenIsExpired(WireMockRuntimeInfo wiremock) {
        final Path dbFile = tempDir.resolve("expired-token.db");

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {
            configDb.addClient(new Client(TEST_SLUG, TEST_VERSION, COMPLETED, testInstallationPath().toAbsolutePath().toString()));
            configDb.addSession(new Session(TEST_SLUG, EXPIRED_TEST_TOKEN));

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG);

            assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock)))
                    .isInstanceOf(PanicException.class)
                    .hasMessage("Session token has expired: run 'install --username ...' to create a new one");
        }
    }

    @Test
    @Order(1)
    void shouldInstallClientFromScratch(WireMockRuntimeInfo wiremock) throws SQLException {
        stubAuthenticationFlow(wiremock);
        stubChunkDownload("a1fd9407db7effaf");
        stubChunkDownload("3d8638fbc9718fcb");
        stubChunkDownload("a5700d088b8922a7");
        stubChunkDownload("31054dae2bb797ad");

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            assertThat(tempDir.resolve("mnm")).doesNotExist();
            assertThat(tempDir.resolve("downloads")).doesNotExist();

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions("username", "password", null);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(listDirs(tempDir)).containsExactlyInAnyOrder("mnm", "downloads");
            assertThat(tempDir.resolve("mnm")).isNotEmptyDirectory();
            assertThat(tempDir.resolve("downloads")).isNotEmptyDirectory();
            assertThat(tempDir.resolve("downloads").resolve("bundles")).isNotEmptyDirectory();

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(3);
            assertThat(result.orphan()).isEqualTo(0);
        }
    }

    @Test
    @Order(2)
    void shouldValidateClientAfterInstallation(WireMockRuntimeInfo wiremock) throws SQLException {
        stubAuthenticationFlow(wiremock);

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(0);
        }
    }

    @Test
    @Order(3)
    void shouldRepairAndReInstallMissingFiles(WireMockRuntimeInfo wiremock) throws SQLException {
        deletePath(testInstallationPath().resolve("data"));
        stubAuthenticationFlow(wiremock);

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(1);
            assertThat(result.orphan()).isEqualTo(0);
        }
    }

    @Test
    @Order(4)
    void shouldRepairAndFixCorruptedFiles(WireMockRuntimeInfo wiremock) throws SQLException {
        appendToFile(testInstallationPath().resolve("numbers.txt"), "corrupted");
        stubAuthenticationFlow(wiremock);

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(result.invalid()).isEqualTo(1);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(0);
        }
    }

    @Test
    @Order(5)
    void shouldRemoveOrphanFiles(WireMockRuntimeInfo wiremock) throws SQLException {
        final Path additionalFile1 = testInstallationPath().resolve("unnecessary-1.txt");
        final Path additionalFile2 = testInstallationPath().resolve("unnecessary-2.bin");
        appendToFile(additionalFile1, "some-text");
        appendToFile(additionalFile2, "some-text");

        assertThat(additionalFile1).isNotEmptyFile();
        assertThat(additionalFile2).isNotEmptyFile();

        stubAuthenticationFlow(wiremock);

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertThat(additionalFile1).doesNotExist();
            assertThat(additionalFile2).doesNotExist();

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(2);
        }
    }

    private static void assertDatabaseContainsClientAndSession(Path dbFile) throws SQLException {
        try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
            assertThat(testDatabase.getTables())
                    .containsExactlyInAnyOrder("clients", "sessions");

            testDatabase.assertThatTable("clients")
                    .containsClient(new Client(TEST_SLUG, TEST_VERSION, COMPLETED, testInstallationPath().toAbsolutePath().toString()))
                    .hasRows(1);
            testDatabase.assertThatTable("sessions")
                    .containsSession(1, new Session(TEST_SLUG, VALID_TEST_TOKEN))
                    .hasRows(1);
        }
    }

    private static void stubAuthenticationFlow(WireMockRuntimeInfo wiremock) {
        stubAccountLogin(VALID_TEST_TOKEN);
        stubGameVersions(wiremock.getHttpBaseUrl());
        stubManifestDownload();
    }

    private static String mockApiBaseUrl(WireMockRuntimeInfo wiremock) {
        return wiremock.getHttpBaseUrl();
    }

    private static Path testConfigDatabase(Path tempDir) {
        return tempDir.resolve("sweet-test.db");
    }

    private static Path testInstallationPath() {
        return tempDir.resolve(TEST_SLUG);
    }

    private static List<String> listDirs(Path base) {
        try {
            return Files.list(base).toList().stream()
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
