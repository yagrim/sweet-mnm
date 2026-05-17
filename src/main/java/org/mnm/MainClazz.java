package org.mnm;

import org.mnm.cli.Arguments;
import org.mnm.cli.ArgumentsParser;
import org.mnm.cli.Command;
import org.mnm.cli.CommandParser;
import org.mnm.tools.PanicException;

import java.util.function.Function;
import java.util.function.IntConsumer;

public class MainClazz {

    static void main(String[] args) {
        main(args, ArgumentsParser::parse, CommandParser::parse, System::exit);
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
            command.run(argumentsParser.apply(args));
        } catch (PanicException e) {
            System.err.println("[Error] " + e.getMessage());
            exit.accept(1);
        } catch (Exception e) {
            System.err.println("Unexpected error:");
            e.printStackTrace();
            exit.accept(1);
        }
    }
}
