package org.mnm.launcher;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class LoginCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(TokenUpdater.class);

    private final Supplier<Path> databaseFileLocator;

    public LoginCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        final String username = args.get("username");
        if (isEmpty(username)) {
            panic("Missing parameter: '--username'");
        }
        final String password = args.get("password");
        if (isEmpty(password)) {
            panic("Missing parameter: '--password'");
        }

        Options options = new Options(args.getBoolean("ignore-update"), processDevFlags(args));


        TokenUpdater updater = new TokenUpdater(databaseFileLocator);
        updater.update(username, password, options);
    }

    private static DevFlags processDevFlags(Arguments args) {
        final String apiEndpoint = args.get("api-endpoint");
        if (args.getBoolean("dev-options") && !isEmpty(apiEndpoint)) {
            logger.info("DEVELOPER OPTIONS ENABLED!");
            logger.info("If you see this line, proceed at your own risk");
            return new DevFlags(true, apiEndpoint);
        }
        return new DevFlags(false, null);
    }

    @Override
    public String name() {
        return "login";
    }

    @Override
    public String description() {
        return "Login with your username and password (can update launcher database)";
    }

    @Override
    public String help() {
        return description();
    }

    record Options(boolean ignoreUpdate, DevFlags devFlags) {
    }

    record DevFlags(boolean enabled, String apiEndpoint) {
    }
}
