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
import org.mnm.LoggerHandler;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.api.ApiConnector;
import org.mnm.api.RestClient;
import org.mnm.client.ClientInstaller.InstallationResult;
import org.mnm.client.InstallerOptions.FileCheck;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.ApiServerStubs.TEST_SLUG;
import static org.mnm.ApiServerStubs.TEST_VERSION;
import static org.mnm.ApiServerStubs.stubAccountLogin;
import static org.mnm.ApiServerStubs.stubChunkDownload;
import static org.mnm.ApiServerStubs.stubEmptyManifestDownload;
import static org.mnm.ApiServerStubs.stubGameVersions;
import static org.mnm.ApiServerStubs.stubManifestDownload;
import static org.mnm.TestUtils.appendToFile;
import static org.mnm.TestUtils.deletePath;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.TestUtils.testToken;
import static org.mnm.TestUtils.validToken;
import static org.mnm.client.InstallerOptions.FileCheck.xxhsum;
import static org.mnm.config.Client.Status.INSTALLING;
import static org.mnm.config.Client.Status.UPDATED;


@ExtendWith(SystemOutCaptureExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WireMockTest(extensionScanningEnabled = true)
class ClientInstallerTest {

    private static final String VALID_TOKEN = validToken();
    private static final String EXPIRED_TOKEN = expiredToken();

    @Test
    void shouldFailWithoutCredentials(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final ClientInstaller installer = new ClientInstaller(null, null);
        InstallerOptions options = new InstallerOptions("", "", null, xxhsum);

        assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null))
            .isInstanceOf(PanicException.class)
            .hasMessage("Username or password is empty");
    }

    @Test
    void shouldFailWithOnlyUsername(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final ClientInstaller installer = new ClientInstaller(null, null);
        InstallerOptions options = new InstallerOptions("username", null, null, xxhsum);

        assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null))
            .isInstanceOf(PanicException.class)
            .hasMessage("Username or password is empty");
    }

    @Test
    void shouldFailWhenNoClientIsFound(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("missing-client.db");

        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            final ClientInstaller installer = new ClientInstaller(configDb, null);
            InstallerOptions options = new InstallerOptions(null, null, TEST_SLUG, xxhsum);

            assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null))
                .isInstanceOf(PanicException.class)
                .hasMessage("No client found: run 'install --username ...' first");
        }
    }

    @Test
    void shouldFailWhenNoTokenIsFound(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("missing-client.db");

        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client(TEST_SLUG, TEST_VERSION, UPDATED, Path.of("")));

            final ClientInstaller installer = new ClientInstaller(configDb, null);
            InstallerOptions options = new InstallerOptions(null, null, TEST_SLUG, xxhsum);

            assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null))
                .isInstanceOf(PanicException.class)
                .hasMessage("No client found: run 'install --username ...' first");
        }
    }

    @Test
    void shouldFailWhenStoredTokenIsExpired(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("expired-token.db");

        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client(TEST_SLUG, TEST_VERSION, UPDATED, testInstallationPath(tempDir).toAbsolutePath()));
            configDb.addToken(new Token(TEST_SLUG, EXPIRED_TOKEN));

            final ClientInstaller installer = new ClientInstaller(configDb, null);
            InstallerOptions options = new InstallerOptions(null, null, TEST_SLUG, xxhsum);

            assertThatThrownBy(() -> installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null))
                .isInstanceOf(PanicException.class)
                .hasMessage("All token(s) expired: run 'install --username ...' to create a new one");
        }
    }

    @Test
    void shouldRefreshExpiredStoredToken(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final String refreshToken = testToken(Instant.now().plus(10, ChronoUnit.MINUTES));
        final String httpBaseUrl = wiremock.getHttpBaseUrl();
        stubAccountLogin(refreshToken);
        stubGameVersions(httpBaseUrl);
        stubEmptyManifestDownload();

        final Path dbFile = tempDir.resolve("refresh-expired-token.db");

        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir));
            configDb.addToken(new Token(TEST_SLUG, VALID_TOKEN));
            configDb.addToken(new Token(TEST_SLUG, EXPIRED_TOKEN));
            configDb.addToken(new Token(TEST_SLUG, VALID_TOKEN));

            final ApiConnector apiConnector = new ApiConnector(new RestClient(httpBaseUrl));
            final ClientInstaller installer = new ClientInstaller(configDb, apiConnector);
            InstallerOptions options = new InstallerOptions("username", "password", null, xxhsum);

            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(0);
        }

        try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
            testDatabase.assertThatTable("clients")
                .containsClient(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir))
                .hasRows(1);
            testDatabase.assertThatTable("tokens")
                .containsToken(new Token(1, TEST_SLUG, VALID_TOKEN))
                .containsToken(new Token(2, TEST_SLUG, refreshToken))
                .containsToken(new Token(3, TEST_SLUG, VALID_TOKEN))
                .hasRows(3);
        }
    }

    @Test
    void shouldRefreshFirstTokenWhenAllAreValid(WireMockRuntimeInfo wiremock, @TempDir Path tempDir) {
        final String refreshToken = testToken(Instant.now().plus(10, ChronoUnit.MINUTES));
        final String httpBaseUrl = wiremock.getHttpBaseUrl();
        stubAccountLogin(refreshToken);
        stubGameVersions(httpBaseUrl);
        stubEmptyManifestDownload();

        final Path dbFile = tempDir.resolve("refresh-expired-token.db");

        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir));
            configDb.addToken(new Token(TEST_SLUG, VALID_TOKEN));
            configDb.addToken(new Token(TEST_SLUG, VALID_TOKEN));
            configDb.addToken(new Token(TEST_SLUG, VALID_TOKEN));

            ApiConnector apiConnector = new ApiConnector(new RestClient(httpBaseUrl));
            final ClientInstaller installer = new ClientInstaller(configDb, apiConnector);
            InstallerOptions options = new InstallerOptions("username", "password", null, xxhsum);

            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(0);
        }

        try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
            testDatabase.assertThatTable("clients")
                .containsClient(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir))
                .hasRows(1);
            testDatabase.assertThatTable("tokens")
                .containsToken(new Token(1, TEST_SLUG, refreshToken))
                .containsToken(new Token(2, TEST_SLUG, VALID_TOKEN))
                .containsToken(new Token(3, TEST_SLUG, VALID_TOKEN))
                .hasRows(3);
        }
    }

    @ParameterizedTest
    @EnumSource(FileCheck.class)
    public void shouldInstallAndRepair(FileCheck fileCheck, SystemOutCaptureExtension out, WireMockRuntimeInfo wiremock, @TempDir Path tempDir) throws SQLException {
        LoggerHandler.setInfo(true);

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
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {

            assertThat(tempDir.resolve("mnm")).doesNotExist();
            assertThat(tempDir.resolve("downloads")).doesNotExist();

            final ApiConnector apiConnector = new ApiConnector(new RestClient(wiremock.getHttpBaseUrl()));
            final ClientInstaller installer = new ClientInstaller(configDb, apiConnector);
            InstallerOptions options = new InstallerOptions("username", "password", null, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null);

            assertDatabaseContainsClientAndToken(dbFile, tempDir);

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
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {

            final ApiConnector apiConnector = new ApiConnector(new RestClient(wiremock.getHttpBaseUrl()));
            final ClientInstaller installer = new ClientInstaller(configDb, apiConnector);
            InstallerOptions options = new InstallerOptions(null, null, TEST_SLUG, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null);

            assertDatabaseContainsClientAndToken(dbFile, tempDir);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(0);
        }

        int start = out.getOutput().lastIndexOf("Session - Authenticating");
        assertThat(out.getOutput().substring(start))
            .doesNotContain("Found");
    }

    void shouldRepairAndReInstallMissingFiles(FileCheck fileCheck, Path tempDir, SystemOutCaptureExtension out, WireMockRuntimeInfo wiremock) throws SQLException {
        deletePath(testInstallationPath(tempDir).resolve("data"));
        stubAuthenticationFlow(wiremock);

        final Path dbFile = testConfigDatabase(tempDir);
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {

            final ApiConnector apiConnector = new ApiConnector(new RestClient(wiremock.getHttpBaseUrl()));
            final ClientInstaller installer = new ClientInstaller(configDb, apiConnector);
            InstallerOptions options = new InstallerOptions(null, null, TEST_SLUG, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null);

            assertDatabaseContainsClientAndToken(dbFile, tempDir);

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
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {

            final ApiConnector apiConnector = new ApiConnector(new RestClient(wiremock.getHttpBaseUrl()));
            final ClientInstaller installer = new ClientInstaller(configDb, apiConnector);
            InstallerOptions options = new InstallerOptions(null, null, TEST_SLUG, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null);

            assertDatabaseContainsClientAndToken(dbFile, tempDir);

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
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {

            final ApiConnector apiConnector = new ApiConnector(new RestClient(wiremock.getHttpBaseUrl()));
            final ClientInstaller installer = new ClientInstaller(configDb, apiConnector);
            InstallerOptions options = new InstallerOptions(null, null, TEST_SLUG, fileCheck);
            InstallationResult result = installer.install(options, tempDir, mockApiBaseUrl(wiremock), INSTALLING, null);

            assertThat(additionalFile1).doesNotExist();
            assertThat(additionalFile2).doesNotExist();

            assertDatabaseContainsClientAndToken(dbFile, tempDir);

            assertThat(result.invalid()).isEqualTo(0);
            assertThat(result.missing()).isEqualTo(0);
            assertThat(result.orphan()).isEqualTo(2);
        }

        assertThat(recentOutput(out))
            .contains("Found 2 orphan file(s) to delete");
    }

    private static String recentOutput(SystemOutCaptureExtension out) {
        int start = out.getOutput().lastIndexOf("Authenticating");
        return out.getOutput().substring(start);
    }

    private static void assertDatabaseContainsClientAndToken(Path dbFile, Path tempDir) throws SQLException {
        try (var testDatabase = ConfigTestDatabase.open(dbFile)) {
            assertThat(testDatabase.getTables())
                .containsExactlyInAnyOrder("clients", "tokens", "settings");

            testDatabase.assertThatTable("clients")
                .containsClient(new Client(TEST_SLUG, TEST_VERSION, UPDATED, tempDir))
                .hasRows(1);
            testDatabase.assertThatTable("tokens")
                .containsToken(new Token(1, TEST_SLUG, VALID_TOKEN))
                .hasRows(1);
        }
    }

    private static void stubAuthenticationFlow(WireMockRuntimeInfo wiremock) {
        stubAccountLogin(VALID_TOKEN);
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
