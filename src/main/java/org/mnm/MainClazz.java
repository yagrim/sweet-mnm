package org.mnm;

import org.mnm.ui.Command;
import org.mnm.ui.CommandParser;

public class MainClazz {

    static void main(String[] args) {

        Command command = CommandParser.parse(args);
        command.run();
    }
}
