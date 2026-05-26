package org.mnm.client;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;

import static org.mnm.tools.Formatting.width;

public class ClientsCommand implements Command {

    private final Supplier<Path> configFileLocator;

    public ClientsCommand(Supplier<Path> configDbSupplier) {
        this.configFileLocator = configDbSupplier;
    }

    @Override
    public void run(Arguments args) {
        try (ConfigDb configDb = ConfigDb.open(configFileLocator.get())) {
            configDb.initialize();

            List<ClientSummary> clients = configDb.getClients().stream()
                .map(client -> new ClientSummary(
                    client.slug(),
                    client.version(),
                    configDb.getTokens(client.slug()).size(),
                    client.path().toString()))
                .toList();

            System.out.print(format(clients));
        }
    }

    private static String format(List<ClientSummary> clients) {
        if (clients.isEmpty()) {
            return "No clients found%n".formatted();
        }

        int slugWidth = width("Slug", clients.stream().map(c -> c.slug));
        int versionWidth = width("Version", clients.stream().map(c -> c.version));
        // We can assume no one is going to have more than 99999 tokens
        int tokensWidth = 6;
        int pathWidth = width("Install_path", clients.stream().map(c -> c.path));

        String rowFormat = "%-" + slugWidth + "s  %-" + versionWidth + "s  %" + tokensWidth + "s  %-" + pathWidth + "s%n";
        StringBuilder sb = new StringBuilder();
        sb.append(rowFormat.formatted("Slug", "Version", "Tokens", "Install_path"));
        for (ClientSummary client : clients) {
            sb.append(rowFormat.formatted(client.slug(), client.version(), client.tokens(), client.path()));
        }
        return sb.toString();
    }

    @Override
    public String name() {
        return "clients";
    }

    @Override
    public String description() {
        return "Lists configured clients";
    }

    @Override
    public String help() {
        return """
            %s
            
            Usage:
              sweet %s
            
            Options:
              --debug  Enables debug messages
              --help   Shows this help
            """.formatted(description(), name());
    }

    public record ClientSummary(String slug, String version, int tokens, String path) {
    }
}
