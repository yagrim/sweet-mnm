package org.mnm.cli;

public class RepairCommand implements Command {

    @Override
    public void run() {
        System.out.println("Repair Command");
    }

    @Override
    public String name() {
        return "repair";
    }

    @Override
    public String help() {
        return "Checks installation and updates if necessary";
    }
}
