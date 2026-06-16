package org.mnm.gui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.mnm.config.VersionDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.cli.DevFlags;
import org.mnm.client.ClientInstaller;
import org.mnm.client.ClientRunner;
import org.mnm.client.InstallerOptions;
import org.mnm.client.LoginService;
import org.mnm.client.RunnerOptions;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;
import org.mnm.config.ConfigDbLocator;
import org.mnm.config.Environment;
import org.mnm.config.OS;
import org.mnm.tools.JwtParser;
import org.mnm.tools.ProcessUtils;

import static org.mnm.config.Environment.API_BASE_URL;
import static org.mnm.config.Environment.NATIVE_IMAGE;
import static org.mnm.config.Environment.getWorkDir;
import static org.mnm.gui.ClientStatus.getClientStatus;
import static org.mnm.gui.MainTabs.DEFAULT_SLUG;
import static org.mnm.tools.FileUtils.installClasspathResource;

public class GuiCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(GuiCommand.class);

    // https://docs.oracle.com/javase/8/docs/technotes/guides/intl/fontconfig.html
    private static final String FONTCONFIG_PROTON = "fontconfig-proton.properties";

    @FunctionalInterface
    interface GuiStarter {
        void start(Supplier<ClientStatus> clientStatus);
    }

    @FunctionalInterface
    interface RepairAction {
        ClientStatus repair(String slug, Client.Status status, boolean inMemoryHashing);
    }

    @FunctionalInterface
    interface LoginAction {
        ClientStatus login(String username, String password);
    }

    @FunctionalInterface
    interface LogoutAction {
        void logout(String slug);
    }

    @FunctionalInterface
    interface PlayAction {
        void run(RunnerOptions options);
    }

    // Validations run after initializing the UI
    @FunctionalInterface
    interface PostInitializationAction {
        void run(ClientStatus clientStatus);
    }

    private final Supplier<Path> configDbLocator;
    private final GuiStarter guiStarter;
    private final PostInitializationAction postInitAction;

    private final PlayAction runAction;
    private final RepairAction repairAction;
    private final LoginAction loginAction;
    private final LogoutAction logoutAction;

    private JFrame frame;

    public GuiCommand() {
        this(new ConfigDbLocator());
    }

    // NOTE: we wrap logic in Actions to keep ConfigDB handling here
    GuiCommand(Supplier<Path> configDbLocator) {
        this.configDbLocator = configDbLocator;
        this.repairAction = (slug, status, inMemoryHashing) -> repairClient(configDbLocator, slug, status, inMemoryHashing);
        this.runAction = (options) -> runClient(configDbLocator, options);
        this.loginAction = (username, password) -> login(configDbLocator, username, password);
        this.logoutAction = slug -> logout(configDbLocator, slug);
        this.guiStarter = this::startSwingInterface;
        this.postInitAction = (clientStatus) -> postInitializeSwing(clientStatus);
    }

    @Override
    public void run(Arguments args) {
        final DevFlags devFlags = DevFlags.parse(args);
        final String apiEndpoint = devFlags.enabled() ? devFlags.apiEndpoint() : API_BASE_URL;

        initialize();
        guiStarter.start(() -> getClientStatus(DEFAULT_SLUG, configDbLocator.get(), apiEndpoint));
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
        VersionDetails versionDetails = Environment.versionDetails();
        logger.info("Launcher version: {} ({})", versionDetails.version(), versionDetails.gitSha());
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

    private void startSwingInterface(Supplier<ClientStatus> clientStatusSupplier) {
        try {
            // For message windows
            UIManager.put("OptionPane.messageFont", new Font("Dialog", Font.PLAIN, 18));
            UIManager.put("OptionPane.buttonFont", new Font("Dialog", Font.PLAIN, 15));

            SwingUtilities.invokeAndWait(() -> {
                this.frame = new JFrame("Sweet GUI");

                final MainTabs tabs = new MainTabs(frame, loginAction, logoutAction, repairAction, runAction);

                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(tabs, BorderLayout.CENTER);
                frame.setResizable(false);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                // Prevent UI locking for some seconds
                CompletableFuture
                    .supplyAsync(() -> clientStatusSupplier.get())
                    .whenComplete((clientStatus, _) -> SwingUtilities.invokeLater(() -> {
                        postInitAction.run(clientStatus);
                    }));
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting the GUI", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to start the GUI", e.getCause());
        }
    }

    static void postInitializeSwing(ClientStatus clientStatus) {
        if (clientStatus.client() == null) {
            logger.debug("No client found in config db");
        }
        ClientEventHandler.getInstance().refresh(clientStatus);
    }

    void close() {
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    private static ClientStatus repairClient(Supplier<Path> configDbLocator, String slug, Client.Status status, boolean inMemoryHashing) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            final InstallerOptions options = InstallerOptions.forRepair(slug, inMemoryHashing);
            new ClientInstaller(configDb)
                .install(options, getWorkDir(), API_BASE_URL, status);

            return buildClientStatus(configDb, slug);
        }
    }

    private static ClientStatus login(Supplier<Path> configDbLocator, String username, String password) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            String slug = new LoginService(configDb)
                .login(username, password, getWorkDir(), API_BASE_URL);

            return buildClientStatus(configDb, slug);
        }
    }

    /**
     * Returns data with the most up-to-date information from DB.
     */
    private static ClientStatus buildClientStatus(ConfigDb configDb, String slug) {
        JwtParser.JwtClaims claims = JwtParser.parse(configDb.getTokens(slug).get(0).token());
        return new ClientStatus(configDb.getClient(slug), true, claims.expirationTime());
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
