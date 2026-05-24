package org.mnm.client;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;
import org.mnm.config.OS;
import org.mnm.config.StoredSession;
import org.mnm.tools.JwtParser;
import org.mnm.tools.JwtParser.JwtClaims;

import static org.mnm.tools.Formatting.toInstant;
import static org.mnm.tools.Formatting.width;
import static org.mnm.tools.StringUtils.isEmpty;

public class TokensCommand implements Command {

    private final Supplier<Path> databaseFileLocator;

    public TokensCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        final String slug = args.get("slug");

        try (ConfigDb configDb = ConfigDb.open(databaseFileLocator.get())) {
            configDb.initialize();
            List<StoredSession> sessions = isEmpty(slug) ? configDb.getSessions() : configDb.getSessions(slug);

            List<SessionSummary> list = sessions.stream()
                .map(session -> {
                    JwtClaims claims = JwtParser.parse(session.token());
                    return new SessionSummary(session.id(), session.slug(), claims.email(), toInstant(claims.expiration()).toString());
                })
                .toList();


            System.out.print(format(list));
        }

    }

    private static String format(List<SessionSummary> sessions) {
        if (sessions.isEmpty()) {
            return "No tokens found%n".formatted();
        }

        int slugWidth = width("Slug", sessions.stream().map(SessionSummary::slug));
        // We can assume no one is going to have more than 99 tokens
        int idWidth = 2;
        int emailWidth = width("email", sessions.stream().map(s -> s.email));
        // We assume format like: 2026-06-11T20:45:00Z
        int expirationWidth = 20;

        String rowFormat = "%-" + slugWidth + "s  %-" + idWidth + "s  %" + emailWidth + "s  %" + expirationWidth + "s%n";
        StringBuilder sb = new StringBuilder();
        sb.append(rowFormat.formatted("Slug", "Id", "email", "Expiration"));
        for (SessionSummary session : sessions) {
            sb.append(rowFormat.formatted(session.slug, session.id(), session.email, session.expiration));
        }
        return sb.toString();
    }

    @Override
    public String name() {
        return "tokens";
    }

    @Override
    public String description() {
        return "Lists stored tokens";
    }

    @Override
    public String help() {
        return """
            %s
            
            Usage:
              sweet %s
            
            Options:
              --slug     Show only tokens for the given client slug
              --debug    Enables debug messages
              --help     Shows this help
            """.formatted(description(), name());
    }

    @Override
    public boolean isAvailable() {
        return !OS.isWindows();
    }

    public record SessionSummary(Integer id, String slug, String email, String expiration) {
    }
}
