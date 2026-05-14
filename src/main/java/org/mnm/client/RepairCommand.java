package org.mnm.client;

import java.nio.file.Path;
import java.util.function.Supplier;

public class RepairCommand extends InstallCommand {

    public RepairCommand(Supplier<Path> configDbSupplier) {
        super(configDbSupplier);
    }

    @Override
    protected void shutdownHook() {
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
