package org.mnm.gui;

import javax.swing.*;
import java.awt.*;

import static org.mnm.gui.GuiComponents.setFontSize;

public class PlayPanel extends JPanel
    implements LoginListener, RepairListener {

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
        // TODO maybe we can assume true? depends on when is this invoked
        play.setEnabled(true);
//        play.setEnabled(client.validToken()
//            && client.clientUptoDate()
//            && (client.client() != null && client.client().status() == UPDATED));
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

    public void refresh() {
//        play.setEnabled(hasToken && upToDate && isCompleted);
    }

    public void refreshToken() {
//        play.setEnabled(false);
    }

    public void disableAll() {
//        play.setEnabled(false);
    }

}
