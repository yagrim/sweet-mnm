package org.mnm.gui;

import java.nio.file.Path;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

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
import static org.mnm.TestUtils.validToken;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.GUI.DEFAULT_SLUG;

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
            configDb.addToken(new Token(DEFAULT_SLUG, validToken()));
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

    static Client testClient() {
        return new Client(DEFAULT_SLUG, "1.2.3", UPDATED, Path.of("."));
    }

}
