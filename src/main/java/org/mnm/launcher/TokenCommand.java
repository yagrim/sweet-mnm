package org.mnm.launcher;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.OS;

import static org.mnm.tools.StringUtils.isEmpty;

public class TokenCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public TokenCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        final TokenService tokenService = new TokenService();
        final String token = tokenService.retrieveToken(databaseFileLocator.get());

        if (isEmpty(token)) {
            System.out.println("No token found");
        } else {
            System.out.println(token);
        }
    }

    @Override
    public String name() {
        return "token";
    }

    @Override
    public String description() {
        return "Shows official launcher current token";
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
