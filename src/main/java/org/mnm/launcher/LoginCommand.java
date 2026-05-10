package org.mnm.launcher;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.StringUtils;

import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.tools.ProcessUtils.panic;

public class LoginCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public LoginCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        final String username = args.get("username");
        if (StringUtils.isEmpty(username)) {
            panic("Missing parameter: '--username'");
        }
        final String password = args.get("password");
        if (StringUtils.isEmpty(password)) {
            panic("Missing parameter: '--password'");
        }

        TokenUpdater updater = new TokenUpdater(databaseFileLocator);
        updater.update(username, password, args);
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

}
