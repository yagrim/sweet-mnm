package org.mnm.client;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;

public class RepairCommand implements Command {

    private final Supplier<Path> configFileLocator;
    private final BiConsumer<InstallerOptions, ConfigDb> installer;

    public RepairCommand(Supplier<Path> configFileLocator) {
        this(configFileLocator, Factories::installer);
    }

    RepairCommand(Supplier<Path> configFileLocator, BiConsumer<InstallerOptions, ConfigDb> installer) {
        this.configFileLocator = configFileLocator;
        this.installer = installer;
    }

    @Override
    public void run(Arguments args) {
        InstallerOptions options = InstallerOptions.parse(args);
        options.validateRepair();

        try (ConfigDb configDb = ConfigDb.open(configFileLocator.get())) {
            installer.accept(options, configDb);
        }

        System.out.println("Repair completed");
    }

    @Override
    public String name() {
        return "repair";
    }

    @Override
    public String description() {
        return "Checks an installation and updates if necessary";
    }

    @Override
    public String help() {
        return """
            %s
            
            Usage:
              sweet %s --slug <slug>
            
            Options:
              --slug          Existing configured client slug, can be used instead of credentials
              --file-check    Check files using external process or in-memory method (in-memory, xxhsum (default))
              --debug         Enables debug messages
              --help          Shows this help
            """.formatted(description(), name());
    }
}
