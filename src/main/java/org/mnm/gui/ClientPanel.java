package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.function.BooleanSupplier;

import org.mnm.config.SettingsStore;

import static org.mnm.gui.Style.SCALE;

class ClientPanel extends JPanel {

    ClientPanel(JFrame mainWindow,
                GuiCommand.LoginAction loginAction,
                GuiCommand.LogoutAction logoutAction,
                GuiCommand.RepairAction repairAction, BooleanSupplier inMemoryHashing,
                SettingsStore settingsStore,
                CredentialsHandler credentialsHandler,
                float uiScaling) {
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        ClientButtonsPanel clientButtons = new ClientButtonsPanel(mainWindow, loginAction, logoutAction, repairAction, inMemoryHashing, credentialsHandler, uiScaling);

        Color color = this.getBackground();
        InfoPanel infoPanel = new InfoPanel(clientButtons.getPreferredSize().width, Math.round(SCALE * 6 * uiScaling), color, uiScaling);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(clientButtons);
        this.add(infoPanel, BorderLayout.CENTER);
        this.add(new ClientPathPanel(settingsStore, color, uiScaling), BorderLayout.CENTER);
    }

}
