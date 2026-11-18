package org.mnm.tools;

import java.util.List;

public record ClientCommand(List<String> arguments) {

    private static final String TOKEN_OPTION = "--token";
    private static final String REDACTED = "***";

    public ClientCommand {
        if (arguments == null || arguments.isEmpty()) {
            throw new IllegalArgumentException("ClientCommand cannot be empty");
        }
        arguments = List.copyOf(arguments);
    }

    public static ClientCommand of(String... arguments) {
        return new ClientCommand(List.of(arguments));
    }

    public String executable() {
        return arguments.getFirst();
    }

    public String[] toArray() {
        return arguments.toArray(String[]::new);
    }

    @Override
    public String toString() {
        String[] redacted = toArray();
        for (int i = 0; i < redacted.length - 1; i++) {
            if (TOKEN_OPTION.equals(redacted[i])) {
                redacted[i + 1] = REDACTED;
            }
        }
        return String.join(" ", redacted);
    }

}
