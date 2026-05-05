package org.mnm.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
                Arguments.of("repair", RepairCommand.class)
        );
    }

    @ParameterizedTest
    @MethodSource("helpArguments")
    void shouldReturnHelpWhenEmptyInput(String[] args) {
        assertThat(CommandParser.parse(args))
                .isInstanceOf(HelpCommand.class);
    }

    static Stream<Arguments> helpArguments() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of((Object) new String[]{}),
                Arguments.of((Object) new String[]{""}),
                Arguments.of((Object) new String[]{"unknown-command"})
        );
    }

    @Test
    void shouldIgnoreFurtherArguments() {
        String[] args = {"something", "else", "here"};

        assertThat(CommandParser.parse(args))
                .isInstanceOf(HelpCommand.class);
    }

    @Test
    void shouldParseFirstArguments() {
        String[] args = {"something", "install", "here"};

        assertThat(CommandParser.parse(args))
                .isInstanceOf(HelpCommand.class);
    }

}
