package org.mnm.cli;

import org.mnm.tools.StringUtils;

public class UnknownCommand implements Command {

    private final String command;
    private final Command delegate;

    public UnknownCommand(String command, Command delegate) {
        this.command = command;
        this.delegate = delegate;
    }

    @Override
    public void run(Arguments args) {
        if (StringUtils.isEmpty(command)) {
            System.out.printf("Command not set%n%n");
        } else {
            System.out.printf("Unrecognized command: '%s'%n%n", command);
        }
        if (delegate != null) {
            delegate.run(args);
        }
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public String help() {
        return null;
    }

}
