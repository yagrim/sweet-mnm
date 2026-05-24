package org.mnm.cli;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnm.SystemOutCaptureExtension;
import org.mnm.client.ClientsCommand;
import org.mnm.client.InstallCommand;
import org.mnm.client.RepairCommand;
import org.mnm.client.RunCommand;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
class HelpCommandTest {

    @Test
    void shouldReturnHelpDescription() {
        HelpCommand command = new HelpCommand(emptyList());

        String help = command.help();

        assertThat(help).isEqualTo("""
            Shows available commands
            
            Usage:
              sweet help
            
            Options:
              --debug  Enables debug messages
              --help   Shows this help
            """);
    }

    @Test
    void shouldReturnListOfCommands(SystemOutCaptureExtension out) {
        HelpCommand command = new HelpCommand(List.of(new ClientsCommand(null), new InstallCommand(null), new RepairCommand(null), new RunCommand(null)));

        command.run(null);

        assertThat(out.getOutput()).isEqualTo("""
            (The unofficial and...) Sweet tool to manage Monsters & Memories clients
            
            Usage:
              sweet <command> [--option [value]] ...
            
            Available commands:
              clients   Lists configured clients
              install   Installs MnM client in the current location
              repair    Checks installation and updates if necessary
              run       Runs configured client
            
            Options:
              --debug  Enables debug messages
              --help   Shows this help
            """);
    }

}
