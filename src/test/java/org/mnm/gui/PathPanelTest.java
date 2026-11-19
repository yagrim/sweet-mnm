package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Desktop;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.gui.ReflectionTestTools.getButton;
import static org.mnm.gui.ReflectionTestTools.getField;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class PathPanelTest {

    @Test
    void shouldDisableButtonsInitially() {
        PathPanel panel = new PathPanel("Label", "Tooltip", Color.WHITE, 1.0f);

        assertThat(getButton(panel, "copyToClipboard").isEnabled()).isFalse();
        assertThat(getButton(panel, "open").isEnabled()).isFalse();
    }

    @Test
    void shouldShowPathAndToggleButtonsWithValidity() {
        PathPanel panel = new PathPanel("Label", "Tooltip", Color.WHITE, 1.0f);
        JTextField path = getField(panel, "textArea", JTextField.class);

        panel.setPath("/some/path", true);

        assertThat(path.getText()).isEqualTo("/some/path");
        assertThat(path.getToolTipText()).isEqualTo("/some/path");
        assertThat(getButton(panel, "copyToClipboard").isEnabled()).isTrue();
        assertThat(getButton(panel, "open").isEnabled()).isTrue();

        panel.setPath("Error: no path", false);

        assertThat(path.getText()).isEqualTo("Error: no path");
        assertThat(getButton(panel, "copyToClipboard").isEnabled()).isFalse();
        assertThat(getButton(panel, "open").isEnabled()).isFalse();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldThrowErrorWhenFileExplorerCannotOpen(@TempDir Path tempDir) throws Exception {
        PathPanel panel = new PathPanel("Label", "Tooltip", Color.WHITE, 1.0f);
        JButton open = getButton(panel, "open");
        panel.setPath(tempDir.toString(), true);

        Desktop desktop = Mockito.mock(Desktop.class);
        doThrow(new IOException("No desktop")).when(desktop).open(any());

        try (MockedStatic<Desktop> desktopMock = Mockito.mockStatic(Desktop.class);
             MockedConstruction<ProcessBuilder> processBuilderMock = Mockito.mockConstruction(ProcessBuilder.class,
                 (mock, _) -> when(mock.start()).thenThrow(new IOException("xdg-open not found")))) {
            desktopMock.when(Desktop::getDesktop).thenReturn(desktop);

            assertThatThrownBy(open::doClick)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessage("xdg-open not found");
            assertThat(processBuilderMock.constructed()).hasSize(1);
        }
    }

}
