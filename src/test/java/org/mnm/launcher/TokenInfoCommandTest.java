package org.mnm.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Command;
import org.sqlite.SQLiteException;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.LauncherTestDatabase.TestDatabase;
import static org.mnm.LauncherTestDatabase.withSettings;

@ExtendWith(SystemOutCaptureExtension.class)
class TokenInfoCommandTest {

    private static String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJtbm0iLCJlbWFpbCI6ImEtdXNlcm5hbWVAc29tZS1lbWFpbC5jb20iLCJleHAiOjE3ODA3OTAyNTcsImlhdCI6MTc3ODM3MTA1NywiaXNzIjoibW5tIiwianRpIjoiYzg2MThkMmEtYTExNy00YmQ4LWJiZmUtZDQwMjJkNWI4MThjIiwibmJmIjoxNzc4MzcxMDU2LCJwdXJwb3NlIjowLCJzdWIiOiI0MjQyNDIiLCJ0eXAiOiJhY2Nlc3MiLCJ2ZXJzaW9uIjoyMX0.8_TEQWuqz4abx3YoXawWRGlnPBVFgm9MigBA4nHt9eA";

    @Test
    void shouldPrintTokenInformation(SystemOutCaptureExtension out, @TempDir Path tempDir) {

        final TestDatabase testDb = withSettings(tempDir);
        testDb.updateSettingsToken(TEST_TOKEN);

        final Command command = new TokenInfoCommand(() -> testDb.path());

        command.run(null);

        assertThat(out.getOutput())
                .isEqualTo("""
                        issuer  : mnm
                        created : 2026-05-09T23:57:37Z
                        expires : 2026-06-06T23:57:37Z
                        email   : a-username@some-email.com
                        """);
    }


    @Test
    void shouldPrintMessageWhenTokenNotPresent(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSettings(tempDir);
        testDb.clearSettingsToken();

        final Command command = new TokenInfoCommand(() -> testDb.path());

        command.run(null);

        assertThat(out.getOutput())
                .isEqualTo("""
                        No token found
                        """);
    }

    @Test
    void shouldFailWhenDatabaseFileNotPresent(SystemOutCaptureExtension out, @TempDir Path tempDir) throws Exception {
        final Path path = tempDir.resolve("not-a-db.db");

        final Command command = new TokenInfoCommand(() -> path);

        Throwable t = catchThrowable(() -> command.run(null));

        assertThat(t)
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageEndingWith("/not-a-db.db");
    }

    @Test
    void shouldPanicWhenPasswordIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) throws Exception {
        final Path path = tempDir.resolve("not-a-db.db");
        path.toFile().createNewFile();
        final Command command = new TokenInfoCommand(() -> path);

        Throwable t = catchThrowable(() -> command.run(null));

        assertThat(t)
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(SQLiteException.class)
                .hasMessage("[SQLITE_ERROR] SQL error or missing database (no such table: settings)");
    }
}
