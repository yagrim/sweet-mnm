package org.mnm.client;

import java.io.File;
import java.nio.file.Path;
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

    public RunCommand(Supplier<Path> locator) {
        this(
            locator,
            (workingDirectory, command, environment) -> ProcessUtils.run(workingDirectory, command, environment),
            OS::isWindows,
            Validators::checkVersion
        );
    }

    RunCommand(Supplier<Path> configDbSupplier, ProcessRunner processRunner, BooleanSupplier windowsDetector, BiConsumer<String, Client> versionChecker) {
        this.configFileLocator = configDbSupplier;
        this.processRunner = processRunner;
        this.windowsDetector = windowsDetector;
        this.versionChecker = versionChecker;
    }

    @Override
    public void run(Arguments args) {
        try (ConfigDb configDb = ConfigDb.open(configFileLocator.get())) {
            configDb.initialize();

            final String slug = args.get("slug");
            final boolean skipVersionCheck = args.getBoolean("skip-version-check");
            final Client client = selectClient(configDb, slug);
            final Token token = selectToken(configDb, client.slug());

            if (!skipVersionCheck) {
                versionChecker.accept(token.token(), client);
            }

            final Path workingDirectory = client.path();
            final boolean isWindows = windowsDetector.getAsBoolean();

            String[] command = buildCommand(client.slug(), token.token(), isWindows);
            Map<String, String> environment = buildEnvironment(isWindows, workingDirectory);

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

    private Token selectToken(ConfigDb configDb, String slug) {
        var tokens = configDb.getTokens(slug);
        if (tokens.isEmpty()) {
            panic("No token found: run 'install --username ...' first");
        }
        if (tokens.size() > 1) {
            panic("Could not identify token");
        }
        return tokens.get(0);
    }

    private String[] buildCommand(String slug, String token, boolean isWindows) {
        if (isWindows) {
            return new String[]{concatPath(slug, "mnm.exe"), "--token", token};
        }
        return new String[]{"umu-run", concatPath(".", slug, "mnm.exe"), "--token", token};
    }

    private static Map<String, String> buildEnvironment(boolean isWindows, Path workingDirectory) {
        return isWindows
            ? Map.of()
            : Map.of(
            "GAMEID", "mnm",
            "PROTONPATH", "GE-Proton10-33",
            "WINEPREFIX", workingDirectory.toAbsolutePath().resolve("mnm_prefix").toString());
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
              sweet %s [--slug <slug>] [--skip-version-check]
            
            Options:
              --slug               Client slug to run (optional)
              --skip-version-check  Skip client version validation
              --debug               Enables debug messages
              --help                Shows this help
            """.formatted(description(), name());
    }

}
