package org.mnm.client;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.ApiConnector;
import org.mnm.api.RestClient;
import org.mnm.api.TokenSupplier;
import org.mnm.cli.Arguments;
import org.mnm.cli.CliTwoFactorTokenSupplier;
import org.mnm.cli.Command;
import org.mnm.cli.LineReader;
import org.mnm.config.ConfigDb;

import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.config.Environment.getWorkDir;

public class LoginCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class);

    private final Supplier<Path> databaseFileLocator;

    public LoginCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {

        final Credentials credentials = Credentials.parse(args);
        credentials.validate();

        try (ConfigDb configDb = ConfigDb.open(databaseFileLocator.get())) {
            ApiConnector apiConnector = new ApiConnector(new RestClient(API_BASE_URL));
            TokenSupplier tokenSupplier = new CliTwoFactorTokenSupplier(apiConnector, new LineReader());

            String slug = new LoginService(configDb, apiConnector)
                .login(credentials.username(), credentials.password(), getWorkDir(), tokenSupplier);

            logger.info("Stored token for slug '{}'", slug);
        }
    }

    @Override
    public String name() {
        return "login";
    }

    @Override
    public String description() {
        return "Logins with credentials to generate and store a token";
    }

    @Override
    public String help() {
        return """
            %s
            
            Usage:
              sweet %s --username <username> --password <password>
            
            Options:
              --username    MnM account username (required when --slug is not set)
              --password    MnM account password (required when --username is set)
              --debug       Enables debug messages
              --help        Shows this help
            """.formatted(description(), name());
    }

}
