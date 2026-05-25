package org.mnm.client;

import org.mnm.config.ConfigDb;

import java.nio.file.Path;

import static org.mnm.config.Environment.API_BASE_URL;

public class Factories {

    static void installer(InstallerOptions options, ConfigDb configDb) {
        ClientInstaller client = new ClientInstaller(configDb);
        client.install(options, Path.of(System.getProperty("user.dir")), API_BASE_URL);
    }
}
