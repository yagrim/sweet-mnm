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

import org.mnm.client.RunnerOptions;
import org.mnm.config.Environment;
import org.mnm.config.VersionDetails;
import org.mnm.events.ClientEventHandler;
import org.mnm.events.LoginListener;
import org.mnm.events.Refreshable;
import org.mnm.events.RepairListener;

import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.GuiComponents.setFontSize;
import static org.mnm.gui.Style.ACTION_BUTTON_FONT_SIZE;
import static org.mnm.gui.Style.BASE_FONT_SIZE;
import static org.mnm.gui.Style.SCALE;

public class PlayPanel extends JPanel
    implements LoginListener, RepairListener, Refreshable {

    final JButton play;
    final VersionPanel versionPanel;
    // "Play" is disabled for the clients that run
    private final RunningClients runningClients = new RunningClients();
    // What the state of the client allows, without the running games
    private boolean playAllowed = false;
    private String currentSlug = null;

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
            final RunnerOptions options = optionsSupplier.get();
            final Process process = runAction.run(options);
            if (process != null && options != null) {
                runningClients.track(options.slug(), process);
            }
        });
        runningClients.addChangeListener(this::updatePlayEnabled);
        this.add(play);
        this.versionPanel = new VersionPanel(uiScaling);
        this.add(versionPanel);

        ClientEventHandler.getInstance().register(this);
    }

    private void setPlayAllowed(boolean allowed) {
        playAllowed = allowed;
        updatePlayEnabled();
    }

    private void updatePlayEnabled() {
        play.setEnabled(playAllowed && !runningClients.isRunning(currentSlug));
    }

    private static JButton createButton(String text, float uiScaling) {
        JButton button = new JButton(text);
        button.setEnabled(false);
        setFontSize(button, ACTION_BUTTON_FONT_SIZE * uiScaling);
        return button;
    }

    static class VersionPanel extends JPanel {

        private final JLabel label;
        private final String text;

        VersionPanel(float uiScaling) {
            super(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            this.setBorder(BorderFactory.createEmptyBorder(0, SCALE, SCALE, SCALE));

            VersionDetails version = Environment.versionDetails();
            this.text = "Sweet v%s (%s)".formatted(version.version(), version.gitSha());

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
        setPlayAllowed(false);
    }

    @Override
    public void loginDone(ClientStatus client) {
        refresh(client);
    }

    @Override
    public void logoutDone() {
        setPlayAllowed(false);
    }

    @Override
    public void repairStart() {
        setPlayAllowed(false);
    }

    @Override
    public void repairDone(ClientStatus client) {
        setPlayAllowed(true);
    }

    @Override
    public void refresh(ClientStatus client) {
        currentSlug = client == null || client.client() == null ? null : client.client().slug();
        setPlayAllowed(client != null && client.validToken() && client.statusIs(UPDATED));
    }

}
