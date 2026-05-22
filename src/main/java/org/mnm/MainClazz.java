package org.mnm;

import java.util.function.Function;
import java.util.function.IntConsumer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.cli.CommandParser;
import org.mnm.tools.PanicException;

public class MainClazz {

    static void main(String[] args) {
        main(args, Arguments::parse, CommandParser::parse, System::exit);
    }

    static void main(String[] args,
                     Function<String[], Arguments> argumentsParser,
                     Function<String[], Command> commandParser,
                     IntConsumer exit) {
        Command command = commandParser.apply(args);
        if (command == null) {
            System.out.printf("Unrecognized command: '%s'%n", args[0]);
            return;
        }
        try {
            Arguments arguments = argumentsParser.apply(args);

            if (arguments.getBoolean("debug")) {
                enableDebug();
            }

            if (arguments.isHelp() && command.help() != null) {
                System.out.println(command.help());
                return;
            }
            if (command.isAvailable()) {
                command.run(arguments);
            } else {
                System.err.println("Command '%s' not supported for your platform".formatted(command.name()));
            }
        } catch (PanicException e) {
            System.err.println("[Error] " + e.getMessage());
            exit.accept(1);
        } catch (Exception e) {
            System.err.println("Unexpected error:");
            e.printStackTrace();
            exit.accept(1);
        }
    }

    private static void enableDebug() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        final Logger myLogger = context.getLogger("org.mnm");
        myLogger.setLevel(Level.DEBUG);
    }
}
