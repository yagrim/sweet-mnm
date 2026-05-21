package org.mnm.cli;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mnm.SystemOutCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
class UnknownCommandTest {

    @Test
    void shouldReturnNullDescriptors() {
        Command command = new UnknownCommand(null, null);

        assertThat(command.name()).isNull();
        assertThat(command.description()).isNull();
        assertThat(command.help()).isNull();
    }

    @Test
    void shouldPrintOriginalCommand(SystemOutCaptureExtension out) {
        Command command = new UnknownCommand("hello", null);

        command.run(new Arguments(Map.of()));

        assertThat(out.getOutput()).isEqualTo("""
            Unrecognized command: 'hello'
            
            """);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldPrintEmptyWhenOriginalCommandIsEmpty(String input, SystemOutCaptureExtension out) {
        Command command = new UnknownCommand(input, null);

        command.run(new Arguments(Map.of()));

        assertThat(out.getOutput()).isEqualTo("""
            Command not set
            
            """);
    }

    @Test
    void shouldInvokeDelegate(SystemOutCaptureExtension out) {
        Command command = new UnknownCommand(null, new TestCommand());

        command.run(null);

        assertThat(out.getOutput()).isEqualTo("""
            Command not set
            
            Hello from the delegate!
            """);
    }

    private class TestCommand implements Command {

        @Override
        public void run(Arguments args) {
            System.out.println("Hello from the delegate!");
        }

        @Override
        public String name() {
            return "";
        }

        @Override
        public String description() {
            return "";
        }

        @Override
        public String help() {
            return "";
        }
    }
}
