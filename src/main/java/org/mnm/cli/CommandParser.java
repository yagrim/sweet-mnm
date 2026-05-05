package org.mnm.cli;

import java.util.List;

public class CommandParser {

    private CommandParser() {
    }

    /**
     * Returns Help if no command is recognized.
     */
    public static Command parse(String[] args) {

        final var commands = List.of(new InstallCommand(), new RepairCommand());
        final Command helpCommand = new HelpCommand(commands);

        if (args == null || args.length == 0) {
            return helpCommand;
        }

        for (Command command : commands) {
            if (command.name().equals(args[0])) {
                return command;
            }
        }

        return helpCommand;
    }

}
