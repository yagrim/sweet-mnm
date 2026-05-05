package org.mnm;

import org.mnm.cli.Command;
import org.mnm.cli.CommandParser;

public class MainClazz {

    static void main(String[] args) {

        Command command = CommandParser.parse(args);
        command.run();
    }
}
