package org.mnm.cli;

public interface Command {

    void run();

    String name();

    String help();
}
