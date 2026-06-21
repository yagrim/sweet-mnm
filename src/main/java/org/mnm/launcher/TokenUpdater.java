package org.mnm.launcher;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.mnm.api.ApiConnection;
import org.mnm.api.ApiConnector;
import org.mnm.api.RestClient;

class TokenUpdater {

    private final Supplier<Path> databaseFileLocator;

    TokenUpdater(Supplier<Path> databaseFileLocator) {
        this.databaseFileLocator = databaseFileLocator;
    }

    void update(String apiEndpoint, Options options) {

        ApiConnector apiConnector = new ApiConnector(new RestClient(apiEndpoint));
        ApiConnection apiConnection = apiConnector.login(options.username(), options.password());

        final String newToken = apiConnection.getToken();
        if (!options.ignoreUpdate()) {
            try (LauncherDb launcherDb = new LauncherDb(databaseFileLocator.get())) {

                launcherDb.updateSetting("token", newToken);
                System.out.println("Token updated in launcher database");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Skipping token update in launcher database");
        }
    }

    record Options(String username, String password, boolean ignoreUpdate) {
    }

}
