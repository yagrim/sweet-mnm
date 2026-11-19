package org.mnm.gui;

import javax.swing.JTextField;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.LoggerHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.gui.ReflectionTestTools.getButton;
import static org.mnm.gui.ReflectionTestTools.getField;

class SweetLogsPanelTest {

    @Test
    void shouldShowLogDirectoryWhenItExists(@TempDir Path tempDir) throws IOException {
        Path logDirectory = Files.createDirectories(tempDir.resolve("logs"));

        try (MockedStatic<LoggerHandler> loggerHandler = Mockito.mockStatic(LoggerHandler.class)) {
            loggerHandler.when(LoggerHandler::getLogDirectory).thenReturn(logDirectory);

            PathPanel pathPanel = pathPanel(new SweetLogsPanel(1.0f));

            assertThat(getField(pathPanel, "textArea", JTextField.class).getText()).isEqualTo(logDirectory.toString());
            assertThat(getButton(pathPanel, "copyToClipboard").isEnabled()).isTrue();
            assertThat(getButton(pathPanel, "open").isEnabled()).isTrue();
        }
    }

    @Test
    void shouldShowErrorWhenLogDirectoryDoesNotExist(@TempDir Path tempDir) {
        try (MockedStatic<LoggerHandler> loggerHandler = Mockito.mockStatic(LoggerHandler.class)) {
            loggerHandler.when(LoggerHandler::getLogDirectory).thenReturn(tempDir.resolve("missing"));

            PathPanel pathPanel = pathPanel(new SweetLogsPanel(1.0f));

            assertThat(getField(pathPanel, "textArea", JTextField.class).getText()).isEqualTo("Error: no log folder found");
            assertThat(getButton(pathPanel, "copyToClipboard").isEnabled()).isFalse();
            assertThat(getButton(pathPanel, "open").isEnabled()).isFalse();
        }
    }

    @Test
    void shouldLimitHeight() {
        SweetLogsPanel panel = new SweetLogsPanel(1.0f);

        assertThat(panel.getMaximumSize().height).isLessThan(panel.getPreferredSize().height);
    }

    private static PathPanel pathPanel(SweetLogsPanel panel) {
        return getField(panel, "pathPanel", PathPanel.class);
    }

}
