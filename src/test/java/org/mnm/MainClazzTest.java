package org.mnm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnm.cli.Arguments;
import org.mnm.cli.ArgumentsParser;
import org.mnm.cli.Command;
import org.mnm.tools.PanicException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
class MainClazzTest {

    @Test
    void shouldPrintMessageAndHelpWhenCommandIsUnknown(SystemOutCaptureExtension out) {
        MainClazz mainClazz = new MainClazz();
        mainClazz.main(new String[]{"unknown"});

        assertThat(out.getOutput())
                .isEqualTo("""
                        Unrecognized command: 'unknown'
                        
                        %s""".formatted(expectedHelp()));
    }

    @Test
    void shouldPrintHelp(SystemOutCaptureExtension out) {
        MainClazz mainClazz = new MainClazz();
        mainClazz.main(new String[]{"help"});

        assertThat(out.getOutput()).isEqualTo(expectedHelp());
    }

    @Test
    void shouldPrintUnexpectedErrorAndExitWhenCommandFails(SystemOutCaptureExtension out) {
        Command failingCommand = new TestCommand(new IllegalStateException("broken command"));
        AtomicInteger exitStatus = new AtomicInteger();

        MainClazz.main(
                new String[]{"broken"},
                ArgumentsParser::parse,
                _ -> failingCommand,
                exitStatus::set);

        assertThat(exitStatus).hasValue(1);
        assertThat(out.getErrorOutput())
                .startsWith("""
                        Unexpected error:
                        java.lang.IllegalStateException: broken command
                        """);
    }

    @Test
    void shouldPrintErrorAndExitWhenCommandPanics(SystemOutCaptureExtension out) {
        Command failingCommand = new TestCommand(new PanicException("broken command"));
        AtomicInteger exitStatus = new AtomicInteger();

        MainClazz.main(
                new String[]{"broken"},
                ArgumentsParser::parse,
                _ -> failingCommand,
                exitStatus::set);

        assertThat(exitStatus).hasValue(1);
        assertThat(out.getErrorOutput()).isEqualTo("""
                [Error] broken command
                """);
    }

    private record TestCommand(RuntimeException exception) implements Command {

        @Override
        public void run(Arguments args) {
            throw exception;
        }

        @Override
        public String name() {
            return "broken";
        }

        @Override
        public String description() {
            return "Broken command";
        }

        @Override
        public String help() {
            return description();
        }
    }

    private static String expectedHelp() {
        return """
                (The unofficial and...) Sweet tool to manage Monsters & Memories clients
                
                Usage:
                  sweet <command> [--option [value]] ...
                
                Available commands:
                  install      Installs MnM client in the current location
                  login        Login with your username and password (can update launcher database)
                  logout       Removes token from the launcher database
                  repair       Checks installation and updates if necessary
                  token        Shows official launcher current token
                  token-info   Shows official launcher token information
                  version      Shows the version
                  help         Shows available commands
                """;
    }

}
