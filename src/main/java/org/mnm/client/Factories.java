package org.mnm.client;

import org.mnm.config.Client;
import org.mnm.config.ConfigDb;

import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.config.Environment.getWorkDir;

public class Factories {

    static void installer(InstallerOptions options, ConfigDb configDb, Client.Status status) {
        new ClientInstaller(configDb)
            .install(options, getWorkDir(), API_BASE_URL, status);
    }

    static void runner(RunnerOptions options, ConfigDb configDb) {
        new ClientRunner(configDb)
            .run(options);
    }
}
