package org.mnm.gui;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

import org.mnm.config.SettingsStore;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.Refreshable;

import static org.mnm.config.SettingsStore.DEFAULT_UMU_USE_CLIENT_AS_PREFIX;

class ClientPathPanel extends JPanel
    implements Refreshable {

    private static final String ERROR_PREFIX = "Error:";

    private final SettingsStore settingsStore;
    private final PathPanel pathPanel;

    ClientPathPanel(SettingsStore settingsStore, Color color, float uiScaling) {
        // BorderLayout gives all the space to the path panel, as before the refactoring.
        super(new BorderLayout());
        this.settingsStore = settingsStore;
        this.pathPanel = new PathPanel("Settings & logs", "Settings, UI profiles and logs", color, uiScaling);
        add(pathPanel, BorderLayout.CENTER);

        ClientEventHandler.getInstance().register(this);
    }

    @Override
    public void refresh(ClientStatus client) {
        boolean clientAsWinePrefix = settingsStore.getBoolean(SettingsStore.UMU_USE_CLIENT_AS_PREFIX, DEFAULT_UMU_USE_CLIENT_AS_PREFIX);
        ClientPaths clientPaths = new ClientPaths(client, clientAsWinePrefix);
        String gameSettingsPath = clientPaths.getGameSettingsPath();
        pathPanel.setPath(gameSettingsPath, !gameSettingsPath.startsWith(ERROR_PREFIX));
    }

}
