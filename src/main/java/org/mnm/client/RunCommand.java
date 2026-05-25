package org.mnm.client;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.OS;
import org.mnm.config.Token;
import org.mnm.tools.ProcessUtils;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class RunCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(RunCommand.class);


    @FunctionalInterface
    interface ProcessRunner {
        String run(Path workingDirectory, String[] command, Map<String, String> environment);
    }

    private final Supplier<Path> configFileLocator;
    private final ProcessRunner processRunner;
    private final BooleanSupplier windowsDetector;
    private final BiConsumer<String, Client> versionChecker;
    private final Supplier<Map<String, String>> environmentSupplier;

    public RunCommand(Supplier<Path> locator) {
        this(
            locator,
            (workingDirectory, command, environment) -> ProcessUtils.run(workingDirectory, command, environment),
            OS::isWindows,
            Validators::checkVersion,
            System::getenv
        );
    }

    RunCommand(Supplier<Path> configDbSupplier, ProcessRunner processRunner, BooleanSupplier windowsDetector, BiConsumer<String, Client> versionChecker) {
        this(configDbSupplier, processRunner, windowsDetector, versionChecker, System::getenv);
    }

    RunCommand(Supplier<Path> configDbSupplier, ProcessRunner processRunner, BooleanSupplier windowsDetector, BiConsumer<String, Client> versionChecker, Supplier<Map<String, String>> environmentSupplier) {
        this.configFileLocator = configDbSupplier;
        this.processRunner = processRunner;
        this.windowsDetector = windowsDetector;
        this.versionChecker = versionChecker;
        this.environmentSupplier = environmentSupplier;
    }

    @Override
    public void run(Arguments args) {
        try (ConfigDb configDb = ConfigDb.open(configFileLocator.get())) {
            configDb.initialize();

            final String slug = args.get("slug");
            final Integer tokenId = parseTokenId(args.get("id"));
            final boolean skipVersionCheck = args.getBoolean("skip-version-check");
            final Client client = selectClient(configDb, slug);
            final Token token = selectToken(configDb, client.slug(), tokenId);

            if (!skipVersionCheck) {
                versionChecker.accept(token.token(), client);
            }

            final Path workingDirectory = client.path();
            final boolean isWindows = windowsDetector.getAsBoolean();

            String[] command = buildCommand(client.slug(), token.token(), isWindows);
            Map<String, String> environment = buildEnvironment(isWindows, workingDirectory, environmentSupplier.get());

            logger.info("Running: {}", String.join(" ", command));
            logger.info("Working directory: {}", workingDirectory);
            if (!environment.isEmpty()) {
                logger.info("Environment variables: {}", environment);
            }

            processRunner.run(workingDirectory, command, environment);
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
        return clients.get(0);
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

    private Integer parseTokenId(String tokenId) {
        if (isEmpty(tokenId)) {
            return null;
        }

        try {
            return Integer.valueOf(tokenId);
        } catch (NumberFormatException e) {
            panic("Invalid token id: %s".formatted(tokenId));
            return null;
        }
    }

    private String[] buildCommand(String slug, String token, boolean isWindows) {
        if (isWindows) {
            return new String[]{concatPath(slug, "mnm.exe"), "--token", token};
        }
        return new String[]{"umu-run", concatPath(".", slug, "mnm.exe"), "--token", token};
    }

    private static Map<String, String> buildEnvironment(boolean isWindows, Path workingDirectory, Map<String, String> currentEnvironment) {
        return isWindows ? Map.of() : buildLinuxEnvironment(workingDirectory, currentEnvironment);
    }

    private static Map<String, String> buildLinuxEnvironment(Path workingDirectory, Map<String, String> currentEnvironment) {
        Map<String, String> environment = new HashMap<>();
        environment.put("GAMEID", currentEnvironment.getOrDefault("GAMEID", "mnm"));
        environment.put("PROTONPATH", currentEnvironment.getOrDefault("PROTONPATH", "GE-Proton10-33"));
        environment.put("WINEPREFIX", currentEnvironment.getOrDefault("WINEPREFIX", workingDirectory.toAbsolutePath().resolve("mnm_prefix").toString()));
        return Map.copyOf(environment);
    }

    private static String concatPath(String... parts) {
        return String.join(File.separator, parts);
    }

    @Override
    public String name() {
        return "run";
    }

    @Override
    public String description() {
        return "Runs configured client";
    }

    @Override
    public String help() {
        return """
            %s
            
            Usage:
              sweet %s [--slug <slug>] [--id <id>] [--skip-version-check]
            
            Options:
              --slug                 Client slug to run (optional)
              --id                   Token id to use when multiple tokens exist
              --skip-version-check   Skip client version validation
              --debug                Enables debug messages
              --help                 Shows this help
            """.formatted(description(), name());
    }

}
