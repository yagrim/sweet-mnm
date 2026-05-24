package org.mnm.client;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;
import org.mnm.config.Token;
import org.mnm.launcher.LauncherTokenCommand;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class TokenCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public TokenCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        final String idValue = args == null ? null : args.get("id");
        if (isEmpty(idValue)) {
            panic("Missing or empty parameter: '--id'");
        }

        final int id;
        try {
            id = Integer.parseInt(idValue);
        } catch (NumberFormatException e) {
            panic("Invalid id: expected integer but found %s".formatted(idValue));
            return;
        }

        try (ConfigDb configDb = ConfigDb.open(databaseFileLocator.get())) {
            configDb.initialize();

            final Token tokenRow = configDb.getToken(id);
            final String token = tokenRow == null ? null : tokenRow.token();

            if (isEmpty(token)) {
                System.out.println("No token found for id %s".formatted(id));
            } else {
                final String output = args == null ? null : args.get("output");
                if (isEmpty(output) || output.equals("raw")) {
                    System.out.println(token);
                } else if (output.equals("rows")) {
                    String formattedToken = LauncherTokenCommand.formatToColumns(token);
                    System.out.printf(formattedToken);
                } else {
                    panic("Invalid output: " + output);
                }
            }
        }
    }

    @Override
    public String name() {
        return "token";
    }

    @Override
    public String description() {
        return "Shows stored token by id";
    }

    @Override
    public String help() {
        return """
            %s
            
            Usage:
              sweet %s --id <id>
            
            Options:
              --id       Token id (required)
              --output   Set output format: 'raw' for JWT token (default ), 'rows' for metadata
              --debug    Enables debug messages
              --help     Shows this help
            """.formatted(description(), name());
    }

}
