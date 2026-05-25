package org.mnm.client;

import org.mnm.config.ConfigDb;

import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.config.OS.getWorkingDirectory;

public class Factories {

    static void installer(InstallerOptions options, ConfigDb configDb) {
        ClientInstaller client = new ClientInstaller(configDb);
        client.install(options, getWorkingDirectory(), API_BASE_URL);
    }
}
