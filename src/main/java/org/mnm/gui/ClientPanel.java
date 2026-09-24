package org.mnm.gui;

import org.mnm.config.SettingsStore;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.BooleanSupplier;

import static org.mnm.gui.Style.SCALE;

class ClientPanel extends JPanel {

    private final ClientButtonsPanel clientButtons;
    private final InfoPanel infoPanel;

    ClientPanel(JFrame mainWindow,
                GuiCommand.LoginAction loginAction,
                GuiCommand.LogoutAction logoutAction,
                GuiCommand.RepairAction repairAction, BooleanSupplier inMemoryHashing,
                CredentialsHandler credentialsHandler,
                SettingsStore settingsStore,
                float uiScaling) {
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        this.clientButtons = new ClientButtonsPanel(mainWindow, loginAction, logoutAction, repairAction, inMemoryHashing, credentialsHandler, settingsStore, uiScaling);
        this.infoPanel = new InfoPanel(clientButtons.getPreferredSize().width, Math.round(SCALE * 6 * uiScaling), this.getBackground(), uiScaling);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(clientButtons);
        this.add(infoPanel, BorderLayout.CENTER);
    }

}
