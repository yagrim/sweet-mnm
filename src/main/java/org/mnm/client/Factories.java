package org.mnm.client;

import org.mnm.api.ApiConnector;
import org.mnm.api.RestClient;
import org.mnm.api.TokenSupplier;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;

import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.config.Environment.getWorkDir;

public class Factories {

    static void installer(InstallerOptions options, ConfigDb configDb, Client.Status status) {
        ApiConnector apiConnector = new ApiConnector(new RestClient(API_BASE_URL));
        TokenSupplier tokenSupplier = new CliTwoFactorTokenSupplier(apiConnector);
        new ClientInstaller(configDb, apiConnector)
            .install(options, getWorkDir(), API_BASE_URL, status, tokenSupplier);
    }

    static void runner(RunnerOptions options, ConfigDb configDb) {
        new ClientRunner(configDb)
            .run(options);
    }
}
