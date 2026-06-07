package org.mnm.gui;

import javax.swing.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.client.InstallerOptions;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.JwtParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.io.CleanupMode.NEVER;
import static org.mnm.TestUtils.expiredToken;
import static org.mnm.TestUtils.validToken;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.GUI.DEFAULT_SLUG;

// TODO update tests to cover
// Test buttons state change after install, logout
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
    void shouldStartGui(@TempDir(cleanup = NEVER) Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(testClient());
            configDb.addToken(new Token(DEFAULT_SLUG, validToken()));
        }

        GuiCommand command = new GuiCommand(() -> dbFile);

        command.run(Arguments.parse());

        // TODO validate buttons
        command.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldPassClientStatusToGuiStarter(boolean token, @TempDir(cleanup = NEVER) Path tempDir) {
        Path dbFile = tempDir.resolve("config.db");
        Instant expirationTime = null;
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client(DEFAULT_SLUG, "1.0.0", UPDATED, tempDir));
            if (token) {
                String expiredToken = expiredToken();
                expirationTime = JwtParser.parse(expiredToken).expirationTime();
                configDb.addToken(new Token(DEFAULT_SLUG, expiredToken));
            }
        }

        AtomicReference<GuiCommand.ClientStatus> clientRef = new AtomicReference<>();
        Command command = new GuiCommand(() -> dbFile, clientStatus -> {
            clientRef.set(clientStatus);
        }, (_, _, _) -> {
        });

        command.run(Arguments.parse());

        var clientStatus = clientRef.get();
        assertThat(clientStatus.client()).isEqualTo(new Client(DEFAULT_SLUG, "1.0.0", UPDATED, tempDir));
        assertThat(clientStatus.clientUptoDate()).isFalse();
        assertThat(clientStatus.validToken()).isFalse();
        assertThat(clientStatus.expiresAt()).isNull();
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

    private static void waitForButtonEnabled(JButton button) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (button.isEnabled()) {
                return;
            }
            Thread.sleep(20);
        }
    }

}
