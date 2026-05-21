package org.mnm.cli;

import java.util.Comparator;
import java.util.List;


class HelpCommand implements Command {

    private final List<Command> commands;

    HelpCommand(List<Command> commands) {
        this.commands = commands;
    }

    @Override
    public void run(Arguments args) {
        int indentation = commands.stream()
            .mapToInt(command -> command.name().length())
            .max()
            .getAsInt() + 3;

        final StringBuilder sb = new StringBuilder();

        sb.append("(The unofficial and...) Sweet tool to manage Monsters & Memories clients\n\n");
        usage(sb);
        commands(sb, indentation);
        options(sb);

        System.out.println(sb);
    }

    private void usage(StringBuilder sb) {
        sb.append("Usage:\n");
        sb.append("  sweet <command> [--option [value]] ...\n\n");
    }

    private void commands(StringBuilder sb, int indentation) {
        sb.append("Available commands:\n");

        commands.stream()
            .sorted(Comparator.comparing(Command::name))
            .forEach(c -> {
                sb.append(format(c.name(), c.description(), indentation));
                sb.append("\n");
            });
    }

    private static StringBuilder options(StringBuilder sb) {
        return sb.append("""
            
            Options:
              --debug  Enables debug messages
              --help   Shows this help""");
    }

    private static String format(String first, String second, int distance) {
        return String.format("  %-" + distance + "s%s", first, second);
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Shows available commands";
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
