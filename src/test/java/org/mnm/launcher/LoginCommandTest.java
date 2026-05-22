package org.mnm.launcher;

import java.nio.file.Path;
import java.util.UUID;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.LauncherTestDatabase.TestDatabase;
import org.mnm.LinuxOnlyCommand;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mnm.ApiServerStubs.stubAccountLogin;
import static org.mnm.LauncherTestDatabase.INITIAL_TOKEN;
import static org.mnm.LauncherTestDatabase.withSettings;

@ExtendWith(SystemOutCaptureExtension.class)
@WireMockTest(httpsEnabled = true)
class LoginCommandTest extends LinuxOnlyCommand {

    @Test
    void shouldReturnHelp() {
        final Command command = new LoginCommand(null);

        assertThat(command.help()).isEqualTo("""
            Login with your username and password (updates launcher database)
            
            Usage:
              sweet launcher-login --username <username> --password <password>
            
            Options:
              --username       MnM account username (required)
              --password       MnM account password (required)
              --ignore-update  Prints the token without updating the launcher database
              --debug          Enables debug messages
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

        Arguments arguments = Arguments.parse(
            "--username", "username",
            "--password", "password",
            "--dev-options", "true",
            "--api-endpoint", wiremock.getHttpBaseUrl());
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

        Arguments arguments = Arguments.parse(
            "--dev-options", "true",
            "--api-endpoint", wiremock.getHttpBaseUrl(),
            "--username", "username",
            "--password", "password",
            "--ignore-update", "true");
        command.run(arguments);

        assertThat(out.getOutput())
            .contains("DevFlags - DEVELOPER OPTIONS ENABLED!")
            .contains("DevFlags - If you see this line, proceed at your own risk")
            .endsWith("Skipping token update in launcher database\n");

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);
    }

    @Test
    void shouldPanicWhenUsernameIsNotSet(@TempDir Path tempDir) {
        final TestDatabase testDb = withSettings(tempDir);
        final Command command = new LoginCommand(() -> testDb.path());

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);

        Arguments arguments = Arguments.parse("--password", "password");
        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--username'");

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);
    }

    @Test
    void shouldPanicWhenPasswordIsNotSet(@TempDir Path tempDir) {
        final TestDatabase testDb = withSettings(tempDir);
        final Command command = new LoginCommand(() -> testDb.path());

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);

        Arguments arguments = Arguments.parse("--username", "username");
        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--password'");

        testDb.assertThatToken().isEqualTo(INITIAL_TOKEN);
    }

    @Override
    protected Command buildCommand() {
        return new LoginCommand(null);
    }

}
