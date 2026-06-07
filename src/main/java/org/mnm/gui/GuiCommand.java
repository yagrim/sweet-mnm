package org.mnm.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.mnm.tools.ProcessUtils;

import static org.mnm.config.Client.Status.REPAIRING;
import static org.mnm.config.Environment.*;
import static org.mnm.gui.ClientStatus.getClientStatus;
import static org.mnm.gui.GUI.createTabbedPanel;
import static org.mnm.gui.MessageWindow.showInfoMessageDialogSync;
import static org.mnm.tools.FileUtils.installClasspathResource;

public class GuiCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(GuiCommand.class);

    // https://docs.oracle.com/javase/8/docs/technotes/guides/intl/fontconfig.html
    private static final String FONTCONFIG_PROTON = "fontconfig-proton.properties";

    @FunctionalInterface
    interface GuiStarter {
        void start(ClientStatus clientStatus);
    }

    @FunctionalInterface
    interface RepairAction {
        Client repair(String slug, boolean inMemoryHashing);
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
        void run(RunnerOptions options);
    }

    // Validations run after initializing the UI
    @FunctionalInterface
    interface PostInitializationAction {
        void run(ClientStatus clientStatus, ClientButtonsHandler buttons, InfoPanel infoPanel);
    }

    private final Supplier<Path> configDbLocator;
    private final GuiStarter guiStarter;
    private final PostInitializationAction postInitAction;

    private final RunAction runAction;
    private final RepairAction repairAction;
    private final LoginAction loginAction;
    private final LogoutAction logoutAction;

    private JFrame frame;

    public GuiCommand() {
        this(new ConfigDbLocator());
    }

    GuiCommand(Supplier<Path> configDbLocator) {
        this.configDbLocator = configDbLocator;
        this.repairAction = (slug, inMemoryHashing) -> repairClient(configDbLocator, slug, inMemoryHashing);
        this.runAction = (options) -> runClient(configDbLocator, options);
        this.loginAction = (username, password) -> login(configDbLocator, username, password);
        this.logoutAction = slug -> logout(configDbLocator, slug);
        this.guiStarter = this::startSwingInterface;
        this.postInitAction = (clientStatus, buttons, intoPanel) -> postInitialization(clientStatus, buttons, intoPanel);
    }

    GuiCommand(Supplier<Path> configDbLocator, GuiStarter guiStarter, PostInitializationAction postInitAction) {
        this.configDbLocator = configDbLocator;
        this.repairAction = (slug, inMemoryHashing) -> repairClient(configDbLocator, slug, inMemoryHashing);
        this.runAction = (options) -> runClient(configDbLocator, options);
        this.loginAction = (username, password) -> login(configDbLocator, username, password);
        this.logoutAction = slug -> logout(configDbLocator, slug);
        this.guiStarter = guiStarter;
        this.postInitAction = postInitAction;
    }

    @Override
    public void run(Arguments args) {
        initialize();
        guiStarter.start(getClientStatus(configDbLocator.get()));
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


    // Commands should not initialize in constructor because debug is configured after it
    private static void initialize() {
        logger.debug("Runtime: native_image:={},windows={}", NATIVE_IMAGE, OS.isWindows());
        if (NATIVE_IMAGE) {
            System.setProperty("java.home", "");
            if (OS.isWindows()) {
                final Path fontconfig = getWorkDir().resolve(FONTCONFIG_PROTON).toAbsolutePath();
                if (!fontconfig.toFile().exists()) {
                    ProcessUtils.panic(FONTCONFIG_PROTON + " not found");
                }
                logger.debug("Setting sun.awt.fontconfig: {}", fontconfig);
                System.setProperty("sun.awt.fontconfig", fontconfig.toString());
            }
        } else {
            final Path fontconfig = installClasspathResource("distribution/" + FONTCONFIG_PROTON);
            logger.debug("Installing fontconfig into: {}", fontconfig);
            System.setProperty("sun.awt.fontconfig", fontconfig.toString());
        }
    }

    private void startSwingInterface(ClientStatus clientStatus) {
        try {
            // For message windows
            UIManager.put("OptionPane.messageFont", new Font("Dialog", Font.PLAIN, 18));
            UIManager.put("OptionPane.buttonFont", new Font("Dialog", Font.PLAIN, 15));

            SwingUtilities.invokeAndWait(() -> {
                this.frame = new JFrame("Sweet GUI");
                final Tabs tabs = createTabbedPanel(frame, clientStatus, loginAction, logoutAction, repairAction, runAction);

                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(tabs.root(), BorderLayout.CENTER);
                frame.setResizable(false);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                // Prevent UI locking for some seconds
                CompletableFuture
                    .runAsync(() -> {
                        ClientPanel clientPanel = tabs.clientPanel();
                        postInitAction.run(clientStatus, clientPanel.getButtonsHandler(), clientPanel.getInfoPanel());
                    })
                    .whenComplete((_, _) -> SwingUtilities.invokeLater(() -> {
                        tabs.clientPanel.getButtonsHandler().refresh();
                    }));
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting the GUI", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to start the GUI", e.getCause());
        }
    }

    void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    private void postInitialization(ClientStatus clientStatus, ClientButtonsHandler buttons, InfoPanel infoPanel) {

        if (clientStatus.client() != null) {
            logger.debug("No client found in config db");
            Client.Status status = clientStatus.client().status();
            if (status.isInProgress()) {
                String message = """
                    Last operation was interrupted: Re-run Install
                    Token expires at: %s""".formatted(clientStatus.expiresAt());
                infoPanel.setText(message);
            } else if (clientStatus.validToken()) {
                String message;
                if (!clientStatus.validToken()) {
                    message = "Token expired: run Logout, and then Login";
                    showInfoMessageDialogSync(message);
                    buttons.refreshToken();
                } else if (!clientStatus.clientUptoDate()) {
                    message = "Client update detected: run Install or Repair";
                    showInfoMessageDialogSync(message);
                } else {
                    message = """
                        Client is up-to-date
                        Token expires at: %s""".formatted(clientStatus.expiresAt());
                }
                infoPanel.setText(message);
            }
        }
    }

    record Tabs(ClientPanel clientPanel, OptionsPanel optionsPanel, JTabbedPane root) {
    }

    private static Client repairClient(Supplier<Path> configDbLocator, String slug, boolean inMemoryHashing) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            final InstallerOptions options = InstallerOptions.forRepair(slug, inMemoryHashing);
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

    private static void runClient(Supplier<Path> configDbLocator, RunnerOptions options) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            new ClientRunner(configDb)
                .run(options);
        }
    }

}
