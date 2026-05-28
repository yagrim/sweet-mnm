package org.mnm.client;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;

public class RunCommand implements Command {

    @FunctionalInterface
    interface ProcessRunner {
        String run(Path workingDirectory, String[] command, Map<String, String> environment);
    }

    private final Supplier<Path> configFileLocator;
    private final BiConsumer<RunnerOptions, ConfigDb> runner;

    public RunCommand(Supplier<Path> locator) {
        this(locator, Factories::runner);
    }

    RunCommand(Supplier<Path> configFileLocator, BiConsumer<RunnerOptions, ConfigDb> runner) {
        this.configFileLocator = configFileLocator;
        this.runner = runner;
    }

    @Override
    public void run(Arguments args) {
        try (ConfigDb configDb = ConfigDb.open(configFileLocator.get())) {
            runner.accept(RunnerOptions.parse(args), configDb);
        }
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
