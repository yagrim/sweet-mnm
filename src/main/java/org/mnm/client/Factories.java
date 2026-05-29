package org.mnm.client;

import java.nio.file.Path;

import org.mnm.config.Client;
import org.mnm.config.ConfigDb;

import static org.mnm.config.Environment.API_BASE_URL;

public class Factories {

    static void installer(InstallerOptions options, ConfigDb configDb, Client.Status status) {
        new ClientInstaller(configDb)
            .install(options, Path.of(System.getProperty("user.dir")), API_BASE_URL, status);
    }

    static void runner(RunnerOptions options, ConfigDb configDb) {
        new ClientRunner(configDb)
            .run(options);
    }
}
