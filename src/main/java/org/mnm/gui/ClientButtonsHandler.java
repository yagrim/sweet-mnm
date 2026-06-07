package org.mnm.gui;

import javax.swing.*;

import org.mnm.config.Client;

import static org.mnm.config.Client.Status.NOT_INSTALLED;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.GuiComponents.setFontSize;

class ClientButtonsHandler {

    private final JButton install;
    private final JButton repair;
    private final JButton play;

    private final JButton login;
    private final JButton logout;

    private Client client;
    private boolean upToDate;
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

    // Used for initialization, no need to refresh
    public void setClient(Client client, boolean upToDate) {
        this.client = client;
        this.upToDate = upToDate;
    }

    // Used for initialization, no need to refresh
    public void setHasToken(boolean hasToken) {
        this.hasToken = hasToken;
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
        boolean isCompleted = clientStatusIs(UPDATED);
        install.setEnabled(hasToken && !isCompleted);
        repair.setEnabled(hasToken && !clientStatusIs(NOT_INSTALLED) && (isCompleted || !upToDate));
        login.setEnabled(!hasToken);
        logout.setEnabled(hasToken);
        play.setEnabled(hasToken && upToDate && isCompleted);
    }

    private boolean clientStatusIs(Client.Status status) {
        return client != null && client.status() == status;
    }

    void installationStart() {
        install.setEnabled(false);
        repair.setEnabled(false);
        logout.setEnabled(false);
        play.setEnabled(false);
    }

    void installationDone(Client client) {
        this.client = client;
        this.upToDate = true;
        refresh();
    }

    void repairStart() {
        installationStart();
    }

    void repairDone(Client client) {
        this.client = client;
        this.upToDate = true;
        refresh();
    }

    void loginStart() {
        login.setEnabled(false);
    }

    void loginDone(Client client) {
        this.client = client;
        // TODO we could use client.status to set upToDate, during login operation we have checked the version
        this.upToDate = false;
        this.hasToken = true;
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
