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
import org.mnm.config.Token;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
class ClientsCommandTest {

    @Test
    void shouldReturnName() {
        final Command command = new ClientsCommand(null);

        assertThat(command.name()).isEqualTo("clients");
    }

    @Test
    void shouldReturnDescription() {
        final Command command = new ClientsCommand(null);

        assertThat(command.description()).isEqualTo("Lists configured clients");
    }

    @Test
    void shouldReturnHelp() {
        final Command command = new ClientsCommand(null);

        assertThat(command.help()).isEqualTo("""
            Lists configured clients
            
            Usage:
              sweet clients
            
            Options:
              --debug  Enables debug messages
              --help   Shows this help
            """);
    }

    @Test
    void shouldListClientsWithTokensCount(@TempDir Path tempDir, SystemOutCaptureExtension capture) {
        final Path dbFile = tempDir.resolve("config.db");

        try (ConfigDb config = ConfigDb.open(dbFile).initialize()) {
            Client mnm = testClient("mnm");
            Client ptr = testClient("ptr");
            config.addClient(mnm);
            config.addClient(ptr);
            config.addToken(new Token(mnm.slug(), "token-1"));
            config.addToken(new Token(mnm.slug(), "token-2"));
            config.addToken(new Token(ptr.slug(), "token-3"));
        }

        Command command = new ClientsCommand(() -> dbFile);
        command.run(Arguments.parse());

        assertThat(capture.getOutput()).isEqualTo("""
            Slug  Version  Tokens  Install_path
            mnm   v1.0.0        2  /install/mnm
            ptr   v1.0.0        1  /install/ptr
            """);
    }

    @Test
    void shouldInitializeConfigDatabaseBeforeListingClients(@TempDir Path tempDir, SystemOutCaptureExtension capture) {
        final Path dbFile = tempDir.resolve("config.db");

        Command command = new ClientsCommand(() -> dbFile);
        command.run(Arguments.parse());

        assertThat(capture.getOutput()).isEqualTo("No clients found\n".formatted());
    }

    private static Client testClient(String slug) {
        return new Client(slug, "v1.0.0", Client.Status.COMPLETED, Path.of("/install/" + slug));
    }
}
