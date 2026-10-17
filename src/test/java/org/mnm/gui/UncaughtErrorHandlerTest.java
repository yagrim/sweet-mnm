package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mnm.LoggerHandler;
import org.mnm.SystemOutCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SystemOutCaptureExtension.class)
class UncaughtErrorHandlerTest {

    @BeforeEach
    void beforeEach() {
        LoggerHandler.setInfo(true);
    }

    @AfterEach
    void afterEach() {
        LoggerHandler.setInfo(false);
    }

    @Test
    void shouldLogErrorAndShowDialog(SystemOutCaptureExtension out) throws Exception {
        final List<String> messages = new CopyOnWriteArrayList<>();
        final var handler = new UncaughtErrorHandler(messages::add);

        handler.uncaughtException(new Thread("test-thread"), new IllegalStateException("Unexpected failure"));
        SwingUtilities.invokeAndWait(() -> {
        });

        assertThat(messages).containsExactly("Error: Unexpected failure");
        assertThat(out.getOutput() + out.getErrorOutput())
            .contains("Uncaught error in thread test-thread")
            .contains("java.lang.IllegalStateException: Unexpected failure");
    }

    @Test
    void shouldLogErrorWhenDialogFails(SystemOutCaptureExtension out) throws Exception {
        final var handler = new UncaughtErrorHandler(_ -> {
            throw new IllegalStateException("No display");
        });

        handler.uncaughtException(new Thread("test-thread"), new IllegalStateException("Unexpected failure"));
        SwingUtilities.invokeAndWait(() -> {
        });

        assertThat(out.getOutput() + out.getErrorOutput())
            .contains("Could not show error dialog")
            .contains("java.lang.IllegalStateException: No display");
    }

    @Test
    void shouldHandleErrorFromButtonListener() throws Exception {
        final List<String> messages = new CopyOnWriteArrayList<>();
        final var previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtErrorHandler(messages::add));
        try {
            final JButton button = new JButton();
            button.addActionListener(_ -> {
                throw new IllegalStateException("Button failure");
            });

            SwingUtilities.invokeLater(button::doClick);
            // Two passes: one for the click, one for the dialog that the handler queues
            SwingUtilities.invokeAndWait(() -> {
            });
            SwingUtilities.invokeAndWait(() -> {
            });

            assertThat(messages).containsExactly("Error: Button failure");
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
        }
    }
}
