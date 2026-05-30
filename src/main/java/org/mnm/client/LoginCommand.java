package org.mnm.client;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;
import org.mnm.config.Environment;

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
            String slug = new LoginService(configDb)
                .login(credentials.username(), credentials.password(), getWorkDir(), Environment.API_BASE_URL);

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
