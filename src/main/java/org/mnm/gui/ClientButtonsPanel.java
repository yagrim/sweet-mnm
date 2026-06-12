package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.GridLayout;

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

    public void refresh(ClientStatus clientStatus) {
        this.clientStatus = clientStatus;

        boolean validToken = this.clientStatus.validToken();
        install.setEnabled(validToken && !clientStatus.statusIs(UPDATED));
        repair.setEnabled(validToken && !clientStatus.statusIs(NOT_INSTALLED) && (clientStatus.statusIs(UPDATED) || !clientStatus.clientUptoDate()));
        login.setEnabled(!validToken);
        logout.setEnabled(validToken);
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
