package org.mnm.launcher;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.tools.JwtParser;
import org.mnm.tools.JwtParser.JwtClaims;

import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Supplier;

public class TokenInfoCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public TokenInfoCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        final TokenService tokenService = new TokenService();
        final String token = tokenService.retrieveToken(databaseFileLocator.get());

        if (token == null) {
            System.out.println("No token found");
        } else {
            JwtClaims claims = JwtParser.parse(token);

            StringBuilder sb = new StringBuilder();
            sb.append("%-7s : %s%n".formatted("issuer", claims.issuer()));
            sb.append("%-7s : %s%n".formatted("created", toInstant(claims.issuedAt())));
            sb.append("%-7s : %s%n".formatted("expires", toInstant(claims.expiration())));
            sb.append("%-7s : %s%n".formatted("email", claims.email()));
            System.out.printf("%s", sb);
        }
    }


    private static Instant toInstant(long value) {
        return value > 0 ? Instant.ofEpochSecond(value) : null;
    }


    @Override
    public String name() {
        return "token-info";
    }

    @Override
    public String description() {
        return "Shows official launcher token information";
    }

    @Override
    public String help() {
        return description();
    }

}
