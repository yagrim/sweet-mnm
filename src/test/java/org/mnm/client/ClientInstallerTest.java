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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.ApiServerStubs.*;
import static org.mnm.TestUtils.appendToFile;
import static org.mnm.TestUtils.deletePath;
import static org.mnm.config.Client.Status.COMPLETED;
import static org.mnm.config.Environment.getInstallPath;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WireMockTest(extensionScanningEnabled = true)
class ClientInstallerTest {

    @TempDir
    private static Path tempDir;

    @BeforeAll
    static void beforeEach() throws IOException {
        try {
            FileUtils.forceDelete(Environment.downloads.toFile());
            FileUtils.forceDelete(Environment.getInstallPath(TEST_SLUG).toFile());
        } catch (FileNotFoundException e) {
        }
    }

    @Test
    @Order(1)
    void shouldInstallClientFromScratch(WireMockRuntimeInfo wiremock) throws SQLException {
        final Path dbFile = testConfigDatabase(tempDir);

        stubAuthenticationFlow(wiremock);
        stubChunkDownload("a1fd9407db7effaf");
        stubChunkDownload("3d8638fbc9718fcb");
        stubChunkDownload("a5700d088b8922a7");
        stubChunkDownload("31054dae2bb797ad");

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions("username", "password", null);
            InstallationResult result = installer.install(options, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(3);
        }
    }

    @Test
    @Order(2)
    void shouldValidateClientAfterInstallation(WireMockRuntimeInfo wiremock) throws SQLException {
        final Path dbFile = testConfigDatabase(tempDir);
        stubAuthenticationFlow(wiremock);

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG);
            InstallationResult result = installer.install(options, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
        }
    }

    @Test
    @Order(3)
    void shouldRepairAndReInstallMissingFiles(WireMockRuntimeInfo wiremock) throws SQLException {
        final Path dbFile = testConfigDatabase(tempDir);
        deletePath(getInstallPath(TEST_SLUG).resolve("data"));
        stubAuthenticationFlow(wiremock);

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG);
            InstallationResult result = installer.install(options, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(1);
        }
    }

    @Test
    @Order(4)
    void shouldRepairAndFixCorruptedFiles(WireMockRuntimeInfo wiremock) throws SQLException {
        final Path dbFile = testConfigDatabase(tempDir);
        appendToFile(getInstallPath(TEST_SLUG).resolve("numbers.txt"), "corrupted");
        stubAuthenticationFlow(wiremock);

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG);
            InstallationResult result = installer.install(options, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile);

            assertThat(result.invalid()).isEqualTo(1);
            assertThat(result.missing()).isEqualTo(0);
        }
    }

    private static void assertDatabaseContainsClientAndSession(Path dbFile) throws SQLException {
        try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
            assertThat(testDatabase.getTables())
                    .containsExactlyInAnyOrder("clients", "sessions");

            testDatabase.assertThatTable("clients")
                    .containsClient(new Client(TEST_SLUG, TEST_VERSION, COMPLETED, installationPath().toAbsolutePath().toString()))
                    .hasRows(1);
            testDatabase.assertThatTable("sessions")
                    .containsSession(1, new Session(TEST_SLUG, "my-token"))
                    .hasRows(1);
        }
    }

    private static void stubAuthenticationFlow(WireMockRuntimeInfo wiremock) {
        stubAccountLogin("my-token");
        stubGameVersions(wiremock.getHttpBaseUrl());
        stubManifestDownload();
    }

    private static String mockApiBaseUrl(WireMockRuntimeInfo wiremock) {
        return wiremock.getHttpBaseUrl();
    }

    private Path testConfigDatabase(Path tempDir) {
        return tempDir.resolve("sweet-test.db");
    }

    private static Path installationPath() {
        return Environment.downloads.getParent().resolve(TEST_SLUG);
    }

}
