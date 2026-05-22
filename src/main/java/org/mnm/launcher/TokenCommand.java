package org.mnm.launcher;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.OS;
import org.mnm.tools.ProcessUtils;

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
            final String output = args.get("output");
            if (isEmpty(output) || output.equals("raw")) {
                System.out.println(token);
            } else if (output.equals("rows")) {
                String formattedToken = tokenService.formatToColumns(token);
                System.out.printf(formattedToken);
            } else {
                ProcessUtils.panic("Invalid output: " + output);
            }
        }
    }

    @Override
    public String name() {
        return "launcher-token";
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
              --outout   Set output format: 'raw' for JWT token (default ), 'rows' for metadata
              --debug    Enables debug messages
              --help     Shows this help
            """.formatted(description(), name());
    }

    @Override
    public boolean isAvailable() {
        return !OS.isWindows();
    }

}
