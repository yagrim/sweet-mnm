package org.mnm.cli;

public interface Command {

    void run(Arguments args);

    String name();

    String description();

    String help();

}
