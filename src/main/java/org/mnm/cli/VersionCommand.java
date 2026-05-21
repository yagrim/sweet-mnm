package org.mnm.cli;

import org.mnm.tools.FileUtils;

class VersionCommand implements Command {

    @Override
    public void run(Arguments args) {
        String version = FileUtils.readFromClasspath("version.txt");
        System.out.printf("Version: %s", version);
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
