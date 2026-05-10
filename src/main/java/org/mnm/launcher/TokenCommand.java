package org.mnm.launcher;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.StringUtils;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.function.Supplier;

import static org.mnm.tools.StringUtils.isEmpty;

public class TokenCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public TokenCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        try (LauncherDb launcherDb = new LauncherDb(databaseFileLocator.get())) {
            String token = launcherDb.getSettings().get("token");
            if (isEmpty(token)) {
                System.out.println("No token found");
            } else {
                System.out.println(token);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "token";
    }

    @Override
    public String description() {
        return "User token utilities";
    }

    @Override
    public String help() {
        return description();
    }

}
