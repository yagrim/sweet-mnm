package org.mnm.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.client.InstallCommand;
import org.mnm.client.RepairCommand;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
class HelpCommandTest {

    @Test
    void shouldReturnHelpDescription() {
        HelpCommand command = new HelpCommand(emptyList());

        String help = command.help();

        assertThat(help).isEqualTo("Shows available commands");
    }

    @Test
    void shouldReturnListOfCommands(SystemOutCaptureExtension out) {
        HelpCommand command = new HelpCommand(List.of(new InstallCommand(null), new RepairCommand(null)));

        command.run(null);

        assertThat(out.getOutput()).isEqualTo("""
                Available commands:
                  install   Installs MnM client in the current location
                  repair    Checks installation and updates if necessary
                  help      Shows available commands
                """);
    }

}
