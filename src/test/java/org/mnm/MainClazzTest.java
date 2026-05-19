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
    void shouldPrintCommandHelpAndSkipRunWhenHelpFlagIsSet(SystemOutCaptureExtension out) {
        AtomicInteger runCount = new AtomicInteger();
        Command command = new TestCommand(runCount, true);

        MainClazz.main(
                new String[]{"test", "--help"},
                ArgumentsParser::parse,
                _ -> command,
                _ -> {
                });

        assertThat(runCount).hasValue(0);
        assertThat(out.getOutput()).isEqualTo("""
                Test command help
                """);
    }

    @Test
    void shouldPrintUnexpectedErrorAndExitWhenCommandFails(SystemOutCaptureExtension out) {
        Command failingCommand = new FailingCommand(new IllegalStateException("broken command"));
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
        Command failingCommand = new FailingCommand(new PanicException("broken command"));
        AtomicInteger exitStatus = new AtomicInteger();

        MainClazz.main(
                new String[]{"broken"},
                ArgumentsParser::parse,
                _ -> failingCommand,
                exitStatus::set);

        assertThat(exitStatus).hasValue(1);
        String errorOutput = out.getErrorOutput();
        assertThat(errorOutput).hasSize(23);
        assertThat(errorOutput).isEqualTo("""
                [Error] broken command
                """);
    }

    @Test
    void shouldRunWhenCommandIsAvailable(SystemOutCaptureExtension out) {
        AtomicInteger runCount = new AtomicInteger();
        Command command = new TestCommand(runCount, true);

        MainClazz.main(
                new String[]{"test"},
                ArgumentsParser::parse,
                _ -> command,
                _ -> {
                });

        assertThat(runCount).hasValue(1);
        assertThat(out.getOutput()).isEmpty();
        assertThat(out.getErrorOutput()).isEmpty();
    }

    @Test
    void shouldRunWhenCommandIsNotAvailable(SystemOutCaptureExtension out) {
        AtomicInteger runCount = new AtomicInteger();
        Command command = new TestCommand(runCount, false);

        MainClazz.main(
                new String[]{"test"},
                ArgumentsParser::parse,
                _ -> command,
                _ -> {
                });

        assertThat(runCount).hasValue(0);
        assertThat(out.getOutput()).isEmpty();
        assertThat(out.getErrorOutput())
                .startsWith("Command 'test' not supported for your platform");
    }

    private record TestCommand(AtomicInteger runCount, boolean isAvailable) implements Command {

        @Override
        public void run(Arguments args) {
            runCount.incrementAndGet();
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public String description() {
            return "Test command";
        }

        @Override
        public String help() {
            return "Test command help";
        }

        @Override
        public boolean isAvailable() {
            return isAvailable;
        }
    }

    private record FailingCommand(RuntimeException exception) implements Command {

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
                  clients      Lists configured clients
                  install      Installs MnM client in the current location
                  login        Login with your username and password (updates launcher database)
                  logout       Removes token from the launcher database
                  repair       Checks installation and updates if necessary
                  token        Shows official launcher current token
                  token-info   Shows official launcher token information
                  version      Shows the version
                
                Options:
                  --help   Shows this help
                """;
    }

}
