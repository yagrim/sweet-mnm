package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JTextField;
import java.awt.Color;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.config.Client;
import org.mnm.events.ClientEventHandler;

import static java.nio.file.Files.createDirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.ReflectionTestTools.getField;

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
        PathPanel pathPanel = getField(panel, "pathPanel", PathPanel.class);
        JButton copy = getField(pathPanel, "copyToClipboard", JButton.class);
        JButton open = getField(pathPanel, "open", JButton.class);
        JTextField path = getField(pathPanel, "textArea", JTextField.class);

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

}
