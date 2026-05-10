package org.mnm.cli;

import org.mnm.client.InstallCommand;
import org.mnm.client.RepairCommand;
import org.mnm.config.Environment;
import org.mnm.launcher.LoginCommand;
import org.mnm.launcher.LogoutCommand;
import org.mnm.launcher.TokenCommand;
import org.mnm.launcher.TokenInfoCommand;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public class CommandParser {

    private CommandParser() {
    }

    public static Command parse(String[] args) {
        final Supplier<Path> databasePathSupplier = () -> Environment.launcherDb;
        final var commands = List.of(
                new InstallCommand(),
                new RepairCommand(),

                new LoginCommand(databasePathSupplier),
                new LogoutCommand(databasePathSupplier),
                new TokenCommand(databasePathSupplier),
                new TokenInfoCommand(databasePathSupplier)
        );
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
