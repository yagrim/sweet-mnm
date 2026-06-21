package org.mnm.client;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.ConfigTestDatabase;
import org.mnm.GeneralOptions;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(SystemOutCaptureExtension.class)
class LogoutCommandTest {

    @Test
    void shouldReturnName() {
        final Command command = new LogoutCommand(null);

        assertThat(command.name()).isEqualTo("logout");
    }

    @Test
    void shouldReturnDescription() {
        final Command command = new LogoutCommand(null);

        assertThat(command.description()).isEqualTo("Removes stored tokens for a client slug");
    }

    @Test
    void shouldReturnHelp() {
        final Command command = new LogoutCommand(null);

        assertThat(command.help()).isEqualTo("""
            Removes stored tokens for a client slug
            
            Usage:
              sweet logout --slug <slug>
            
            Options:
              --slug     Client slug whose tokens will be removed (required)
              --debug    Enables debug messages
              --help     Shows this help
            """);
    }

    @Test
    void shouldDeleteTokensForSlug(@TempDir Path tempDir, SystemOutCaptureExtension out) {
        GeneralOptions.setInfo(true);

        final Path dbFile = tempDir.resolve("config.db");
        initTokens(dbFile);

        Command command = new LogoutCommand(() -> dbFile);
        command.run(Arguments.parse("--slug", "mnm-1"));

        assertThat(out.getOutput()).endsWith("""
            INFO  LogoutCommand - Removed 2 token(s) for slug 'mnm-1'
            """);

        try (ConfigTestDatabase.TestDatabase testDatabase = ConfigTestDatabase.open(dbFile)) {
            testDatabase.assertThatTable("tokens")
                .containsToken(new Token(3, "mnm-2", "token-3"))
                .hasRows(1);
        }
    }

    @Test
    void shouldDeleteNoTokensWhenSlugHasNoMatches(@TempDir Path tempDir, SystemOutCaptureExtension out) {
        final Path dbFile = tempDir.resolve("config.db");
        initTokens(dbFile);

        Command command = new LogoutCommand(() -> dbFile);
        command.run(Arguments.parse("--slug", "unknown"));

        assertThat(out.getOutput()).endsWith("""
            INFO  LogoutCommand - Removed 0 token(s) for slug 'unknown'
            """);

        try (ConfigTestDatabase.TestDatabase testDatabase = ConfigTestDatabase.open(dbFile)) {
            testDatabase.assertThatTable("tokens").hasRows(3);
        }
    }

    @Test
    void shouldPanicWhenSlugIsMissing() {
        final Command command = new LogoutCommand(null);

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--slug'");
    }

    private static void initTokens(Path dbFile) {
        try (ConfigDb config = ConfigDb.open(dbFile)) {
            Client client1 = new Client("mnm-1", "v1.0.0", Client.Status.UPDATED, Path.of("/install/path"));
            Client client2 = new Client("mnm-2", "v1.0.0", Client.Status.UPDATED, Path.of("/install/path"));
            config.addClient(client1);
            config.addClient(client2);
            config.addToken(new Token(client1.slug(), "token-1"));
            config.addToken(new Token(client1.slug(), "token-2"));
            config.addToken(new Token(client2.slug(), "token-3"));
        }
    }
}
