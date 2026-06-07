package org.mnm.gui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.client.RunnerOptions;
import org.mnm.config.Client;
import org.mnm.tools.PanicException;

import static org.mnm.gui.GUI.DEFAULT_SLUG;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;
import static org.mnm.gui.MessageWindow.showInfoMessageDialogSync;

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

    JPanel create(GuiCommand.ClientStatus clientStatus,
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

        installButton.addActionListener(_ -> handleInstall(buttonsHandler, repairAction, inMemoryHashing));
        repairButton.addActionListener(_ -> handleRepair(buttonsHandler, repairAction, inMemoryHashing));
        playButton.addActionListener(_ -> runAction(runAction, optionsSupplier));
        loginButton.addActionListener(_ -> handleLogin(buttonsHandler, parent, loginAction));
        logoutButton.addActionListener(_ -> handleLogout(buttonsHandler, logoutAction));

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

    private static void handleInstall(ClientButtonsHandler buttons, GuiCommand.RepairAction installAction, BooleanSupplier inMemoryHashing) {
        buttons.installationStart();
        CompletableFuture
            .supplyAsync(() -> installAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean()))
            .whenComplete((client, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Installation completed");
                buttons.installationDone(client);
            }));
    }

    private static void handleRepair(ClientButtonsHandler buttons, GuiCommand.RepairAction repairAction, BooleanSupplier inMemoryHashing) {
        buttons.repairStart();
        CompletableFuture
            .supplyAsync(() -> repairAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean()))
            .whenComplete((client, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Repair completed");
                buttons.repairDone(client);
            }));
    }

    private static void handleLogin(ClientButtonsHandler buttons, JFrame parent, GuiCommand.LoginAction loginAction) {
        buttons.loginStart();

        final CredentialsPanel credentialsPanel = new CredentialsPanel();
        final int result = credentialsPanel.show(parent);

        if (result == JOptionPane.OK_OPTION) {
            try {
                final Client client = loginAction.login(credentialsPanel.getUsername(), credentialsPanel.getPassword());
                buttons.loginDone(client);
            } catch (Exception e) {
                buttons.refresh();
                e.printStackTrace();
                showErrorMessageDialogSync("Error: " + e.getMessage());
            }
        }
    }

    private static void handleLogout(ClientButtonsHandler buttons, GuiCommand.LogoutAction logoutAction) {
        logoutAction.logout(DEFAULT_SLUG);
        buttons.logoutDone();
    }

    private static void runAction(GuiCommand.RunAction runAction, Supplier<RunnerOptions> optionsSupplier) {
        try {
            RunnerOptions options = new RunnerOptions(DEFAULT_SLUG, null, false, optionsSupplier.get().enableMangoHud());
            runAction.run(options);
        } catch (PanicException e) {
            e.printStackTrace();
            showErrorMessageDialogSync("Error: " + e.getMessage());
        }
    }

}
