package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.client.RunnerOptions;
import org.mnm.tools.PanicException;

import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.GuiComponents.setFontSize;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;

public class PlayPanel extends JPanel
    implements LoginListener, RepairListener, Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(PlayPanel.class);

    final JButton play;

    public PlayPanel(
        GuiCommand.PlayAction runAction,
        Supplier<RunnerOptions> optionsSupplier) {

        super(new FlowLayout(FlowLayout.CENTER, 0, 0));
        play = createButton("Play");
        play.addActionListener(_ -> {
            try {
                runAction.run(optionsSupplier.get());
            } catch (PanicException e) {
                logger.error("", e);
                showErrorMessageDialogSync("Error: " + e.getMessage());
            }
        });
        this.add(play);

        registerListeners();
    }

    private static JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setEnabled(false);
        setFontSize(button, 20f);
        return button;
    }

    private void registerListeners() {
        ClientEventHandler instance = ClientEventHandler.getInstance();
        instance.register((LoginListener) this);
        instance.register((RepairListener) this);
        instance.register((Refreshable) this);
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
        play.setEnabled(client.validToken() && client.statusIs(UPDATED));
    }

}
