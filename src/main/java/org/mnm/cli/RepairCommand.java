package org.mnm.cli;

public class RepairCommand implements Command {

    @Override
    public void run(Arguments args) {
        System.out.println("Repair Command");
    }

    @Override
    public String name() {
        return "repair";
    }

    @Override
    public String description() {
        return "Checks installation and updates if necessary";
    }

    @Override
    public String help() {
        return description();
    }
}
