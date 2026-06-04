package org.mnm.gui;

import javax.swing.*;

import org.mnm.config.Client;

import static org.mnm.gui.GuiComponents.setFontSize;

class ClientButtonsHandler {

    private final JButton install;
    private final JButton repair;
    private final JButton play;

    private final JButton login;
    private final JButton logout;

    private Client client;
    private boolean hasToken;

    public ClientButtonsHandler(JButton install, JButton repair, JButton play,
                                JButton login, JButton logout) {
        this.install = install;
        this.repair = repair;
        this.play = play;
        this.login = login;
        this.logout = logout;
        setAppearance();
    }

    public void setClient(Client client) {
        this.client = client;
        refresh();
    }

    public void setHasToken(boolean hasToken) {
        this.hasToken = hasToken;
        refresh();
    }

    private void setAppearance() {
        float fontSize = 20f;
        setFontSize(install, fontSize);
        setFontSize(repair, fontSize);
        setFontSize(play, fontSize);
        setFontSize(login, fontSize);
        setFontSize(logout, fontSize);
    }

    public void refresh() {
        boolean hasClient = hasClient();
        boolean isCompleted = client != null && client.status() == Client.Status.COMPLETED;
        install.setEnabled(hasClient && hasToken && !isCompleted);
        // && client status is completed
        repair.setEnabled(hasClient && hasToken && isCompleted);
        login.setEnabled(!hasToken);
        logout.setEnabled(hasToken);
        play.setEnabled(hasClient && hasToken);
    }

    private boolean hasClient() {
        return client != null;
    }

    void installationStart() {
        install.setEnabled(false);
        repair.setEnabled(false);
        logout.setEnabled(false);
        play.setEnabled(false);
    }

    void installationDone() {
        refresh();
    }

    void repairStart() {
        installationStart();
    }

    void repairDone() {
        refresh();
    }

    void loginStart() {
        login.setEnabled(false);
    }

    void loginDone(Client client) {
        setClient(client);
        hasToken = true;
        refresh();
    }

    void logoutDone() {
        hasToken = false;
        refresh();
    }

    public void refreshToken() {
        install.setEnabled(false);
        repair.setEnabled(false);
        play.setEnabled(false);
        logout.setEnabled(true);
    }

    public void disableAll() {
        install.setEnabled(false);
        repair.setEnabled(false);
        play.setEnabled(false);
        login.setEnabled(false);
        logout.setEnabled(false);
    }
}
