package org.mnm.launcher;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.OS;
import org.mnm.tools.JwtParser;
import org.mnm.tools.ProcessUtils;

import static org.mnm.tools.Formatting.toInstant;
import static org.mnm.tools.StringUtils.isEmpty;

public class LauncherTokenCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public LauncherTokenCommand(Supplier<Path> locator) {
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
                String formattedToken = formatToColumns(token);
                System.out.printf(formattedToken);
            } else {
                ProcessUtils.panic("Invalid output: " + output);
            }
        }
    }

    public static String formatToColumns(String token) {
        JwtParser.JwtClaims claims = JwtParser.parse(token);

        StringBuilder sb = new StringBuilder();
        sb.append("%-7s : %s%n".formatted("issuer", claims.issuer()));
        sb.append("%-7s : %s%n".formatted("created", toInstant(claims.issuedAt())));
        sb.append("%-7s : %s%n".formatted("expires", toInstant(claims.expiration())));
        sb.append("%-7s : %s%n".formatted("email", claims.email()));
        return sb.toString();
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
              --output   Set output format: 'raw' for JWT token (default ), 'rows' for metadata
              --debug    Enables debug messages
              --help     Shows this help
            """.formatted(description(), name());
    }

    @Override
    public boolean isAvailable() {
        return !OS.isWindows();
    }

}
