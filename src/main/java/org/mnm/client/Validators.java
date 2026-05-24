package org.mnm.client;

import org.mnm.api.Session;
import org.mnm.config.Client;
import org.mnm.config.Environment;

import static org.mnm.tools.ProcessUtils.panic;

public class Validators {

    public static void checkVersion(String token, Client client) {
        String sessionVersion = Session.login(token, Environment.API_BASE_URL).getVersion();
        if (!sessionVersion.equals(client.version())) {
            panic("Version mismatch: run 'repair' to update the client");
        }
    }
}
