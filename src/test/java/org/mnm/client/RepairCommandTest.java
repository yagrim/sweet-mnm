package org.mnm.client;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mnm.SystemOutCaptureExtension;
import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.PanicException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(SystemOutCaptureExtension.class)
class RepairCommandTest {

    @Test
    void shouldReturnName() {
        final Command command = new RepairCommand(null);

        assertThat(command.name()).isEqualTo("repair");
    }

    @Test
    void shouldReturnDescription() {
        final Command command = new RepairCommand(null);

        assertThat(command.description()).isEqualTo("Checks installation and updates if necessary");
    }

    @Test
    void shouldReturnHelp() {
        final Command command = new RepairCommand(null);

        assertThat(command.help()).isEqualTo("""
            Checks installation and updates if necessary
            
            Usage:
              sweet repair --slug <slug>
            
            Options:
              --slug          Existing configured client slug, can be used instead of credentials
              --file-check    Check files using external process or in-memory method (in-memory, xxhsum (default))
              --debug         Enables debug messages
              --help          Shows this help
            """);
    }

    @Test
    void shouldValidateOptionsBeforeOpeningConfigDatabase() {
        AtomicBoolean configFileLocated = new AtomicBoolean(false);
        Command command = new RepairCommand(() -> {
            configFileLocated.set(true);
            return null;
        });
        Arguments arguments = Arguments.parse("--file-check", "xxhsum");

        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
            .isInstanceOf(PanicException.class)
            .hasMessage("Missing or empty parameter: '--slug'");
        assertThat(configFileLocated).isFalse();
    }

    @Test
    void shouldValidateInvalidFileCheckBeforeOpeningConfigDatabase() {
        AtomicBoolean configFileLocated = new AtomicBoolean(false);
        Command command = new RepairCommand(() -> {
            configFileLocated.set(true);
            return null;
        });
        Arguments arguments = Arguments.parse("--slug", "mnm", "--file-check", "invalid");

        Throwable t = catchThrowable(() -> command.run(arguments));

        assertThat(t)
            .isInstanceOf(PanicException.class)
            .hasMessage("Invalid File Check value, use 'in-memory' or 'xxhsum'");
        assertThat(configFileLocated).isFalse();
    }

}
