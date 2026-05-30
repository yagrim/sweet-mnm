package org.mnm.gui;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.mnm.api.Session;
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
import org.mnm.config.Token;
import org.mnm.tools.JwtParser;

import static org.mnm.config.Client.Status.REPAIRING;
import static org.mnm.config.Environment.*;
import static org.mnm.gui.GUI.DEFAULT_SLUG;
import static org.mnm.gui.GuiComponents.setFontSize;
import static org.mnm.gui.MessageWindow.showInfoMessageDialogSync;

public class GuiCommand implements Command {

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

    // Validations run after initializing the UI
    @FunctionalInterface
    interface PostInitializationAction {
        void run(Client client, ClientButtonsHandler buttons);
    }

    private final Supplier<Path> configDbLocator;
    private final GuiStarter guiStarter;
    private final PostInitializationAction postInitAction;

    private final RunAction runAction;
    private final RepairAction repairAction;
    private final LoginAction loginAction;
    private final LogoutAction logoutAction;

    public GuiCommand() {
        this(new ConfigDbLocator());

        if (NATIVE_IMAGE) {
            // Workaround for AWT Native image error
            // Caused by: java.lang.Error: java.home property not set
            //	at java.desktop@25.0.2/sun.awt.FontConfiguration.findFontConfigFile(FontConfiguration.java:166)
            System.setProperty("java.home", "");
        }
    }

    GuiCommand(Supplier<Path> configDbLocator) {
        this.configDbLocator = configDbLocator;
        this.repairAction = slug -> repairClient(configDbLocator, slug);
        this.runAction = args -> runClient(configDbLocator, args);
        this.loginAction = (username, password) -> login(configDbLocator, username, password);
        this.logoutAction = slug -> logout(configDbLocator, slug);
        this.guiStarter = this::startSwingInterface;
        this.postInitAction = (client, buttons) -> postInitialization(configDbLocator, client, buttons);
    }

    GuiCommand(Supplier<Path> configDbLocator, GuiStarter guiStarter, PostInitializationAction postInitAction) {
        this.configDbLocator = configDbLocator;
        this.repairAction = slug -> repairClient(configDbLocator, slug);
        this.runAction = args -> runClient(configDbLocator, args);
        this.loginAction = (username, password) -> login(configDbLocator, username, password);
        this.logoutAction = slug -> logout(configDbLocator, slug);
        this.guiStarter = guiStarter;
        this.postInitAction = postInitAction;
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
            // For message windows
            UIManager.put("OptionPane.messageFont", new Font("Dialog", Font.PLAIN, 18));
            UIManager.put("OptionPane.buttonFont", new Font("Dialog", Font.PLAIN, 15));

            SwingUtilities.invokeAndWait(() -> {
                final JFrame frame = new JFrame("Sweet GUI");
                final Tabs tabs = createTabbedPanel(frame, client, hasToken, repairAction, loginAction, logoutAction, runAction);

                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(tabs.root(), BorderLayout.CENTER);
                frame.setResizable(false);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                postInitAction.run(client, tabs.clientPanel().getButtonsHandler());
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting the GUI", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to start the GUI", e.getCause());
        }
    }

    private void postInitialization(Supplier<Path> configDbLocator, Client client, ClientButtonsHandler buttons) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            if (client != null) {
                List<Token> tokens = configDb.getTokens(DEFAULT_SLUG);
                if (!tokens.isEmpty()) {
                    final String token = tokens.get(0).token();
                    if (JwtParser.parse(token).isExpired()) {
                        showInfoMessageDialogSync("Token expired: run Logout and Login");
                        buttons.refreshToken();
                    } else {
                        final Session session = Session.login(token, API_BASE_URL);
                        if (!session.getVersion().equals(client.version())) {
                            showInfoMessageDialogSync("Client update detected: run Install or Repair");
                        }
                    }
                }
            }
        }
    }

    static Tabs createTabbedPanel(JFrame frame, Client client, boolean hasToken,
                                  RepairAction repairAction,
                                  LoginAction loginAction, LogoutAction logoutAction, RunAction runAction) {

        final ClientPanel clientPanel = new ClientPanel(frame);
        final OptionsPanel optionsPanel = new OptionsPanel();

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Client", clientPanel.create(client, hasToken, repairAction, loginAction, logoutAction, runAction));
        tabs.addTab("Options", optionsPanel.create());
        setFontSize(tabs, 15f);
        return new Tabs(clientPanel, optionsPanel, tabs);
    }

    record Tabs(ClientPanel clientPanel, OptionsPanel optionsPanel, JTabbedPane root) {
    }

    private static Client repairClient(Supplier<Path> configDbLocator, String slug) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            final InstallerOptions options = OS.isWindows() ? InstallerOptions.forRepairWindows(slug) : InstallerOptions.forRepair(slug);
            new ClientInstaller(configDb)
                    .install(options, getWorkDir(), API_BASE_URL, REPAIRING);

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

}
