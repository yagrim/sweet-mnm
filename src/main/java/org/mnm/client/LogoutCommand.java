package org.mnm.client;

import java.nio.file.Path;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.config.ConfigDb;

import static org.mnm.tools.ProcessUtils.panic;
import static org.mnm.tools.StringUtils.isEmpty;

public class LogoutCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(LogoutCommand.class);

    private final Supplier<Path> databaseFileLocator;

    public LogoutCommand(Supplier<Path> locator) {
        this.databaseFileLocator = locator;
    }

    @Override
    public void run(Arguments args) {
        final String slug = args == null ? null : args.get("slug");
        if (isEmpty(slug)) {
            panic("Missing or empty parameter: '--slug'");
        }

        try (ConfigDb configDb = ConfigDb.open(databaseFileLocator.get())) {
            int deletedTokens = configDb.deleteTokens(slug);
            logger.info("Removed {} token(s) for slug '{}'", deletedTokens, slug);
        }
    }

    @Override
    public String name() {
        return "logout";
    }

    @Override
    public String description() {
        return "Removes stored tokens for a client slug";
    }

    @Override
    public String help() {
        return """
            %s
            
            Usage:
              sweet %s --slug <slug>
            
            Options:
              --slug     Client slug whose tokens will be removed (required)
              --debug    Enables debug messages
              --help     Shows this help
            """.formatted(description(), name());
    }

}
