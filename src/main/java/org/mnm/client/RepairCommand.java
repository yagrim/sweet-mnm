package org.mnm.client;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;

import static org.mnm.client.Validators.validateArguments;

public class RepairCommand implements Command {

    @Override
    public void run(Arguments args) {
        validateArguments(args);

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
