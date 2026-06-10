package org.mnm.gui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.client.RunnerOptions;
import org.mnm.tools.PanicException;

import static org.mnm.gui.GUI.DEFAULT_SLUG;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;
import static org.mnm.gui.MessageWindow.showInfoMessageDialogSync;
import static org.mnm.tools.StringUtils.isEmpty;

class ClientPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ClientPanel.class);

    private static final int SCALE = 8;

    private final JFrame parent;
    private ClientButtonsHandler buttonsHandler;
    private InfoPanel infoPanel;

    ClientPanel(JFrame parent) {
        this.parent = parent;
    }

    ClientButtonsHandler getButtonsHandler() {
        return buttonsHandler;
    }

    public InfoPanel getInfoPanel() {
        return infoPanel;
    }

    JPanel initialize(ClientStatus clientStatus,
                      GuiCommand.RepairAction repairAction,
                      GuiCommand.LoginAction loginAction,
                      GuiCommand.LogoutAction logoutAction,
                      GuiCommand.RunAction runAction,
                      BooleanSupplier inMemoryHashing,
                      Supplier<RunnerOptions> optionsSupplier) {

        this.setBorder(BorderFactory.createEmptyBorder(20, 20, 15, 20));

        final JButton installButton = new JButton("Install");
        final JButton repairButton = new JButton("Repair");
        final JButton playButton = new JButton("Play");
        final JButton loginButton = new JButton("Login");
        final JButton logoutButton = new JButton("Logout");

        final ClientButtonsHandler buttonsHandler = new ClientButtonsHandler(installButton, repairButton, playButton, loginButton, logoutButton);
        buttonsHandler.setClient(clientStatus.client(), clientStatus.clientUptoDate());
        buttonsHandler.setHasToken(clientStatus.validToken());
        buttonsHandler.disableAll();
        this.buttonsHandler = buttonsHandler;

        installButton.addActionListener(_ -> handleInstall(repairAction, inMemoryHashing));
        repairButton.addActionListener(_ -> handleRepair(repairAction, inMemoryHashing));
        playButton.addActionListener(_ -> runAction(runAction, optionsSupplier));
        loginButton.addActionListener(_ -> handleLogin(parent, loginAction));
        logoutButton.addActionListener(_ -> handleLogout(logoutAction));

        final JPanel clientPanel = new JPanel(new GridLayout(1, 2, SCALE, 0));
        clientPanel.add(loginButton);
        clientPanel.add(installButton);
        clientPanel.add(repairButton);
        clientPanel.add(logoutButton);

        final JPanel playRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        playRow.add(playButton);

        this.infoPanel = new InfoPanel(clientPanel.getPreferredSize().width, SCALE * 6, this.getBackground());

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(clientPanel);
        this.add(Box.createVerticalStrut(SCALE));
        this.add(infoPanel, BorderLayout.CENTER);
        this.add(Box.createVerticalStrut(SCALE * 3));
        this.add(playRow);

        return this;
    }

    private void handleInstall(GuiCommand.RepairAction installAction, BooleanSupplier inMemoryHashing) {
        buttonsHandler.installationStart();
        CompletableFuture
            .supplyAsync(() -> installAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean()))
            .whenComplete((client, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Installation completed");
                infoPanel.setText("""
                    Client is up-to-date
                    Token expires at: %s""".formatted(client.expiresAt()));
                buttonsHandler.installationDone(client.client());

            }));
    }

    private void handleRepair(GuiCommand.RepairAction repairAction, BooleanSupplier inMemoryHashing) {
        buttonsHandler.repairStart();
        CompletableFuture
            .supplyAsync(() -> repairAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean()))
            .whenComplete((client, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Repair completed");
                infoPanel.setText("""
                    Client is up-to-date
                    Token expires at: %s""".formatted(client.expiresAt()));
                buttonsHandler.repairDone(client.client());
            }));
    }

    private void handleLogin(JFrame parent, GuiCommand.LoginAction loginAction) {
        buttonsHandler.loginStart();

        final CredentialsPanel credentialsPanel = new CredentialsPanel();
        final int result = credentialsPanel.show(parent);

        if (result == JOptionPane.OK_OPTION && !isEmpty(credentialsPanel.getUsername()) && !isEmpty(credentialsPanel.getPassword())) {
            try {
                final ClientStatus client = loginAction.login(credentialsPanel.getUsername(), credentialsPanel.getPassword());
                infoPanel.setText("""
                    Successfully authenticated
                    Token expires at: %s""".formatted(client.expiresAt()));
                buttonsHandler.loginDone(client.client());
            } catch (Exception e) {
                buttonsHandler.refresh();
                logger.error("", e);
                showErrorMessageDialogSync("Error: " + e.getMessage());
            }
        } else {
            buttonsHandler.refresh();
        }
    }

    private void handleLogout(GuiCommand.LogoutAction logoutAction) {
        logoutAction.logout(DEFAULT_SLUG);
        infoPanel.setText("Token deleted");
        buttonsHandler.logoutDone();
    }

    private static void runAction(GuiCommand.RunAction runAction, Supplier<RunnerOptions> optionsSupplier) {
        try {
            RunnerOptions options = new RunnerOptions(DEFAULT_SLUG, null, false, optionsSupplier.get().enableMangoHud());
            runAction.run(options);
        } catch (PanicException e) {
            logger.error("", e);
            showErrorMessageDialogSync("Error: " + e.getMessage());
        }
    }

}
