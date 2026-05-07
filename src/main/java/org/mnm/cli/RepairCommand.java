package org.mnm.cli;

import org.mnm.ClientInstaller;

public class RepairCommand implements Command {

    @Override
    public void run(Arguments args) {
        ClientInstaller client = new ClientInstaller();
        client.install(args.get("username"), args.get("password"));

        System.out.println("Repair completed");
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
