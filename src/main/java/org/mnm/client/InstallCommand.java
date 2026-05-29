package org.mnm.client;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.client.ClientInstaller.Installer;
import org.mnm.config.ConfigDb;

import static org.mnm.config.Client.Status.INSTALLING;

public class InstallCommand implements Command {

    private final Supplier<Path> configFileLocator;
    private final Installer installer;

    public InstallCommand(Supplier<Path> configFileLocator) {
        this(configFileLocator, Factories::installer);
    }

    InstallCommand(Supplier<Path> configFileLocator, Installer installer) {
        this.configFileLocator = configFileLocator;
        this.installer = installer;
    }

    @Override
    public void run(Arguments args) {
        InstallerOptions options = InstallerOptions.parse(args);
        options.validateInstall();

        try (ConfigDb configDb = ConfigDb.open(configFileLocator.get())) {
            installer.install(options, configDb, INSTALLING);
        }

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
        return """
            %s
            
            Usage:
              sweet %s --username <username> --password <password>
            
            Options:
              --username      MnM account username (required when --slug is not set)
              --password      MnM account password (required when --username is set)
              --file-check    Check files using external process or in-memory method (in-memory, xxhsum (default))
              --debug         Enables debug messages
              --help          Shows this help
            """.formatted(description(), name());
    }

}
