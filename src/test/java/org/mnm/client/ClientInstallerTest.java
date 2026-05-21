package org.mnm.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.mnm.ConfigTestDatabase;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.client.ClientInstaller.InstallationResult;
import org.mnm.client.InstallOptions.FileCheck;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Session;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.ApiServerStubs.*;
import static org.mnm.TestUtils.*;
import static org.mnm.client.InstallOptions.FileCheck.xxhsum;
import static org.mnm.config.Client.Status.COMPLETED;


@ExtendWith(SystemOutCaptureExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WireMockTest(extensionScanningEnabled = true)
class ClientInstallerTest {

    private static final String VALID_TEST_TOKEN = testToken(Instant.now().plus(5, ChronoUnit.MINUTES));
    private static final String EXPIRED_TEST_TOKEN = testToken(Instant.ofEpochSecond(1000));


    @Test
    void shouldFailWithoutCredentials(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final ClientInstaller installer = new ClientInstaller(null);
        InstallOptions options = new InstallOptions("", "", null, xxhsum);

        assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock)))
            .isInstanceOf(PanicException.class)
            .hasMessage("Username or password is empty");
    }

    @Test
    void shouldFailWithOnlyUsername(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final ClientInstaller installer = new ClientInstaller(null);
        InstallOptions options = new InstallOptions("username", null, null, xxhsum);

        assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock)))
            .isInstanceOf(PanicException.class)
            .hasMessage("Username or password is empty");
    }

    @Test
    void shouldFailWhenStoredSessionTokenIsExpired(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("expired-token.db");

        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {
            configDb.addClient(new Client(TEST_SLUG, TEST_VERSION, COMPLETED, testInstallationPath(tempDir).toAbsolutePath()));
            configDb.addSession(new Session(TEST_SLUG, EXPIRED_TEST_TOKEN));

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG, xxhsum);

            assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock)))
                .isInstanceOf(PanicException.class)
                .hasMessage("Session token has expired: run 'install --username ...' to create a new one");
        }
    }

    @ParameterizedTest
    @EnumSource(FileCheck.class)
    public void shouldInstallAndRepair(FileCheck fileCheck, SystemOutCaptureExtension out, WireMockRuntimeInfo wiremock, @TempDir Path tempDir) throws SQLException {
        shouldInstallClientFromScratch(fileCheck, tempDir, out, wiremock);
        shouldValidateClientAfterInstallation(fileCheck, tempDir, out, wiremock);
        shouldRepairAndReInstallMissingFiles(fileCheck, tempDir, out, wiremock);
        shouldRepairAndFixCorruptedFiles(fileCheck, tempDir, out, wiremock);
        shouldRemoveOrphanFiles(fileCheck, tempDir, out, wiremock);
    }

    void shouldInstallClientFromScratch(FileCheck fileCheck, Path tempDir, SystemOutCaptureExtension out, WireMockRuntimeInfo wiremock) throws SQLException {
        stubAuthenticationFlow(wiremock);
        stubChunkDownload("a1fd9407db7effaf");
        stubChunkDownload("3d8638fbc9718fcb");
        stubChunkDownload("a5700d088b8922a7");
        stubChunkDownload("31054dae2bb797ad");

        final Path dbFile = testConfigDatabase(tempDir);
        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            assertThat(tempDir.resolve("mnm")).doesNotExist();
            assertThat(tempDir.resolve("downloads")).doesNotExist();

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions("username", "password", null, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile, tempDir);

            assertThat(listDirs(tempDir)).containsExactlyInAnyOrder("mnm", "downloads");
            assertThat(tempDir.resolve("mnm")).isNotEmptyDirectory();
            assertThat(tempDir.resolve("downloads")).isNotEmptyDirectory();
            assertThat(tempDir.resolve("downloads").resolve("bundles")).isNotEmptyDirectory();

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(3);
            assertThat(result.orphan()).isEqualTo(0);
        }

        assertThat(out.getOutput())
            .contains("Found 3 missing file(s) to install");
    }

    void shouldValidateClientAfterInstallation(FileCheck fileCheck, Path tempDir, SystemOutCaptureExtension out, WireMockRuntimeInfo wiremock) throws SQLException {
        stubAuthenticationFlow(wiremock);

        final Path dbFile = testConfigDatabase(tempDir);
        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile, tempDir);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(0);
        }

        int start = out.getOutput().lastIndexOf("Connecting with token");
        assertThat(out.getOutput().substring(start))
            .doesNotContain("Found");
    }

    void shouldRepairAndReInstallMissingFiles(FileCheck fileCheck, Path tempDir, SystemOutCaptureExtension out, WireMockRuntimeInfo wiremock) throws SQLException {
        deletePath(testInstallationPath(tempDir).resolve("data"));
        stubAuthenticationFlow(wiremock);

        final Path dbFile = testConfigDatabase(tempDir);
        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile, tempDir);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(1);
            assertThat(result.orphan()).isEqualTo(0);
        }

        assertThat(recentOutput(out))
            .contains("Found 1 missing file(s) to install");
    }

    void shouldRepairAndFixCorruptedFiles(FileCheck fileCheck, Path tempDir, SystemOutCaptureExtension out, WireMockRuntimeInfo wiremock) throws SQLException {
        final Path installationPath = testInstallationPath(tempDir);
        appendToFile(installationPath.resolve("numbers.txt"), "corrupted");
        stubAuthenticationFlow(wiremock);

        final Path dbFile = testConfigDatabase(tempDir);
        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertDatabaseContainsClientAndSession(dbFile, tempDir);

            assertThat(result.invalid()).isEqualTo(1);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(0);
        }

        assertThat(recentOutput(out))
            .contains("Found 1 invalid file(s) to patch");
    }

    void shouldRemoveOrphanFiles(FileCheck fileCheck, Path tempDir, SystemOutCaptureExtension out, WireMockRuntimeInfo wiremock) throws SQLException {
        final Path installationPath = testInstallationPath(tempDir);
        final Path additionalFile1 = installationPath.resolve("unnecessary-1.txt");
        final Path additionalFile2 = installationPath.resolve("unnecessary-2.bin");
        appendToFile(additionalFile1, "some-text");
        appendToFile(additionalFile2, "some-text");

        assertThat(additionalFile1).isNotEmptyFile();
        assertThat(additionalFile2).isNotEmptyFile();

        stubAuthenticationFlow(wiremock);

        final Path dbFile = testConfigDatabase(tempDir);
        try (ConfigDb configDb = ConfigDb.open(dbFile).initialize()) {

            final ClientInstaller installer = new ClientInstaller(configDb);
            InstallOptions options = new InstallOptions(null, null, TEST_SLUG, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock));

            assertThat(additionalFile1).doesNotExist();
            assertThat(additionalFile2).doesNotExist();

            assertDatabaseContainsClientAndSession(dbFile, tempDir);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(2);
        }

        assertThat(recentOutput(out))
            .contains("Found 2 orphan file(s) to delete");
    }

    private static String recentOutput(SystemOutCaptureExtension out) {
        int start = out.getOutput().lastIndexOf("Connecting with token");
        return out.getOutput().substring(start);
    }

    private static void assertDatabaseContainsClientAndSession(Path dbFile, Path tempDir) throws SQLException {
        try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
            assertThat(testDatabase.getTables())
                .containsExactlyInAnyOrder("clients", "sessions");

            final Path installationPath = testInstallationPath(tempDir);
            testDatabase.assertThatTable("clients")
                .containsClient(new Client(TEST_SLUG, TEST_VERSION, COMPLETED, installationPath.toAbsolutePath()))
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

    private static Path testInstallationPath(Path baseDir) {
        return baseDir.resolve(TEST_SLUG);
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
