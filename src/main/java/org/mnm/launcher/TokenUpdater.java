package org.mnm.launcher;

import org.mnm.api.ApiConnection;
import org.mnm.api.ApiConnector;
import org.mnm.api.RestClient;
import org.mnm.launcher.LoginCommand.DevFlags;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.config.Environment.API_BASE_URL;

public class TokenUpdater {

    private final Supplier<Path> databaseFileLocator;

    public TokenUpdater(Supplier<Path> databaseFileLocator) {
        this.databaseFileLocator = databaseFileLocator;
    }

    public void update(String username, String password, LoginCommand.Options options) {

        final DevFlags devFlags = options.devFlags();
        final String apiEndpoint = devFlags.enabled() ? devFlags.apiEndpoint() : API_BASE_URL;

        ApiConnector apiConnector = new ApiConnector(new RestClient(apiEndpoint));
        ApiConnection apiConnection = apiConnector.login(username, password);

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

}
