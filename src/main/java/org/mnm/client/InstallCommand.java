package org.mnm.client;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;

import static org.mnm.config.Environment.API_BASE_URL;

public class InstallCommand implements Command {

    private final Supplier<Path> configFileLocator;

    public InstallCommand(Supplier<Path> configDbSupplier) {
        this.configFileLocator = configDbSupplier;
    }

    @Override
    public void run(Arguments args) {
        InstallOptions options = InstallOptions.parse(args);
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
        return """
            %s
            
            Usage:
              sweet %2$s --username <username> --password <password>
              sweet %2$s --slug <slug>
            
            Options:
              --username      MnM account username (required when --slug is not set)
              --password      MnM account password (required when --username is set)
              --slug          Existing configured client slug, can be used instead of credentials
              --file-check    Check files using external process or in-memory method (in-memory, xxhsum (default))
              --debug         Enables debug messages
              --help          Shows this help
            """.formatted(description(), name());
    }

    private static Path getWorkingDirectory() {
        return Path.of(System.getProperty("user.dir"));
    }

}
