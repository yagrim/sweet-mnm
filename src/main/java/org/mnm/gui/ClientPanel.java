package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Supplier;

import org.mnm.client.RunnerOptions;

class ClientPanel extends JPanel {

    static final int SCALE = 8;

    private final ClientButtonsPanel clientButtons;
    private final InfoPanel infoPanel;
    private final PlayPanel playPanel;

    ClientPanel(JFrame mainWindow,
                GuiCommand.LoginAction loginAction,
                GuiCommand.LogoutAction logoutAction,
                GuiCommand.PlayAction playAction, Supplier<RunnerOptions> optionsSuppler) {
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // ClientButtonsHandler: LoginListener, LogoutListener, RepairListener
        // OptionsPanel: RepairListener
        // infoPanel: LoginListener, LogoutListener, RepairListener
        // TODO move play button to PlayPannel, implements LoginListener, LogoutListener, RepairListener

        this.clientButtons = new ClientButtonsPanel(mainWindow, loginAction, logoutAction);
        this.infoPanel = new InfoPanel(clientButtons.getPreferredSize().width, SCALE * 6, this.getBackground());
        this.playPanel = new PlayPanel(playAction, optionsSuppler);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(clientButtons);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(infoPanel, BorderLayout.CENTER);
        this.add(Box.createVerticalStrut(SCALE * 3));
        this.add(playPanel);
    }

}
