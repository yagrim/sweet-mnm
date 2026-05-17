package org.mnm.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.TestUtils;

import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
class VersionCommandTest {

    @Test
    void shouldReturnName() {
        final Command command = new VersionCommand();
        assertThat(command.name()).isEqualTo("version");
    }

    @Test
    void shouldReturnDescription() {
        final Command command = new VersionCommand();
        assertThat(command.description()).isEqualTo("Shows the version");
    }

    @Test
    void shouldReturnHelp() {
        final Command command = new VersionCommand();

        assertThat(command.help()).isEqualTo("""
                Shows the version
                
                Usage:
                  sweet version
                
                Options:
                  --help   Shows this help
                """);
    }

    @Test
    void shouldRun(SystemOutCaptureExtension out) throws IOException {
        final Command command = new VersionCommand();

        command.run(null);

        assertThat(out.getOutput())
                .startsWith("Version: %s".formatted(readVersion()));
    }

    private static String readVersion() throws IOException {
        return Files.readString(TestUtils.classpathFile("version.txt"));
    }

}
