package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridLayout;

import static org.mnm.config.Client.Status.NOT_INSTALLED;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.ClientPanel.SCALE;
import static org.mnm.gui.GuiComponents.setFontSize;

class ClientButtonsPanel extends JPanel
    implements LoginListener, RepairListener, Refreshable {

    private final JButton install;
    private final JButton repair;

    private final JButton login;
    private final JButton logout;

    private ClientStatus clientStatus;

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

    @Override
    public void repairStart() {
        install.setEnabled(false);
        repair.setEnabled(false);
        logout.setEnabled(false);
    }

    @Override
    public void repairDone(ClientStatus client) {
        this.clientStatus = client;
        refresh(clientStatus);
    }

    @Override
    public void loginStart() {
        login.setEnabled(false);
    }

    @Override
    public void loginDone(ClientStatus client) {
        clientStatus = client;
        refresh(clientStatus);
    }

    @Override
    public void logoutDone() {
        clientStatus = null;
        refresh(clientStatus);
    }

    @Override
    public void refresh(ClientStatus client) {
        this.clientStatus = client;

        boolean validToken = client.validToken();
        install.setEnabled(validToken && !client.statusIs(UPDATED));
        repair.setEnabled(validToken && !client.statusIs(NOT_INSTALLED) && (client.statusIs(UPDATED) || !client.clientUptoDate()));
        login.setEnabled(!validToken);
        logout.setEnabled(validToken);
    }

    public void disableAll() {
        install.setEnabled(false);
        repair.setEnabled(false);
        login.setEnabled(false);
        logout.setEnabled(false);
    }

}
