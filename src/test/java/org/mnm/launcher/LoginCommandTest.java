package org.mnm.launcher;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mnm.LauncherTestDatabase.TestDatabase;
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
import static org.mnm.LauncherTestDatabase.withSettings;

@ExtendWith(SystemOutCaptureExtension.class)
@WireMockTest(httpsEnabled = true)
class LoginCommandTest {

    @Test
    void shouldReturnHelp() {
        final Command command = new LoginCommand(null);

        assertThat(command.help()).isEqualTo("""
                Login with your username and password (updates launcher database)
                
                Usage:
                  sweet login --username <username> --password <password>
                
                Options:
                  --username       MnM account username (required)
                  --password       MnM account password (required)
                  --ignore-update  Prints the token without updating the launcher database
                  --help           Shows this help
                """);
    }

    @Test
    void shouldLoginAndUpdateToken(SystemOutCaptureExtension out,
                                   WireMockRuntimeInfo wiremock,
                                   @TempDir Path tempDir) {
        String uuid = UUID.randomUUID().toString();
        stubAccountLogin(uuid);

        final TestDatabase testDb = withSettings(tempDir);
        final Command command = new LoginCommand(() -> testDb.path());

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);

        Arguments arguments = new Arguments(Map.of(
                "username", "username",
                "password", "password",
                "dev-options", "true",
                "api-endpoint", wiremock.getHttpBaseUrl()
        ));
        command.run(arguments);

        assertThat(out.getOutput())
                .contains("DevFlags - DEVELOPER OPTIONS ENABLED!")
                .contains("DevFlags - If you see this line, proceed at your own risk")
                .endsWith("Token updated in launcher database\n");

        testDb.assertThatToken().isEqualTo(uuid);
    }

    @Test
    void shouldLoginAndReturnTokenOnly(SystemOutCaptureExtension out,
                                       WireMockRuntimeInfo wiremock,
                                       @TempDir Path tempDir) {
        String uuid = UUID.randomUUID().toString();
        stubAccountLogin(uuid);

        final TestDatabase testDb = withSettings(tempDir);
        final Command command = new LoginCommand(() -> testDb.path());

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);

        Arguments arguments = new Arguments(Map.of(
                "dev-options", "true",
                "api-endpoint", wiremock.getHttpBaseUrl(),
                "username", "username",
                "password", "password",
                "ignore-update", "true"
        ));
        command.run(arguments);

        assertThat(out.getOutput())
                .contains("DevFlags - DEVELOPER OPTIONS ENABLED!")
                .contains("DevFlags - If you see this line, proceed at your own risk")
                .endsWith("Skipping token update in launcher database\n");

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);
    }

    @Test
    void shouldPanicWhenUsernameIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSettings(tempDir);
        final Command command = new LoginCommand(() -> testDb.path());

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);

        Arguments arguments = new Arguments(Map.of("password", "password"));
        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--username'");

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);
    }

    @Test
    void shouldPanicWhenPasswordIsNotSet(SystemOutCaptureExtension out, @TempDir Path tempDir) {
        final TestDatabase testDb = withSettings(tempDir);
        final Command command = new LoginCommand(() -> testDb.path());

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);

        Arguments arguments = new Arguments(Map.of("username", "username"));
        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
                .isInstanceOf(PanicException.class)
                .hasMessage("Missing or empty parameter: '--password'");

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);
    }
}
