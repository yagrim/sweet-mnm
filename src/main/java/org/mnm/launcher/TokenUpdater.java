package org.mnm.launcher;

import org.mnm.api.ApiConnection;
import org.mnm.api.ApiConnector;
import org.mnm.api.RestClient;
import org.mnm.cli.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.config.Environment.API_BASE_URL;

public class TokenUpdater {

    private static final Logger logger = LoggerFactory.getLogger(TokenUpdater.class);

    private final Supplier<Path> databaseFileLocator;

    public TokenUpdater(Supplier<Path> databaseFileLocator) {
        this.databaseFileLocator = databaseFileLocator;
    }

    public void update(String username, String password, Arguments arguments) {

        String apiEndpoint = API_BASE_URL;
        if (arguments.getBoolean("dev-options")) {
            apiEndpoint = arguments.get("api-endpoint");
            logger.info("DEVELOPER OPTIONS ENABLED!");
            logger.info("If you see this line, proceed at your own risk");
        }

        ApiConnector apiConnector = new ApiConnector(new RestClient(apiEndpoint));
        ApiConnection apiConnection = apiConnector.login(username, password);

        try (LauncherDb launcherDb = new LauncherDb(databaseFileLocator.get())) {
            launcherDb.updateSetting("token", apiConnection.getToken());
            System.out.println("Token updated in launcher database");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
