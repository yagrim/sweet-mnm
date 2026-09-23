package org.mnm.tools;

import java.io.IOException;

public class CommandNotFound extends RuntimeException {
    public CommandNotFound(String message, IOException e) {
        super(message, e);
    }
}
