package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ClientPanel extends JPanel
    implements LoginListener, RepairListener {

    static final int SCALE = 8;

    final List<LoginListener> loginListeners;
    final List<RepairListener> repairListeners;

    private final ClientButtonsPanel clientButtons;
    private final InfoPanel infoPanel;
    private final PlayPanel playPanel;

    ClientPanel(JFrame parent) {
        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        // ClientButtonsHandler: LoginListener, LogoutListener, RepairListener
        // OptionsPanel: RepairListener
        // infoPanel: LoginListener, LogoutListener, RepairListener
        // TODO move play button to PlayPannel, implements LoginListener, LogoutListener, RepairListener

        this.clientButtons = new ClientButtonsPanel();
        this.infoPanel = new InfoPanel(clientButtons.getPreferredSize().width, SCALE * 6, this.getBackground());
        this.playPanel = new PlayPanel();

        var listeners = List.of(clientButtons, infoPanel, playPanel);
        loginListeners = (List<LoginListener>) (List<?>) listeners;
        repairListeners = (List<RepairListener>) (List<?>) listeners;

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(clientButtons);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(infoPanel, BorderLayout.CENTER);
        this.add(Box.createVerticalStrut(SCALE * 3));
        this.add(playPanel);
    }

    @Override
    public void loginStart() {
        loginListeners.forEach(LoginListener::loginStart);
    }

    @Override
    public void loginDone(ClientStatus client) {
        loginListeners.forEach(l -> l.loginDone(client));
    }

    @Override
    public void logoutDone() {
        loginListeners.forEach(LoginListener::logoutDone);
    }

    @Override
    public void repairStart() {
        repairListeners.forEach(RepairListener::repairStart);
    }

    @Override
    public void repairDone(ClientStatus client) {
        repairListeners.forEach(l -> l.repairDone(client));
    }

    public void refresh(ClientStatus clientStatus) {
        infoPanel.refresh(clientStatus);
        clientButtons.refresh(clientStatus);
        playPanel.refresh(clientStatus);
    }
}
