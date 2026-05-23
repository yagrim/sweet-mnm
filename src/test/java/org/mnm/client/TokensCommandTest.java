package org.mnm.client;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.TestData.TEST_TOKEN;


@ExtendWith(SystemOutCaptureExtension.class)
class TokensCommandTest {

    @Test
    void shouldReturnName() {
        final Command command = new TokensCommand(null);

        assertThat(command.name()).isEqualTo("tokens");
    }

    @Test
    void shouldReturnDescription() {
        final Command command = new TokensCommand(null);

        assertThat(command.description()).isEqualTo("Lists stored tokens");
    }

    @Test
    void shouldReturnHelp() {
        final Command command = new TokensCommand(null);

        assertThat(command.help()).isEqualTo("""
            Lists stored tokens
            
            Usage:
              sweet tokens
            
            Options:
              --slug     Show only tokens for the given client slug
              --debug    Enables debug messages
              --help     Shows this help
            """);
    }

    @Test
    void shouldListAllTokens(@TempDir Path tempDir, SystemOutCaptureExtension capture) {
        final Path dbFile = tempDir.resolve("config.db");
        initSessions(dbFile);

        Command command = new TokensCommand(() -> dbFile);
        command.run(Arguments.parse());

        assertThat(capture.getOutput()).isEqualTo("""
            Slug   Id                      email            Expiration
            mnm-1  1   a-username@some-email.com  2026-06-06T23:57:37Z
            mnm-1  2   a-username@some-email.com  2026-06-06T23:57:37Z
            mnm-2  3   a-username@some-email.com  2026-06-06T23:57:37Z
            """);
    }

    @Test
    void shouldListTokensForSlug(@TempDir Path tempDir, SystemOutCaptureExtension capture) {
        final Path dbFile = tempDir.resolve("config.db");
        initSessions(dbFile);

        Command command = new TokensCommand(() -> dbFile);
        command.run(Arguments.parse("--slug", "mnm-2"));

        assertThat(capture.getOutput()).isEqualTo("""
            Slug   Id                      email            Expiration
            mnm-2  3   a-username@some-email.com  2026-06-06T23:57:37Z
            """);
    }

    @Test
    void shouldPrintMessageWhenThereAreNoTokens(@TempDir Path tempDir, SystemOutCaptureExtension capture) {
        final Path dbFile = tempDir.resolve("config.db");

        Command command = new TokensCommand(() -> dbFile);
        command.run(Arguments.parse());

        assertThat(capture.getOutput()).isEqualTo("No tokens found\n");
    }

    @Test
    void shouldPrintMessageWhenThereAreNoTokensForSlug(@TempDir Path tempDir, SystemOutCaptureExtension capture) {
        final Path dbFile = tempDir.resolve("config.db");
        initSessions(dbFile);

        Command command = new TokensCommand(() -> dbFile);
        command.run(Arguments.parse("--slug", "mnm-3"));

        assertThat(capture.getOutput()).isEqualTo("No tokens found\n");
    }

    private static void initSessions(Path dbFile) {
        try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
            Client client = new Client("mnm-1", "v1.0.0", Client.Status.COMPLETED, Path.of("/install/path"));
            config.addClient(client);
            config.addSession(new Session(client.slug(), TEST_TOKEN));
            config.addSession(new Session(client.slug(), TEST_TOKEN));
            Client client2 = new Client("mnm-2", "v1.0.0", Client.Status.COMPLETED, Path.of("/install/path"));
            config.addClient(client2);
            config.addSession(new Session(client2.slug(), TEST_TOKEN));
        }
    }
}
