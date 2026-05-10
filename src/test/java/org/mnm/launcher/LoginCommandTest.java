package org.mnm.launcher;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.LauncherTestDatabase;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.PanicException;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.ApiServerStubs.stubAccountLogin;
import static org.mnm.LauncherTestDatabase.INITIAL_TOKEN;
import static org.mnm.LauncherTestDatabase.copyTestDb;

@ExtendWith(SystemOutCaptureExtension.class)
@WireMockTest(httpsEnabled = true)
class LoginCommandTest {

    @Test
    void shouldLoginAndUpdateToken(SystemOutCaptureExtension out,
                                   WireMockRuntimeInfo wiremock,
                                   @TempDir Path tempDir) throws Exception {
        String uuid = UUID.randomUUID().toString();
        stubAccountLogin(uuid);

        final Path path = copyTestDb(tempDir);
        final Command command = new LoginCommand(() -> path);

        assertTokenInDbHasNotBeenModified(path);

        Arguments arguments = new Arguments(Map.of(
                "username", "username",
                "password", "password",
                "dev-options", "true",
                "api-endpoint", wiremock.getHttpBaseUrl()
        ));
        command.run(arguments);

        assertThat(out.getOutput())
                .contains("LoginCommand - DEVELOPER OPTIONS ENABLED!")
                .contains("LoginCommand - If you see this line, proceed at your own risk")
                .endsWith("Token updated in launcher database\n");

        try (LauncherDb launcherDb = new LauncherDb(path)) {
            assertThat(launcherDb.getSettings()).containsEntry("token", uuid);
        }
    }

    @Test
    void shouldLoginAndReturnTokenOnly(SystemOutCaptureExtension out,
                                   WireMockRuntimeInfo wiremock,
                                   @TempDir Path tempDir) throws Exception {
        String uuid = UUID.randomUUID().toString();
        stubAccountLogin(uuid);

        final Path path = copyTestDb(tempDir);
        final Command command = new LoginCommand(() -> path);

        assertTokenInDbHasNotBeenModified(path);

        Arguments arguments = new Arguments(Map.of(
                "dev-options", "true",
                "api-endpoint", wiremock.getHttpBaseUrl(),
                "username", "username",
                "password", "password",
                "ignore-update", "true"
        ));
        command.run(arguments);

        assertThat(out.getOutput())
                .contains("LoginCommand - DEVELOPER OPTIONS ENABLED!")
                .contains("LoginCommand - If you see this line, proceed at your own risk")
                .endsWith("Skipping token update in launcher database\n");

        try (LauncherDb launcherDb = new LauncherDb(path)) {
            assertThat(launcherDb.getSettings()).containsEntry("token", INITIAL_TOKEN);
        }
    }

    @Test
    void shouldPanicWhenUsernameIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) throws Exception {
        final Path path = copyTestDb(tempDir);
        final Command command = new LoginCommand(() -> path);

        assertTokenInDbHasNotBeenModified(path);
        Arguments arguments = new Arguments(Map.of("password", "password"));
        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing parameter: '--username'");

        assertTokenInDbHasNotBeenModified(path);
    }

    @Test
    void shouldPanicWhenPasswordIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) throws Exception {
        final Path path = copyTestDb(tempDir);
        final Command command = new LoginCommand(() -> path);

        assertTokenInDbHasNotBeenModified(path);

        Arguments arguments = new Arguments(Map.of("username", "username"));
        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing parameter: '--password'");

        assertTokenInDbHasNotBeenModified(path);
    }

    private static void assertTokenInDbHasNotBeenModified(Path path) throws Exception {
        try (LauncherDb launcherDb = new LauncherDb(path)) {
            assertThat(launcherDb.getSettings()).containsEntry("token", INITIAL_TOKEN);
        }
    }
}
