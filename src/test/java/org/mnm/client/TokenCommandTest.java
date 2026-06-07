package org.mnm.client;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.TestData.TEST_TOKEN;

@ExtendWith(SystemOutCaptureExtension.class)
class TokenCommandTest {

    @Test
    void shouldReturnName() {
        final Command command = new TokenCommand(null);

        assertThat(command.name()).isEqualTo("token");
    }

    @Test
    void shouldReturnDescription() {
        final Command command = new TokenCommand(null);

        assertThat(command.description()).isEqualTo("Shows stored token by id");
    }

    @Test
    void shouldReturnHelp() {
        final Command command = new TokenCommand(null);

        assertThat(command.help()).isEqualTo("""
            Shows stored token by id
            
            Usage:
              sweet token --id <id>
            
            Options:
              --id       Token id (required)
              --output   Set output format: 'raw' for JWT token (default ), 'rows' for metadata
              --debug    Enables debug messages
              --help     Shows this help
            """);
    }

    @ParameterizedTest
    @MethodSource("outputRawOptions")
    void shouldReturnTokenAsRaw(List<String> options, @TempDir Path tempDir, SystemOutCaptureExtension out) {
        final Path dbFile = tempDir.resolve("config.db");
        initClientToken(dbFile);

        Command command = new TokenCommand(() -> dbFile);
        command.run(Arguments.parse(Stream.concat(Arrays.stream(new String[]{"--id", "1"}), options.stream()).toArray(String[]::new)));

        assertThat(out.getOutput()).isEqualTo("""
            %s
            """.formatted(TEST_TOKEN));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> outputRawOptions() {
        return Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(List.of()),
            org.junit.jupiter.params.provider.Arguments.of(List.of("--output")),
            org.junit.jupiter.params.provider.Arguments.of(List.of("--output", "")),
            org.junit.jupiter.params.provider.Arguments.of(List.of("--output", "raw"))
        );
    }

    @Test
    void shouldReturnTokenAsRows(@TempDir Path tempDir, SystemOutCaptureExtension out) {
        final Path dbFile = tempDir.resolve("config.db");
        initClientToken(dbFile);

        Command command = new TokenCommand(() -> dbFile);
        command.run(Arguments.parse("--id", "1", "--output", "rows"));

        assertThat(out.getOutput())
            .isEqualTo("""
                issuer  : mnm
                created : 2026-05-09T23:57:37Z
                expires : 2026-06-06T23:57:37Z
                email   : a-username@some-email.com
                """);
    }

    @Test
    void shouldPrintNoTokenFoundWhenTokenDoesNotExist(@TempDir Path tempDir, SystemOutCaptureExtension out) {
        final Path dbFile = tempDir.resolve("config.db");

        Command command = new TokenCommand(() -> dbFile);
        command.run(Arguments.parse("--id", "99"));

        assertThat(out.getOutput()).isEqualTo("""
            No token found for id 99
            """);
    }

    @Test
    void shouldPanicWhenOutputIsInvalid(@TempDir Path tempDir) {
        final Path dbFile = tempDir.resolve("config.db");
        initClientToken(dbFile);

        Command command = new TokenCommand(() -> dbFile);

        assertThatThrownBy(() -> command.run(Arguments.parse("--id", "1", "--output", "random")))
            .isInstanceOf(PanicException.class)
            .hasMessage("Invalid output: random");
    }

    @Test
    void shouldPanicWhenIdIsMissing() {
        final Command command = new TokenCommand(null);

        assertThatThrownBy(() -> command.run(Arguments.parse()))
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--id'");
    }

    @Test
    void shouldPanicWhenIdIsNotAnInteger() {
        final Command command = new TokenCommand(null);

        assertThatThrownBy(() -> command.run(Arguments.parse("--id", "abc")))
            .isInstanceOf(PanicException.class)
            .hasMessage("Invalid id: expected integer but found abc");
    }

    private static void initClientToken(Path dbFile) {
        try (ConfigDb config = ConfigDb.open(dbFile)) {
            Client client = new Client("mnm-1", "v1.0.0", Client.Status.UPDATED, Path.of("/install/path"));
            config.addClient(client);
            config.addToken(new Token(client.slug(), TEST_TOKEN));
        }
    }

}
