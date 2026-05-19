package org.mnm.launcher;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.OS;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.function.Supplier;

public class LogoutCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public LogoutCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        try (LauncherDb launcherDb = new LauncherDb(databaseFileLocator.get())) {
            launcherDb.updateSetting("token", "");
            System.out.println("Token removed from launcher database");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "logout";
    }

    @Override
    public String description() {
        return "Removes token from the launcher database";
    }

    @Override
    public String help() {
        return """
                %s
                
                Usage:
                  sweet %s
                
                Options:
                  --help   Shows this help
                """.formatted(description(), name());
    }

    @Override
    public boolean isAvailable() {
        return !OS.isWindows();
    }

}
