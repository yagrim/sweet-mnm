package org.mnm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sqlite.SQLiteException;

import org.mnm.LauncherTestDatabase;
import org.mnm.LauncherTestDatabase.TestDatabase;
import org.mnm.LinuxOnlyCommand;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.*;
import static org.mnm.LauncherTestDatabase.withSchema;
import static org.mnm.LauncherTestDatabase.withSettings;

@ExtendWith(SystemOutCaptureExtension.class)
class TokenCommandTest extends LinuxOnlyCommand {

    private static String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJtbm0iLCJlbWFpbCI6ImEtdXNlcm5hbWVAc29tZS1lbWFpbC5jb20iLCJleHAiOjE3ODA3OTAyNTcsImlhdCI6MTc3ODM3MTA1NywiaXNzIjoibW5tIiwianRpIjoiYzg2MThkMmEtYTExNy00YmQ4LWJiZmUtZDQwMjJkNWI4MThjIiwibmJmIjoxNzc4MzcxMDU2LCJwdXJwb3NlIjowLCJzdWIiOiI0MjQyNDIiLCJ0eXAiOiJhY2Nlc3MiLCJ2ZXJzaW9uIjoyMX0.8_TEQWuqz4abx3YoXawWRGlnPBVFgm9MigBA4nHt9eA";


    @Test
    void shouldReturnHelp() {
        final Command command = new TokenCommand(null);

        assertThat(command.help()).isEqualTo("""
            Shows official launcher current token
            
            Usage:
              sweet launcher-token
            
            Options:
              --outout   Set output format: 'raw' for JWT token (default ), 'rows' for metadata
              --debug    Enables debug messages
              --help     Shows this help
            """);
    }

    @ParameterizedTest
    @MethodSource("outputRawOptions")
    void shouldReturnTokenAsRaw(List<String> args, SystemOutCaptureExtension out) {
        Command command = new TokenCommand(LauncherTestDatabase::fromClasspath);

        command.run(Arguments.parse(args.toArray(new String[0])));

        assertThat(out.getOutput()).isEqualTo("""
            123.456.789
            """);
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
    void shouldReturnTokenAsRaw(SystemOutCaptureExtension out) {
        final Command command = new TokenCommand(LauncherTestDatabase::fromClasspath);

        command.run(Arguments.parse("--output", "raw"));

        assertThat(out.getOutput()).isEqualTo("""
            123.456.789
            """);
    }

    @Test
    void shouldReturnTokenAsRawWhenSetAsBooleanFlag(SystemOutCaptureExtension out) {
        final Command command = new TokenCommand(LauncherTestDatabase::fromClasspath);

        command.run(Arguments.parse("--output"));

        assertThat(out.getOutput()).isEqualTo("""
            123.456.789
            """);
    }

    @Test
    void shouldShowTokenAsRows(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSettings(tempDir);
        testDb.updateSettingsToken(TEST_TOKEN);

        final Command command = new TokenCommand(() -> testDb.path());

        command.run(Arguments.parse("--output", "rows"));

        assertThat(out.getOutput())
            .isEqualTo("""
                issuer  : mnm
                created : 2026-05-09T23:57:37Z
                expires : 2026-06-06T23:57:37Z
                email   : a-username@some-email.com
                """);
    }

    @Test
    void shouldPanicWhenOutputIsNotValid() {
        final Command command = new TokenCommand(LauncherTestDatabase::fromClasspath);

        assertThatThrownBy(() -> command.run(Arguments.parse("--output", "random")))
            .isInstanceOf(PanicException.class)
            .hasMessage("Invalid output: random");
    }

    @Test
    void shouldPrintMessageWhenTokenIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSchema(tempDir);
        Command token = new TokenCommand(testDb::path);

        token.run(null);

        assertThat(out.getOutput()).isEqualTo("""
            No token found
            """);
    }

    @Test
    void shouldFailWhenDatabaseFileNotPresent(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final Path path = tempDir.resolve("not-a-db.db");

        final Command command = new TokenCommand(() -> path);

        Throwable t = catchThrowable(() -> command.run(null));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .cause()
            .isInstanceOf(FileNotFoundException.class)
            .hasMessageEndingWith(File.separator + "not-a-db.db");
    }

    @Test
    void shouldPanicWhenPasswordIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) throws Exception {
        final Path path = tempDir.resolve("not-a-db.db");
        path.toFile().createNewFile();
        final Command command = new TokenCommand(() -> path);

        Throwable t = catchThrowable(() -> command.run(null));

        assertThat(t)
            .isInstanceOf(RuntimeException.class)
            .cause()
            .isInstanceOf(SQLiteException.class)
            .hasMessage("[SQLITE_ERROR] SQL error or missing database (no such table: settings)");
    }

    @Override
    protected Command buildCommand() {
        return new TokenCommand(null);
    }

}
