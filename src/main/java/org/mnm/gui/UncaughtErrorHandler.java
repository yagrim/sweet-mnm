package org.mnm.gui;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the errors that no other code catches.
 * For example, errors from button listeners, timers or background threads.
 * <p>
 * NOTE: does not capture errors from {@link java.util.concurrent.CompletableFuture}.
 * The future keeps these errors. The code that uses the future must handle them.
 *
 * @see Thread#setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)
 */
class UncaughtErrorHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(UncaughtErrorHandler.class);

    private final Consumer<String> errorDialog;

    UncaughtErrorHandler(Consumer<String> errorDialog) {
        this.errorDialog = errorDialog;
    }

    static void install() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtErrorHandler(MessageDialog::showErrorMessageDialogSync));
    }

    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        logger.error("Uncaught error in thread {}", thread.getName(), error);
        SwingUtilities.invokeLater(() -> {
            // Do not let a dialog error come back to this handler, to prevent a loop
            try {
                errorDialog.accept("Error: " + error.getMessage());
            } catch (RuntimeException e) {
                logger.error("Could not show error dialog", e);
            }
        });
    }
}
