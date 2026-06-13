package org.mnm.gui;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import org.mnm.config.Client;

import static org.mnm.gui.MessageWindow.showInfoMessageDialogSync;

public class InfoPanel extends JPanel
    implements LoginListener, RepairListener, Refreshable {

    private final JTextPane textArea;

    public InfoPanel(int width, int height, Color color) {
        super(new FlowLayout(FlowLayout.CENTER, 0, 0));
        textArea = new JTextPane();
        GuiComponents.setFontSize(textArea, 15);
        textArea.setText("Checking data...");
        textArea.setEditable(false);
        textArea.setBackground(color);
        textArea.setBorder(new LineBorder(Color.GRAY, 1));
        textArea.setPreferredSize(new Dimension(width, height));

        StyledDocument doc = textArea.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        this.add(textArea);

        registerListeners();
    }

    private void registerListeners() {
        ClientEventHandler instance = ClientEventHandler.getInstance();
        instance.register((LoginListener) this);
        instance.register((RepairListener) this);
        instance.register((Refreshable) this);
    }

    public void setText(String text) {
        textArea.setText(text);
    }

    @Override
    public void loginStart() {
    }

    @Override
    public void loginDone(ClientStatus client) {
        this.setText("""
            Successfully authenticated
            Token expires at: %s""".formatted(client.expiresAt()));
    }

    @Override
    public void logoutDone() {
        this.setText("Token deleted");
    }

    @Override
    public void repairStart() {

    }

    @Override
    public void repairDone(ClientStatus client) {
        this.setText("""
            Client is up-to-date
            Token expires at: %s""".formatted(client.expiresAt()));
    }

    @Override
    public void refresh(ClientStatus client) {
        if (client.client() != null) {
            Client.Status status = client.client().status();
            if (status.isInProgress()) {
                String message = """
                    Last operation was interrupted: Re-run Install
                    Token expires at: %s""".formatted(client.expiresAt());
                this.setText(message);
            } else if (client.validToken()) {
                String message;
                if (!client.validToken()) {
                    message = "Token expired: run Logout, and then Login";
                    showInfoMessageDialogSync(message);
                } else if (!client.clientUptoDate()) {
                    message = "Client update detected: run Install or Repair";
                    showInfoMessageDialogSync(message);
                } else {
                    message = """
                        Client is up-to-date
                        Token expires at: %s""".formatted(client.expiresAt());
                }
                this.setText(message);
            }
        } else {
            this.setText(null);
        }
    }
}
