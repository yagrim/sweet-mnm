package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Desktop;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.mnm.config.Client;
import org.mnm.events.ClientEventHandler;

import static java.nio.file.Files.createDirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.ReflectionTestTools.getButton;
import static org.mnm.gui.ReflectionTestTools.getField;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class ClientPathPanelTest {

    private static final String GAME_SETTINGS = "pfx/drive_c/users/steamuser/AppData/LocalLow/Niche Worlds Cult/Monsters and Memories";

    @AfterEach
    void clearEventListeners() {
        ClientEventHandler.getInstance().clear();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldDisablePathActionsForAnUnresolvedPathAndEnableThemAfterRefresh(@TempDir Path tempDir) throws Exception {
        Client client = new Client("mnm", "1.2.3", UPDATED, tempDir);
        ClientStatus clientStatus = new ClientStatus(client, true, null);
        ClientPathPanel panel = new ClientPathPanel(new InMemorySettingsStore(Map.of()), Color.WHITE, 1.0f);
        JButton copy = getButton(panel, "copyToClipboard");
        JButton open = getButton(panel, "open");
        JTextField path = getField(panel, "textArea", JTextField.class);

        panel.refresh(clientStatus);

        assertThat(path.getText()).isEqualTo("Error: installation not found, run Install or Repair");
        assertThat(copy.isEnabled()).isFalse();
        assertThat(open.isEnabled()).isFalse();

        Path gameSettingsPath = tempDir.resolve("mnm_prefix").resolve(GAME_SETTINGS);
        createDirectories(gameSettingsPath);
        panel.refresh(clientStatus);

        assertThat(path.getText()).isEqualTo(gameSettingsPath.toString());
        assertThat(copy.isEnabled()).isTrue();
        assertThat(open.isEnabled()).isTrue();
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldThrowErrorWhenFileExplorerCannotOpen(@TempDir Path tempDir) throws Exception {
        ClientPathPanel panel = new ClientPathPanel(new InMemorySettingsStore(Map.of()), Color.WHITE, 1.0f);
        JButton open = getButton(panel, "open");
        getField(panel, "textArea", JTextField.class).setText(tempDir.toString());
        open.setEnabled(true);

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
