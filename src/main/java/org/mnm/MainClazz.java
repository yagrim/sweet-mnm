package org.mnm;

import java.util.function.Function;
import java.util.function.IntConsumer;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.cli.CommandParser;
import org.mnm.config.OS;
import org.mnm.gui.GuiCommand;
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
                GeneralOptions.setDebug(true);
            }

            if (arguments.isHelp() && command.help() != null) {
                System.out.println(command.help());
                return;
            }
            if (command.isAvailable()) {
                if (!(command instanceof GuiCommand)) {
                    GeneralOptions.setInfo(true);
                }
                command.run(arguments);
            } else {
                System.err.printf("Command '%s' not supported for your platform%n", command.name());
            }
        } catch (Exception e) {
            handleError(e, exit);
        }
    }

    private static void handleError(Exception e, IntConsumer exit) {
        if (e instanceof PanicException) {
            System.err.println("[Error] " + e.getMessage());
        } else {
            System.err.println("[Error] Unexpected error!");
        }
        e.printStackTrace();
        // In Windows we hide the console at compile time and this would lock the program
        if (!OS.isWindows()) {
            pause();
        }
        exit.accept(1);
    }

    public static void pause() {
        System.out.print("Press Enter to close...");
        try {
            System.in.read();
        } catch (Exception e) {
            // ignore
        }
    }

}
