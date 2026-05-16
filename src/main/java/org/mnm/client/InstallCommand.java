package org.mnm.client;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.config.Environment.API_BASE_URL;

public class InstallCommand implements Command {

    private final Supplier<Path> configFileLocator;

    public InstallCommand(Supplier<Path> configDbSupplier) {
        this.configFileLocator = configDbSupplier;
    }

    @Override
    public void run(Arguments args) {

        InstallOptions options = new InstallOptions(
                args.get("username"),
                args.get("password"),
                args.getOrDefault("slug", null)
        );
        options.validate();

        try (ConfigDb configDb = ConfigDb.open(configFileLocator.get())) {
            configDb.initialize();

            ClientInstaller client = new ClientInstaller(configDb);
            client.install(options, getWorkingDirectory(), API_BASE_URL);
        }

        shutdownHook();
    }

    protected void shutdownHook() {
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

    private static Path getWorkingDirectory() {
        return Path.of(System.getProperty("user.dir"));
    }

}
