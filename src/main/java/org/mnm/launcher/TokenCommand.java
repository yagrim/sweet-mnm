package org.mnm.launcher;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.Environment;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

public class TokenCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public TokenCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        try (LauncherDb launcherDb = new LauncherDb(databaseFileLocator.get())) {
            System.out.println(launcherDb.getSettings().get("token"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Path getDatabaseLocation() {
        return Environment.launcherDb;
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
