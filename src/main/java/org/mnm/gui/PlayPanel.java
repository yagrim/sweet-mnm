package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.client.RunnerOptions;
import org.mnm.config.Environment;
import org.mnm.config.VersionDetails;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.LoginListener;
import org.mnm.events.Refreshable;
import org.mnm.events.RepairListener;
import org.mnm.tools.PanicException;

import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.GuiComponents.setFontSize;
import static org.mnm.gui.MessageDialog.showErrorMessageDialogSync;
import static org.mnm.gui.Style.ACTION_BUTTON_FONT_SIZE;
import static org.mnm.gui.Style.BASE_FONT_SIZE;
import static org.mnm.gui.Style.SCALE;

public class PlayPanel extends JPanel
    implements LoginListener, RepairListener, Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(PlayPanel.class);

    final JButton play;

    public PlayPanel(
        GuiCommand.PlayAction runAction,
        Supplier<RunnerOptions> optionsSupplier) {
        this(runAction, optionsSupplier, 1f);
    }

    public PlayPanel(
        GuiCommand.PlayAction runAction,
        Supplier<RunnerOptions> optionsSupplier,
        float uiScaling) {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(2 * SCALE, 0, 0, 0));

        play = createButton("Play", uiScaling);
        play.setAlignmentX(Component.CENTER_ALIGNMENT);
        play.addActionListener(_ -> {
            try {
                runAction.run(optionsSupplier.get());
            } catch (PanicException e) {
                logger.error("", e);
                showErrorMessageDialogSync("Error: " + e.getMessage());
            }
        });
        this.add(play);
        this.add(new VersionPanel(uiScaling));

        ClientEventHandler.getInstance().register(this);
    }

    private static JButton createButton(String text, float uiScaling) {
        JButton button = new JButton(text);
        button.setEnabled(false);
        setFontSize(button, ACTION_BUTTON_FONT_SIZE * uiScaling);
        return button;
    }

    static class VersionPanel extends JPanel {

        private final JLabel label;

        VersionPanel(float uiScaling) {
            super(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            this.setBorder(BorderFactory.createEmptyBorder(0, SCALE, SCALE, SCALE));

            VersionDetails version = Environment.versionDetails();
            String text = "Sweet v%s (%s)".formatted(version.version(), version.gitSha());

            this.label = new JLabel(text);
            label.setForeground(Color.GRAY);
            GuiComponents.setFontSize(label, (float) (BASE_FONT_SIZE * 0.9 * uiScaling));
            this.add(label);
        }

        String getText() {
            return label.getText();
        }

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
        play.setEnabled(client != null && client.validToken() && client.statusIs(UPDATED));
    }

}
