package org.mnm.cli;

import java.util.List;

public class CommandParser {

    private CommandParser() {
    }

    public static Command parse(String[] args) {
        final var commands = List.of(new InstallCommand(), new RepairCommand());
        final Command help = new HelpCommand(commands);

        if (args == null || args.length == 0) {
            return new UnknownCommand(null, help);
        }

        for (Command command : commands) {
            if (command.name().equals(args[0])) {
                return command;
            }
        }

        if (help.name().equals(args[0])) {
            return help;
        }

        return new UnknownCommand(args[0], help);
    }

}
