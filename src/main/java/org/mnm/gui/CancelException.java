package org.mnm.gui;

/**
 * Exception to abort an action without signaling an error.
 */
public class CancelException extends RuntimeException {

    public CancelException(String message) {
        super(message);
    }
}
