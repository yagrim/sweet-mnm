package org.mnm.launcher;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.OS;
import org.mnm.launcher.TokenUpdater.Options;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class LoginCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public LoginCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        final String username = args.get("username");
        if (isEmpty(username)) {
            panic("Missing or empty parameter: '--username'");
        }
        final String password = args.get("password");
        if (isEmpty(password)) {
            panic("Missing or empty parameter: '--password'");
        }

        final DevFlags devFlags = DevFlags.parse(args);
        final String apiEndpoint = devFlags.enabled() ? devFlags.apiEndpoint() : API_BASE_URL;
        new TokenUpdater(databaseFileLocator)
                .update(apiEndpoint,
                        new Options(username, password, args.getBoolean("ignore-update")));
    }

    @Override
    public String name() {
        return "login";
    }

    @Override
    public String description() {
        return "Login with your username and password (updates launcher database)";
    }

    @Override
    public String help() {
        return """
                %s
                
                Usage:
                  sweet %s --username <username> --password <password>
                
                Options:
                  --username       MnM account username (required)
                  --password       MnM account password (required)
                  --ignore-update  Prints the token without updating the launcher database
                  --help           Shows this help
                """.formatted(description(), name());
    }

    @Override
    public boolean isAvailable() {
        return !OS.isWindows();
    }

}
