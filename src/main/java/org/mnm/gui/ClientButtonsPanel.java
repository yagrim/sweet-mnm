package org.mnm.gui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mnm.config.Client.Status.NOT_INSTALLED;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.ClientPanel.SCALE;
import static org.mnm.gui.GuiComponents.setFontSize;
import static org.mnm.gui.MainTabs.DEFAULT_SLUG;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;
import static org.mnm.gui.MessageWindow.showInfoMessageDialogSync;
import static org.mnm.tools.StringUtils.isEmpty;

class ClientButtonsPanel extends JPanel
    implements LoginListener, RepairListener, Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(ClientButtonsPanel.class);

    private final JButton install;
    private final JButton repair;

    private final JButton login;
    private final JButton logout;

    private ClientStatus clientStatus;

    public ClientButtonsPanel(
        JFrame mainWindow,
        GuiCommand.LoginAction loginAction,
        GuiCommand.LogoutAction logoutAction,
        GuiCommand.RepairAction repairAction, BooleanSupplier inMemoryHashing
    ) {

        super(new GridLayout(1, 2, SCALE, 0));

        install = createButton("Install");
        repair = createButton("Repair");
        login = createButton("Login");
        logout = createButton("Logout");

        this.add(login);
        this.add(install);
        this.add(repair);
        this.add(logout);

        this.login.addActionListener(e -> handleLogin(mainWindow, loginAction));
        this.logout.addActionListener(e -> handleLogout(logoutAction));
        this.repair.addActionListener(e -> handleRepair(repairAction, inMemoryHashing));

        registerListeners();
    }

    private void registerListeners() {
        ClientEventHandler instance = ClientEventHandler.getInstance();
        instance.register((LoginListener) this);
        instance.register((RepairListener) this);
        instance.register((Refreshable) this);
    }

    private static JButton createButton(String text) {
        JButton button = new JButton(text);
        setFontSize(button, 20f);
        button.setEnabled(false);
        return button;
    }

    @Override
    public void repairStart() {
        install.setEnabled(false);
        repair.setEnabled(false);
        logout.setEnabled(false);
    }

    @Override
    public void repairDone(ClientStatus client) {
        refresh(client);
    }

    @Override
    public void loginStart() {
        login.setEnabled(false);
    }

    @Override
    public void loginDone(ClientStatus client) {
        refresh(client);
    }

    @Override
    public void logoutDone() {
        refresh(null);
    }

    @Override
    public void refresh(ClientStatus client) {
        this.clientStatus = client;

        boolean validToken = client != null && client.validToken();
        install.setEnabled(validToken && !client.statusIs(UPDATED));
        repair.setEnabled(validToken && !client.statusIs(NOT_INSTALLED) && (client.statusIs(UPDATED) || !client.clientUptoDate()));
        login.setEnabled(!validToken);
        logout.setEnabled(validToken);
    }

    private void handleLogin(JFrame parent, GuiCommand.LoginAction loginAction) {
        ClientEventHandler eventHandler = ClientEventHandler.getInstance();

        eventHandler.loginStart();

        final CredentialsPanel credentialsPanel = new CredentialsPanel();
        final int result = credentialsPanel.show(parent);

        if (result == JOptionPane.OK_OPTION && !isEmpty(credentialsPanel.getUsername()) && !isEmpty(credentialsPanel.getPassword())) {
            try {
                final ClientStatus client = loginAction.login(credentialsPanel.getUsername(), credentialsPanel.getPassword());
                eventHandler.loginDone(client);
            } catch (Exception e) {
                eventHandler.refresh(clientStatus);
                logger.error("", e);
                showErrorMessageDialogSync("Error: " + e.getMessage());
            }
        } else {
            eventHandler.refresh(clientStatus);
        }
    }

    private void handleRepair(GuiCommand.RepairAction repairAction, BooleanSupplier inMemoryHashing) {
        ClientEventHandler.getInstance().repairStart();

        CompletableFuture
            .supplyAsync(() -> repairAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean()))
            .whenComplete((client, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Repair completed");
                ClientEventHandler.getInstance().repairDone(client);
            }));
    }

    private void handleLogout(GuiCommand.LogoutAction logoutAction) {
        logoutAction.logout(DEFAULT_SLUG);
        ClientEventHandler.getInstance().logoutDone();
    }

}
