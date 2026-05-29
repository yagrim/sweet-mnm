package org.mnm.gui;

import javax.swing.*;

import static org.mnm.gui.GuiComponents.setFontSize;

// TODO Handle token expired
class ClientButtonsHandler {

    private final JButton install;
    private final JButton repair;
    private final JButton play;
    private final JButton logout;

    private boolean hasClient;
    private boolean hasToken;

    public ClientButtonsHandler(JButton install, JButton repair, JButton play, JButton logout) {
        this.install = install;
        this.repair = repair;
        this.play = play;
        this.logout = logout;
        setAppearance();
    }

    public void setHasClient(boolean hasClient) {
        this.hasClient = hasClient;
        refresh();
    }

    public void setHasToken(boolean hasToken) {
        this.hasToken = hasToken;
        refresh();
    }

    public void refresh() {
        install.setEnabled(!hasClient || !hasToken);
        repair.setEnabled(hasClient && hasToken);
        logout.setEnabled(hasToken);
        play.setEnabled(hasClient && hasToken);
    }

    void installationStart() {
        install.setEnabled(false);
    }

    void installationDone() {
        install.setEnabled(true);
    }

    void repairStart() {
        repair.setEnabled(false);
    }

    void repairDone() {
        repair.setEnabled(true);
    }

    void logout() {
        hasToken = false;
        refresh();
    }

    private void setAppearance() {
        float fontSize = 20f;
        setFontSize(install, fontSize);
        setFontSize(repair, fontSize);
        setFontSize(play, fontSize);
        setFontSize(logout, fontSize);
    }

}
