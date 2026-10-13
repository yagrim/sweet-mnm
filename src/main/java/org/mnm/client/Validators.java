package org.mnm.client;

import org.mnm.api.ApiConnector;
import org.mnm.api.Session;
import org.mnm.config.Client;

import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.tools.ProcessUtils.panic;

public class Validators {

    public static void checkVersion(String token, Client client) {
        ApiConnector apiConnector = Factories.apiConnector(API_BASE_URL);
        String sessionVersion = Session.login(token, apiConnector).getVersion();
        if (!sessionVersion.equals(client.version())) {
            panic("Version mismatch: run 'repair' to update the client");
        }
    }
}
