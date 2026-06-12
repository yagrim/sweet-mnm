package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.FlowLayout;

import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.GuiComponents.setFontSize;

public class PlayPanel extends JPanel
    implements LoginListener, RepairListener, Refreshable {

    final JButton play;

    public PlayPanel() {
        super(new FlowLayout(FlowLayout.CENTER, 0, 0));
        play = createButton("Play");
        this.add(play);
    }

    private static JButton createButton(String text) {
        JButton button = new JButton(text);
        setFontSize(button, 20f);
        return button;
    }

    @Override
    public void loginStart() {
        play.setEnabled(false);
    }

    @Override
    public void loginDone(ClientStatus client) {
        refresh(client);
    }

    @Override
    public void logoutDone() {
        play.setEnabled(false);
    }

    @Override
    public void repairStart() {
        play.setEnabled(false);
    }

    @Override
    public void repairDone(ClientStatus client) {
        play.setEnabled(true);
    }

    @Override
    public void refresh(ClientStatus client) {
        play.setEnabled(client.validToken()
            && client.clientUptoDate()
            && client.statusIs(UPDATED));
    }

}
