package org.mnm.cli;

import org.mnm.config.Environment;
import org.mnm.config.VersionDetails;

class VersionCommand implements Command {

    @Override
    public void run(Arguments args) {
        VersionDetails versionDetails = Environment.versionDetails();
        System.out.printf("Version: %s%nGit SHA: %s%n", versionDetails.version(), versionDetails.gitSha());
    }

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String description() {
        return "Shows the version";
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
}
