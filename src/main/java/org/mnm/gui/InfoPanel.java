package org.mnm.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Dimension;

import org.mnm.config.Client;
import org.mnm.config.SplitVersion;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.LoginListener;
import org.mnm.events.Refreshable;
import org.mnm.events.RepairListener;

import static org.mnm.config.Client.Status.NEEDS_UPDATE;
import static org.mnm.gui.ClientPanel.SCALE;
import static org.mnm.gui.MessageWindow.showInfoMessageDialogSync;

public class InfoPanel extends JPanel
    implements LoginListener, RepairListener, Refreshable {

    private final JTextPane textArea;
    private final JLabel versionLabel;

    public InfoPanel(int width, int height, Color color) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        textArea = new JTextPane();
        GuiComponents.setFontSize(textArea, 15);
        textArea.setText("Checking data...");
        textArea.setEditable(false);
        textArea.setBackground(color);
        textArea.setBorder(new LineBorder(Color.GRAY, 1));
        textArea.setPreferredSize(new Dimension(width, height));

        versionLabel = new JLabel(" ", SwingConstants.RIGHT);
        GuiComponents.setFontSize(versionLabel, 15);
        versionLabel.setMaximumSize(new Dimension(width, versionLabel.getPreferredSize().height));

        StyledDocument doc = textArea.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        this.add(Box.createVerticalGlue());
        this.add(textArea);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(versionLabel);
        this.add(Box.createVerticalGlue());

        ClientEventHandler.getInstance().register(this);
    }

    @Override
    public void loginStart() {
    }

    @Override
    public void loginDone(ClientStatus client) {
        this.updateText("""
            Successfully authenticated
            Token expires at: %s""".formatted(client.expiresAt()));

        updateVersion(client);
    }

    @Override
    public void logoutDone() {
        this.updateText("Token deleted");
    }

    @Override
    public void repairStart() {

    }

    @Override
    public void repairDone(ClientStatus client) {
        this.updateText("""
            Client is up-to-date
            Token expires at: %s""".formatted(client.expiresAt()));

        updateVersion(client);
    }

    @Override
    public void refresh(ClientStatus client) {
        if (client.client() != null) {
            Client.Status status = client.client().status();
            if (status.isInProgress()) {
                String message = """
                    Last operation was interrupted: Re-run Install
                    Token expires at: %s""".formatted(client.expiresAt());
                this.updateText(message);
            } else if (client.validToken()) {
                String message;
                if (!client.validToken()) {
                    message = "Token expired: run Logout, and then Login";
                    showInfoMessageDialogSync(message);
                } else if (client.statusIs(NEEDS_UPDATE)) {
                    message = "Client update detected: run Repair";
                    showInfoMessageDialogSync(message);
                } else {
                    message = """
                        Client is up-to-date
                        Token expires at: %s""".formatted(client.expiresAt());
                }
                this.updateText(message);
            }
            updateVersion(client);
        } else {
            this.updateText(null);
        }
    }

    private void updateText(String text) {
        textArea.setText(text);
    }

    private void updateVersion(ClientStatus client) {
        SplitVersion splitVersion = client.client().getSplitVersion();
        versionLabel.setText("v%s (%s, %s)".formatted(splitVersion.getSemver(), splitVersion.getPrefix(), splitVersion.getShortSha()));
        versionLabel.setToolTipText("Installed version");
    }
}
