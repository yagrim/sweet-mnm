package org.mnm.gui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.client.ClientInstaller;
import org.mnm.client.ClientRunner;
import org.mnm.client.InstallerOptions;
import org.mnm.client.LoginService;
import org.mnm.client.RunnerOptions;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.ConfigDbLocator;
import org.mnm.config.OS;
import org.mnm.tools.PanicException;

import static org.mnm.GeneralOptions.toggleDebug;
import static org.mnm.config.Client.Status.INSTALLING;
import static org.mnm.config.Client.Status.REPAIRING;
import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.config.Environment.getWorkDir;
import static org.mnm.gui.GuiComponents.setFontSize;

public class GuiCommand implements Command {

    private static final String DEFAULT_SLUG = "mnm";

    @FunctionalInterface
    interface GuiStarter {
        void start(Client client, boolean hasToken);
    }

    @FunctionalInterface
    interface RepairAction {
        Client repair(String slug);
    }

    @FunctionalInterface
    interface LoginAction {
        Client login(String username, String password);
    }

    @FunctionalInterface
    interface LogoutAction {
        void logout(String slug);
    }

    @FunctionalInterface
    interface RunAction {
        void run(Arguments args);
    }

    private final Supplier<Path> configDbLocator;
    private final GuiStarter guiStarter;

    private final RunAction runAction;
    private final RepairAction repairAction;
    private final LoginAction loginAction;
    private final LogoutAction logoutAction;

    public GuiCommand() {
        this(new ConfigDbLocator());
    }

    // TODO handle Panic popup, "can't here Cannot call invokeAndWait from the event dispatcher thread"
    // } catch (PanicException e) {
    //  showMessageDialogSync("Error: " + e.getMessage());
    // }
    GuiCommand(Supplier<Path> configDbLocator) {
        this.configDbLocator = configDbLocator;
        this.repairAction = slug -> repairClient(configDbLocator, slug);
        this.runAction = args -> runClient(configDbLocator, args);
        this.loginAction = (username, password) -> login(configDbLocator, username, password);
        this.logoutAction = slug -> logout(configDbLocator, slug);

        this.guiStarter = this::startSwingInterface;
    }

    GuiCommand(Supplier<Path> configDbLocator, GuiStarter guiStarter) {
        this.configDbLocator = configDbLocator;
        this.guiStarter = guiStarter;
        this.repairAction = slug -> repairClient(configDbLocator, slug);
        this.runAction = args -> runClient(configDbLocator, args);
        this.loginAction = (username, password) -> login(configDbLocator, username, password);
        this.logoutAction = slug -> logout(configDbLocator, slug);
    }

    @Override
    public void run(Arguments args) {
        guiStarter.start(getClient(), hasAvailableToken());
    }

    @Override
    public String name() {
        return "gui";
    }

    @Override
    public String description() {
        return "Opens a simple Swing interface";
    }

    @Override
    public String help() {
        return """
            %s
            
            Usage:
              sweet %s
            
            Options:
              --debug  Enables debug messages
              --help   Shows this help
            """.formatted(description(), name());
    }

    private Client getClient() {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            return configDb.getClient(DEFAULT_SLUG);
        }
    }

    private boolean hasAvailableToken() {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            return !configDb.getTokens(DEFAULT_SLUG).isEmpty();
        }
    }

    private void startSwingInterface(Client client, boolean hasToken) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                final JFrame frame = new JFrame("Sweet GUI");
                final JTabbedPane tabs = createTabbedPanel(frame, client, hasToken, repairAction, loginAction, logoutAction, runAction);

                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(tabs, BorderLayout.CENTER);
                frame.setResizable(false);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting the GUI", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to start the GUI", e.getCause());
        }
    }

    static JPanel createButtonsPanel(JFrame frame, Client client, boolean hasToken) {
        return createButtonsPanel(frame, client, hasToken,
            _ -> null,
            (_, _) -> null,
            _ -> {
            });
    }

    static JPanel createButtonsPanel(JFrame frame, Client client, boolean hasToken,
                                     RepairAction repairAction, LoginAction loginAction, LogoutAction logoutAction) {
        return createButtonsPanel(frame, client, hasToken, repairAction, loginAction, logoutAction, args -> {
        });
    }

    static JPanel createButtonsPanel(JFrame frame, Client client, boolean hasToken,
                                     RepairAction repairAction,
                                     LoginAction loginAction,
                                     LogoutAction logoutAction,
                                     RunAction runAction
    ) {
        final JButton installButton = new JButton("Install");
        final JButton repairButton = new JButton("Repair");
        final JButton playButton = new JButton("Play");
        final JButton loginButton = new JButton("Login");
        final JButton logoutButton = new JButton("Logout");

        final ClientButtonsHandler buttonsHandler = new ClientButtonsHandler(installButton, repairButton, playButton, loginButton, logoutButton);
        buttonsHandler.setClient(client);
        buttonsHandler.setHasToken(hasToken);
        buttonsHandler.refresh();

        installButton.addActionListener(_ -> handleInstall(buttonsHandler, repairAction));
        repairButton.addActionListener(_ -> handleRepair(buttonsHandler, repairAction));
        playButton.addActionListener(_ -> runAction(runAction));
        loginButton.addActionListener(_ -> handleLogin(buttonsHandler, frame, loginAction));
        logoutButton.addActionListener(_ -> handleLogout(buttonsHandler, logoutAction));

        final JPanel firstRow = new JPanel(new GridLayout(1, 2, 8, 0));
        firstRow.add(loginButton);
        firstRow.add(installButton);
        firstRow.add(repairButton);
        firstRow.add(logoutButton);

        final JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        secondRow.add(playButton);

        final JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.add(firstRow);
        buttons.add(Box.createVerticalStrut(8));
        buttons.add(secondRow);
        buttons.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return buttons;
    }

    static JTabbedPane createTabbedPanel(JFrame frame, Client client, boolean hasToken,
                                         RepairAction repairAction,
                                         LoginAction loginAction, LogoutAction logoutAction, RunAction runAction) {

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Client", createButtonsPanel(frame, client, hasToken, repairAction, loginAction, logoutAction, runAction));
        tabs.addTab("Options", createOptionsPanel());
        setFontSize(tabs, 15f);
        return tabs;
    }

    static JPanel createOptionsPanel() {
        final JCheckBox debugOption = new JCheckBox("Enable debug");
        debugOption.setActionCommand("debug");
        debugOption.addActionListener(_ -> toggleDebug(debugOption.isSelected()));

        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.add(debugOption);
        return panel;
    }

    private static void handleInstall(ClientButtonsHandler buttons, RepairAction installAction) {
        buttons.installationStart();
        CompletableFuture
            .runAsync(() -> buttons.setClient(installAction.repair(DEFAULT_SLUG)))
            .whenComplete((_, _) -> SwingUtilities.invokeLater(() -> {
                buttons.installationDone();
            }));
    }

    private static void handleRepair(ClientButtonsHandler buttons, RepairAction repairAction) {
        buttons.repairStart();
        CompletableFuture
            .runAsync(() -> buttons.setClient(repairAction.repair(DEFAULT_SLUG)))
            .whenComplete((_, _) -> SwingUtilities.invokeLater(() -> {
                buttons.repairDone();
            }));
    }

    private static void handleLogin(ClientButtonsHandler buttons, JFrame parent, LoginAction loginAction) {
        buttons.loginStart();

        final CredentialsPanel credentialsPanel = new CredentialsPanel();
        final int result = credentialsPanel.show(parent);

        if (result == JOptionPane.OK_OPTION) {
            try {
                final Client client = loginAction.login(credentialsPanel.getUsername(), credentialsPanel.getPassword());
                buttons.loginDone(client);
            } catch (Exception e) {
                buttons.refresh();
                showMessageDialogSync("Error: " + e.getMessage(), JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private static void handleLogout(ClientButtonsHandler buttons, LogoutAction logoutAction) {
        logoutAction.logout(DEFAULT_SLUG);
        buttons.logoutDone();
    }

    private static void runAction(RunAction runAction) {
        try {
            runAction.run(Arguments.parse("--slug", DEFAULT_SLUG));
        } catch (PanicException e) {
            showMessageDialogSync("Error: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Client installClient(Supplier<Path> configDbLocator, String slug) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            final InstallerOptions options = OS.isWindows() ? InstallerOptions.forRepairWindows(slug) : InstallerOptions.forRepair(slug);
            new ClientInstaller(configDb)
                .install(options, getWorkDir(), API_BASE_URL, INSTALLING);

            showMessageDialogSync("Installation completed !!", JOptionPane.INFORMATION_MESSAGE);
            return configDb.getClient(slug);
        }
    }

    private static Client repairClient(Supplier<Path> configDbLocator, String slug) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            final InstallerOptions options = OS.isWindows() ? InstallerOptions.forRepairWindows(slug) : InstallerOptions.forRepair(slug);
            new ClientInstaller(configDb)
                .install(options, getWorkDir(), API_BASE_URL, REPAIRING);

            showMessageDialogSync("Repair completed !!", JOptionPane.INFORMATION_MESSAGE);
            return configDb.getClient(slug);
        }
    }

    private static Client login(Supplier<Path> configDbLocator, String username, String password) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            String slug = new LoginService(configDb)
                .login(username, password, getWorkDir(), API_BASE_URL);

            return configDb.getClient(slug);
        }
    }

    private static void logout(Supplier<Path> configDbLocator, String slug) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            configDb.deleteTokens(slug);
        }
    }

    private static void runClient(Supplier<Path> configDbLocator, Arguments args) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            new ClientRunner(configDb)
                .run(RunnerOptions.parse(args));
        }
    }

    private static void showMessageDialogSync(String message, int type) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                String title = type == JOptionPane.ERROR_MESSAGE ? "Error" : "Info";
                SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(null, message, title, type));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while showing dialog", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to show dialog", e.getCause());
        }
    }

}
