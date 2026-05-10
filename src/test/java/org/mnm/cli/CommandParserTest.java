package org.mnm.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mnm.client.InstallCommand;
import org.mnm.client.RepairCommand;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CommandParserTest {

    @ParameterizedTest
    @MethodSource("supportedCommands")
    void shouldParserSupportedCommands(String command, Class<? extends Command> expected) {
        String[] args = {command};

        assertThat(CommandParser.parse(args))
                .isInstanceOf(expected);
    }

    static Stream<Arguments> supportedCommands() {
        return Stream.of(
                Arguments.of("install", InstallCommand.class),
                Arguments.of("repair", RepairCommand.class),
                Arguments.of("help", HelpCommand.class)
        );
    }

    @ParameterizedTest
    @MethodSource("unknownArguments")
    void shouldReturnNullWhenEmptyInput(String[] args) {
        assertThat(CommandParser.parse(args))
                .isInstanceOf(UnknownCommand.class);
    }

    static Stream<Arguments> unknownArguments() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of((Object) new String[]{}),
                Arguments.of((Object) new String[]{""}),
                Arguments.of((Object) new String[]{"unknown-command"})
        );
    }

    @Test
    void shouldIgnoreFurtherArgumentsWithUnknownCommand() {
        String[] args = {"something", "else", "here"};

        assertThat(CommandParser.parse(args))
                .isInstanceOf(UnknownCommand.class);
    }

    @Test
    void shouldIgnoreFurtherArgumentsWithKnownCommand() {
        String[] args = {"help", "something", "else"};

        assertThat(CommandParser.parse(args))
                .isInstanceOf(HelpCommand.class);
    }

}
