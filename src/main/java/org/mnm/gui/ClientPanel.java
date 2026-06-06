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

    private final JFrame parent;
    private ClientButtonsHandler buttonsHandler;

    ClientPanel(JFrame parent) {
        this.parent = parent;
    }

    ClientButtonsHandler getButtonsHandler() {
        return buttonsHandler;
    }

    JPanel create(Client client,
                  boolean hasToken,
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
        buttonsHandler.setClient(client);
        buttonsHandler.setHasToken(hasToken);
        buttonsHandler.disableAll();
        this.buttonsHandler = buttonsHandler;

        installButton.addActionListener(_ -> handleInstall(buttonsHandler, repairAction, inMemoryHashing));
        repairButton.addActionListener(_ -> handleRepair(buttonsHandler, repairAction, inMemoryHashing));
        playButton.addActionListener(_ -> runAction(runAction, optionsSupplier));
        loginButton.addActionListener(_ -> handleLogin(buttonsHandler, parent, loginAction));
        logoutButton.addActionListener(_ -> handleLogout(buttonsHandler, logoutAction));

        final JPanel firstRow = new JPanel(new GridLayout(1, 2, 8, 0));
        firstRow.add(loginButton);
        firstRow.add(installButton);
        firstRow.add(repairButton);
        firstRow.add(logoutButton);

        final JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        secondRow.add(playButton);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(firstRow);
        this.add(Box.createVerticalStrut(8));
        this.add(Box.createVerticalStrut(8));
        this.add(secondRow);

        return this;
    }

    private static void handleInstall(ClientButtonsHandler buttons, GuiCommand.RepairAction installAction, BooleanSupplier inMemoryHashing) {
        buttons.installationStart();
        CompletableFuture
            .runAsync(() -> buttons.setClient(installAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean())))
            .whenComplete((_, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Installation completed");
                buttons.installationDone();
            }));
    }

    private static void handleRepair(ClientButtonsHandler buttons, GuiCommand.RepairAction repairAction, BooleanSupplier inMemoryHashing) {
        buttons.repairStart();
        CompletableFuture
            .runAsync(() -> buttons.setClient(repairAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean())))
            .whenComplete((_, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Repair completed");
                buttons.repairDone();
            }));
    }

    private static final Logger logger = LoggerFactory.getLogger(ClientPanel.class);

    private static void handleLogin(ClientButtonsHandler buttons, JFrame parent, GuiCommand.LoginAction loginAction) {
        buttons.loginStart();

        final CredentialsPanel credentialsPanel = new CredentialsPanel();
        final int result = credentialsPanel.show(parent);

        if (result == JOptionPane.OK_OPTION) {
            try {
                logger.debug("username: {}", credentialsPanel.getUsername());
                logger.debug("password: {}", credentialsPanel.getPassword());
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
