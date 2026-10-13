package org.mnm.gui;

import java.nio.file.Path;
import java.util.List;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.api.Session;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.client.InstallerOptions;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.io.CleanupMode.NEVER;
import static org.mnm.ApiServerStubs.stubAccountLogin;
import static org.mnm.ApiServerStubs.stubGameVersions;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.TestUtils.validToken;
import static org.mnm.config.Client.Status.NOT_INSTALLED;
import static org.mnm.config.SettingsStore.SELECTED_SLUG_KEY;
import static org.mnm.config.Client.Status.UPDATED;

// TODO update tests to cover
// Test buttons state change after install, logout
@WireMockTest(httpsEnabled = true)
class GuiCommandTest {

    @Test
    void shouldExposeCommandMetadata() {
        Command command = new GuiCommand();

        assertThat(command.name()).isEqualTo("gui");
        assertThat(command.description()).isEqualTo("Opens a simple Swing interface");
        assertThat(command.help()).contains("sweet gui");
    }

    // Fails in Linux CI with "No X11 DISPLAY variable was set"
    // Windows fails cleanup because db file is locked
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldStartGuiClientIsNotFound(@TempDir(cleanup = NEVER) Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        GuiCommand command = new GuiCommand(() -> dbFile);

        command.run(Arguments.parse());

        // TODO validate buttons
        command.close();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldStartGui(WireMockRuntimeInfo wiremock, @TempDir(cleanup = NEVER) Path tempDir) {
        stubAccountLogin();
        stubGameVersions();

        final Path dbFile = tempDir.resolve("config.db");
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(testClient());
            configDb.addToken(new Token("mnm", validToken()));
        }

        GuiCommand command = new GuiCommand(() -> dbFile);

        command.run(Arguments.parse(
            "--dev-options", "true",
            "--api-endpoint", wiremock.getHttpBaseUrl()
        ));

        // TODO validate buttons
        command.close();
    }

    @Test
    void shouldBuildRepairOptionsWithSlugOnly() {
        InstallerOptions options = InstallerOptions.forRepair("mnm", false);

        assertThat(options.username()).isNull();
        assertThat(options.password()).isNull();
        assertThat(options.slug()).isEqualTo("mnm");
        assertThat(options.fileCheck()).isNotNull();
        assertThat(options.toString()).contains("xxhsum");
    }

    @Test
    void shouldReadEmptyGuiStateWhenThereAreNoClients(@TempDir Path tempDir) {
        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"))) {

            GuiState guiState = GuiCommand.readGuiState(configDb, null);

            assertThat(guiState.slugs()).isEmpty();
            assertThat(guiState.selectedSlug()).isNull();
            assertThat(guiState.clientStatus().client()).isNull();
        }
    }

    @Test
    void shouldReadGuiStateWithPreferredSlug(@TempDir Path tempDir) {
        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"))) {
            addClients(configDb, tempDir, "mnm-test", "mnm", "other");

            GuiState guiState = GuiCommand.readGuiState(configDb, "other");

            assertThat(guiState.slugs()).containsExactly("mnm-test", "mnm", "other");
            assertThat(guiState.selectedSlug()).isEqualTo("other");
            assertThat(guiState.clientStatus().client().slug()).isEqualTo("other");
            assertThat(guiState.clientStatus().validToken()).isTrue();
        }
    }

    @Test
    void shouldReadGuiStateWithMnmWhenPreferredSlugIsNotFound(@TempDir Path tempDir) {
        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"))) {
            addClients(configDb, tempDir, "mnm-test", "mnm");

            GuiState guiState = GuiCommand.readGuiState(configDb, "removed");

            assertThat(guiState.selectedSlug()).isEqualTo("mnm");
        }
    }

    @Test
    void shouldReadGuiStateWithFirstSlugWhenMnmIsNotFound(@TempDir Path tempDir) {
        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"))) {
            addClients(configDb, tempDir, "alpha", "beta");

            GuiState guiState = GuiCommand.readGuiState(configDb, null);

            assertThat(guiState.selectedSlug()).isEqualTo("alpha");
        }
    }

    @Test
    void shouldReadGuiStateForSelectedClientWithExpiredToken(@TempDir Path tempDir) {
        try (ConfigDb configDb = ConfigDb.open(tempDir.resolve("config.db"))) {
            configDb.addClient(new Client("mnm", "1.2.3", NOT_INSTALLED, tempDir));
            configDb.addToken(new Token("mnm", expiredToken()));

            GuiState guiState = GuiCommand.readGuiState(configDb, null);

            assertThat(guiState.slugs()).containsExactly("mnm");
            assertThat(guiState.selectedSlug()).isEqualTo("mnm");
            assertThat(guiState.clientStatus().validToken()).isFalse();
        }
    }

    @Test
    void shouldSelectSlugAndStoreItWithoutApiCalls(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            addClients(configDb, tempDir, "mnm-test", "mnm");
        }

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {

            ClientStatus clientStatus = GuiCommand.selectSlug(() -> dbFile, "mnm-test");

            assertThat(clientStatus.client().slug()).isEqualTo("mnm-test");
            assertThat(clientStatus.validToken()).isTrue();
            sessionMock.verifyNoInteractions();
        }
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            assertThat(configDb.getSettings(SELECTED_SLUG_KEY)).isEqualTo("mnm-test");
            // Stored selection is used on next start
            assertThat(GuiCommand.readGuiState(configDb, configDb.getSettings(SELECTED_SLUG_KEY)).selectedSlug()).isEqualTo("mnm-test");
        }
    }

    private static void addClients(ConfigDb configDb, Path path, String... slugs) {
        for (String slug : slugs) {
            configDb.addClient(new Client(slug, "1.2.3", UPDATED, path));
            configDb.addToken(new Token(slug, validToken()));
        }
    }

    static Client testClient() {
        return new Client("mnm", "1.2.3", UPDATED, Path.of("."));
    }

}
