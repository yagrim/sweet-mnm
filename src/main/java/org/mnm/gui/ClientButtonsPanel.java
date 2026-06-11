package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridLayout;

import org.mnm.config.Client;

import static org.mnm.config.Client.Status.NOT_INSTALLED;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.ClientPanel.SCALE;
import static org.mnm.gui.GuiComponents.setFontSize;

class ClientButtonsPanel extends JPanel
    implements LoginListener, RepairListener {

    private final JButton install;
    private final JButton repair;

    private final JButton login;
    private final JButton logout;

    private ClientStatus client;
    private boolean hasToken;

    public ClientButtonsPanel() {
        super(new GridLayout(1, 2, SCALE, 0));

        install = createButton("Install");
        repair = createButton("Repair");
        login = createButton("Login");
        logout = createButton("Logout");

        this.add(login);
        this.add(install);
        this.add(repair);
        this.add(logout);

        disableAll();
    }

    private static JButton createButton(String text) {
        JButton button = new JButton(text);
        setFontSize(button, 20f);
        return button;
    }

    public void refresh() {
        boolean isCompleted = clientStatusIs(UPDATED);
        install.setEnabled(hasToken && !isCompleted);
        repair.setEnabled(hasToken && !clientStatusIs(NOT_INSTALLED) && (isCompleted || !clientIsUpTodDate()));
        login.setEnabled(!hasToken);
        logout.setEnabled(hasToken);
    }

    private boolean clientStatusIs(Client.Status status) {
        return client != null
            && client.client() != null
            && client.client().status() == status;
    }

    private boolean clientIsUpTodDate() {
        return client != null
            && client.client() != null
            && client.clientUptoDate();
    }

    @Override
    public void repairStart() {
        install.setEnabled(false);
        repair.setEnabled(false);
        logout.setEnabled(false);
    }

    @Override
    public void repairDone(ClientStatus client) {
        this.client = client;
        refresh();
    }

    @Override
    public void loginStart() {
        login.setEnabled(false);
    }

    @Override
    public void loginDone(ClientStatus client) {
        this.client = client;
        refresh();
    }

    @Override
    public void logoutDone() {
        hasToken = false;
        refresh();
    }

    public void refreshToken() {
        install.setEnabled(false);
        repair.setEnabled(false);
        logout.setEnabled(true);
    }

    public void enableAll() {
        install.setEnabled(true);
        repair.setEnabled(true);
        login.setEnabled(true);
        logout.setEnabled(true);
    }

    public void disableAll() {
        install.setEnabled(false);
        repair.setEnabled(false);
        login.setEnabled(false);
        logout.setEnabled(false);
    }

}
