package org.mnm.ui;

public class CommandParser {

    /**
     * Returns Help if no command is recognized.
     */
    public static Command parse(String[] args) {

        if (args == null || args.length == 0) {
            return new HelpCommand();
        }

        return switch (args[0]) {
            case "install" -> new InstallCommand();
            case "repair" -> new RepairCommand();
            default -> new HelpCommand();
        };
    }

}
