package org.mnm.cli;

import java.util.Comparator;
import java.util.List;

public class HelpCommand implements Command {

    private final List<Command> commands;

    public HelpCommand(List<Command> commands) {
        this.commands = commands;
    }

    @Override
    public void run(Arguments args) {
        int indentation = commands.stream()
                .mapToInt(command -> command.name().length())
                .max()
                .getAsInt() + 3;

        final StringBuilder sb = new StringBuilder();

        commands.stream()
                .sorted(Comparator.comparing(Command::name))
                .forEach(c -> {
                    sb.append(format(c.name(), c.help(), indentation));
                    sb.append("\n");
                });

        sb.append(format(this.name(), this.help(), indentation));
        System.out.println(sb);
    }

    public static String format(String first, String second, int distance) {
        return String.format("%-" + distance + "s%s", first, second);
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Displays available commands";
    }

    @Override
    public String help() {
        return description();
    }

}
