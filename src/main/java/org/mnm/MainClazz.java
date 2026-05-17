package org.mnm;

import org.mnm.cli.ArgumentsParser;
import org.mnm.cli.Command;
import org.mnm.cli.CommandParser;
import org.mnm.tools.PanicException;

public class MainClazz {

    static void main(String[] args) {
        Command command = CommandParser.parse(args);
        if (command == null) {
            System.out.printf("Unrecognized command: '%s'%n", args[0]);
            return;
        }
        try {
            command.run(ArgumentsParser.parse(args));
        } catch (PanicException e) {
            System.err.println("[Error] " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            // TODO test
            System.err.println("Unexpected error:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
