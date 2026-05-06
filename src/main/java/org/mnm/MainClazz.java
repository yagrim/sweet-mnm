package org.mnm;

import org.mnm.cli.ArgumentsParser;
import org.mnm.cli.Command;
import org.mnm.cli.CommandParser;

public class MainClazz {

    static void main(String[] args) {
        Command command = CommandParser.parse(args);
        if (command == null) {
            System.out.printf("Unrecognized command: '%s'%n", args[0]);
            return;
        }
        command.run(ArgumentsParser.parse(args));
    }
}
