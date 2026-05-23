package org.mnm.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.mnm.client.ClientsCommand;
import org.mnm.client.InstallCommand;
import org.mnm.client.RepairCommand;
import org.mnm.client.TokenCommand;
import org.mnm.client.TokensCommand;
import org.mnm.config.ConfigDbLocator;
import org.mnm.config.Environment;
import org.mnm.launcher.LauncherTokenCommand;
import org.mnm.launcher.LoginCommand;
import org.mnm.launcher.LogoutCommand;

public class CommandParser {

    private CommandParser() {
    }

    public static Command parse(String[] args) {
        final var commands = buildCommands();
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

    private static List<Command> buildCommands() {
        final Supplier<Path> launcherDbSupplier = () -> Environment.launcherDb;
        final Supplier<Path> configDbSupplier = new ConfigDbLocator();

        final var commands = List.of(
            new ClientsCommand(configDbSupplier),
            new InstallCommand(configDbSupplier),
            new RepairCommand(configDbSupplier),
            new TokenCommand(configDbSupplier),
            new TokensCommand(configDbSupplier),

            new LoginCommand(launcherDbSupplier),
            new LogoutCommand(launcherDbSupplier),
            new LauncherTokenCommand(launcherDbSupplier),

            new VersionCommand()
        );
        return commands;
    }

}
