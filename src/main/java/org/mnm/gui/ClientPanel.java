package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.mnm.client.RunnerOptions;
import org.mnm.config.SettingsStore;

import static org.mnm.config.Settings.readFontScaling;
import static org.mnm.gui.Style.SCALE;

class ClientPanel extends JPanel {

    private final ClientButtonsPanel clientButtons;
    private final InfoPanel infoPanel;
    private final PlayPanel playPanel;

    ClientPanel(JFrame mainWindow,
                GuiCommand.LoginAction loginAction,
                GuiCommand.LogoutAction logoutAction,
                GuiCommand.RepairAction repairAction, BooleanSupplier inMemoryHashing,
                GuiCommand.PlayAction playAction, Supplier<RunnerOptions> optionsSuppler,
                CredentialsHandler credentialsHandler,
                SettingsStore settingsStore) {
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        float fontScaling = readFontScaling(settingsStore);

        this.clientButtons = new ClientButtonsPanel(mainWindow, loginAction, logoutAction, repairAction, inMemoryHashing, credentialsHandler, fontScaling);
        this.infoPanel = new InfoPanel(clientButtons.getPreferredSize().width, Math.round(SCALE * 6 * fontScaling), this.getBackground());
        this.playPanel = new PlayPanel(playAction, optionsSuppler);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(clientButtons);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(infoPanel, BorderLayout.CENTER);
        this.add(Box.createVerticalStrut(SCALE * 3));
        this.add(playPanel);

        GuiComponents.scaleFontSizeRecursively(this, fontScaling);
    }

}
