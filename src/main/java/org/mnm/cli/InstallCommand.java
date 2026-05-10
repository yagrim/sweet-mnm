package org.mnm.cli;

import org.mnm.ClientInstaller;

import static org.mnm.cli.Validators.validateArguments;

public class InstallCommand implements Command {

    @Override
    public void run(Arguments args) {
        validateArguments(args);

        ClientInstaller client = new ClientInstaller();
        client.install(args.get("username"), args.get("password"));

        System.out.println("Installation completed");
    }

    @Override
    public String name() {
        return "install";
    }

    @Override
    public String description() {
        return "Installs MnM client in the current location";
    }

    @Override
    public String help() {
        return description();
    }

}
