package org.mnm.client;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.OS;
import org.mnm.config.Token;
import org.mnm.tools.JwtParser;
import org.mnm.tools.ProcessUtils;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class ClientRunner {

    private final Logger logger = LoggerFactory.getLogger(ClientRunner.class);

    private final ConfigDb configDb;

    public ClientRunner(ConfigDb configDb) {
        this.configDb = configDb;
    }

    public void run(RunnerOptions options) {

        final String slug = options.slug();
        final Integer tokenId = options.tokenId();

        final Client client = selectClient(configDb, slug);
        final Token token = selectToken(configDb, client.slug(), tokenId);
        tokenIsNotExpired(token);

        if (!options.skipVersionCheck()) {
            Validators.checkVersion(token.token(), client);
        }

        final Path clientPath = client.path();
        final boolean isWindows = OS.isWindows();

        String[] command = buildCommand(client.slug(), token.token(), isWindows);
        Map<String, String> environment = buildEnvironment(isWindows, clientPath, System.getenv(), options.linuxOptions());

        logger.info("Running: {}", String.join(" ", redactToken(command)));
        logger.info("Working directory: {}", clientPath);
        if (!environment.isEmpty()) {
            logger.info("Environment variables: {}", environment);
        }

        ProcessUtils.run(clientPath, command, environment);
    }

    private static void tokenIsNotExpired(Token token) {
        if (JwtParser.parse(token.token()).isExpired()) {
            panic("Token expired: run 'install --username ...' to create a new one");
        }
    }

    private Client selectClient(ConfigDb configDb, String slug) {
        if (!isEmpty(slug)) {
            Client client = configDb.getClient(slug);
            if (client == null) {
                panic("No client found: run 'install --username ...' first");
            }
            return client;
        }

        var clients = configDb.getClients();
        if (clients.isEmpty()) {
            panic("No client found: run 'install --username ...' first");
        }
        if (clients.size() > 1) {
            panic("Could not identify client: use --slug");
        }
        Client defaultClient = clients.get(0);
        logger.debug("Found 1 client: {}, {}", defaultClient.slug(), defaultClient.path());
        return defaultClient;
    }

    private Token selectToken(ConfigDb configDb, String slug, Integer id) {
        if (id != null) {
            Token token = configDb.getToken(id);
            if (token == null || !slug.equals(token.slug())) {
                panic("No token found for id %s".formatted(id));
            }
            return token;
        }

        var tokens = configDb.getTokens(slug);
        if (tokens.isEmpty()) {
            panic("No token found: run 'install --username ...' first");
        }
        if (tokens.size() > 1) {
            panic("Could not identify token: use --id");
        }
        return tokens.get(0);
    }

    private String[] buildCommand(String slug, String token, boolean isWindows) {
        final String clientPath = concatPath(slug, "mnm.exe");
        if (isWindows) {
            return new String[]{clientPath, "--token", token};
        }
        return new String[]{"umu-run", clientPath, "--token", token};
    }

    private static String concatPath(String... parts) {
        return String.join(OS.isWindows() ? "\\" : "/", parts);
    }

    private static String[] redactToken(String[] command) {
        String[] redacted = command.clone();
        for (int i = 0; i < redacted.length - 1; i++) {
            if ("--token".equals(redacted[i])) {
                redacted[i + 1] = "***";
            }
        }
        return redacted;
    }

    private Map<String, String> buildEnvironment(boolean isWindows, Path clientPath, Map<String, String> currentEnvironment, RunnerOptions.LinuxOptions linuxOptions) {
        return isWindows
            ? buildWine(linuxOptions.enableMangoHud())
            : buildLinuxEnvironment(clientPath, currentEnvironment, linuxOptions);
    }

    private Map<String, String> buildLinuxEnvironment(Path clientPath, Map<String, String> currentEnvironment, RunnerOptions.LinuxOptions linuxOptions) {
        Map<String, String> environment = new HashMap<>();

        Objects.requireNonNull(linuxOptions, "linuxOptions cannot be null");
        Objects.requireNonNull(linuxOptions.umuOptions(), "linuxOptions.umuOptions cannot be null");

        environment.put("GAMEID", currentEnvironment.getOrDefault("GAMEID", linuxOptions.umuOptions().gameId()));
        environment.put("PROTONPATH", currentEnvironment.getOrDefault("PROTONPATH", linuxOptions.umuOptions().protonPath()));

        String candidateWinePrefixPath;
        if (linuxOptions.useClientAsPrefix() || isEmpty(linuxOptions.umuOptions().winePrefix())) {
            candidateWinePrefixPath = clientPath.toAbsolutePath().resolve("mnm_prefix").toString();
        } else {
            String prefix = linuxOptions.umuOptions().winePrefix();
            Path prefixPath = Path.of(prefix);
            if (prefixPath.isAbsolute()) {
                candidateWinePrefixPath = prefix;
            } else {
                candidateWinePrefixPath = clientPath.toAbsolutePath().resolve(prefixPath).toString();
            }
        }

        environment.put("WINEPREFIX", currentEnvironment.getOrDefault("WINEPREFIX", candidateWinePrefixPath));

        if (linuxOptions.enableMangoHud()) {
            environment.put("MANGOHUD", "1");
        }

        return Map.copyOf(environment);
    }

    private static Map<String, String> buildWine(boolean mangoHudEnabled) {
        Map<String, String> environment = new HashMap<>();
        if (mangoHudEnabled) {
            environment.put("MANGOHUD", "1");
        }
        return Map.copyOf(environment);
    }

}
