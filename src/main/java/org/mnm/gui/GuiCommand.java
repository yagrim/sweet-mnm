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
import org.mnm.client.RunnerOptions;
import org.mnm.config.ConfigDb;
import org.mnm.config.ConfigDbLocator;

import static org.mnm.GeneralOptions.toggleDebug;
import static org.mnm.config.Environment.API_BASE_URL;

public class GuiCommand implements Command {

    private static final String DEFAULT_SLUG = "mnm";

    @FunctionalInterface
    interface GuiStarter {
        void start(boolean hasClients);
    }

    @FunctionalInterface
    interface InstallAction {
        void install(String username, String password);
    }

    @FunctionalInterface
    interface RepairAction {
        void repair(String slug);
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

    private final InstallAction installAction;
    private final RunAction runAction;
    private final RepairAction repairAction;
    private final LogoutAction logoutAction;

    public GuiCommand() {
        this(new ConfigDbLocator());
    }

    GuiCommand(Supplier<Path> configDbLocator) {
        this.configDbLocator = configDbLocator;
        this.installAction = (username, password) -> installClient(configDbLocator, username, password);
        this.runAction = args -> runClient(configDbLocator, args);
        this.repairAction = slug -> repairClient(configDbLocator, slug);
        this.logoutAction = slug -> repairClient(configDbLocator, slug);
        this.guiStarter = this::startSwingInterface;
    }

    GuiCommand(Supplier<Path> configDbLocator, GuiStarter guiStarter) {
        this.configDbLocator = configDbLocator;
        this.guiStarter = guiStarter;
        this.installAction = (username, password) -> installClient(configDbLocator, username, password);
        this.runAction = args -> runClient(configDbLocator, args);
        this.repairAction = slug -> repairClient(configDbLocator, slug);
        this.logoutAction = slug -> repairClient(configDbLocator, slug);
    }

    @Override
    public void run(Arguments args) {
        guiStarter.start(hasAvailableClient());
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

    private boolean hasAvailableClient() {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            return !configDb.getClients().isEmpty();
        }
    }

    private void startSwingInterface(boolean hasClients) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                final JFrame frame = new JFrame("Sweet GUI");
                final JTabbedPane tabs = createTabbedPanel(frame, hasClients, installAction, repairAction, logoutAction, runAction);

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

    static JPanel createButtonsPanel(JFrame frame, boolean hasClients) {
        return createButtonsPanel(frame, hasClients, (username, password) -> {
        }, _ -> {
        }, _ -> {
        });
    }

    static JPanel createButtonsPanel(JFrame frame, boolean hasClients,
                                     InstallAction installAction, RepairAction repairAction, LogoutAction logoutAction) {
        return createButtonsPanel(frame, hasClients, installAction, repairAction, logoutAction, args -> {
        });
    }

    static JPanel createButtonsPanel(JFrame frame, boolean hasClients,
                                     InstallAction installAction,
                                     RepairAction repairAction,
                                     LogoutAction logoutAction,
                                     RunAction runAction
    ) {
        final JButton installButton = new JButton("Install");
        final JButton repairButton = new JButton("Repair");
        final JButton playButton = new JButton("Play");
        final JButton logoutButton = new JButton("Logout");

        installButton.addActionListener(_ -> showInstallDialog(frame, installButton, installAction));
        repairButton.addActionListener(_ -> runRepairAction(repairButton, repairAction));
        playButton.addActionListener(_ -> runAction.run(Arguments.parse("--slug", DEFAULT_SLUG)));
        playButton.addActionListener(_ -> logoutAction(logoutButton, logoutAction));

        installButton.setEnabled(!hasClients);
        repairButton.setEnabled(hasClients);
        playButton.setEnabled(hasClients);

        final JPanel firstRow = new JPanel(new GridLayout(1, 2, 8, 0));
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
        return buttons;
    }

    static JTabbedPane createTabbedPanel(JFrame frame, boolean hasClients,
                                         InstallAction installAction, RepairAction repairAction, LogoutAction logoutAction, RunAction runAction) {

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Main", createButtonsPanel(frame, hasClients, installAction, repairAction, logoutAction, runAction));
        tabs.addTab("Options", createOptionsPanel());
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

    static JPanel createInstallDialogPanel() {
        final JTextField emailField = new JTextField(20);
        final JPasswordField passwordField = new JPasswordField(20);

        final JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Email"));
        panel.add(emailField);
        panel.add(new JLabel("Password"));
        panel.add(passwordField);
        return panel;
    }

    private static void runRepairAction(JButton button, RepairAction repairAction) {
        button.setEnabled(false);
        CompletableFuture
            .runAsync(() -> repairAction.repair(DEFAULT_SLUG))
            .whenComplete((_, _) -> SwingUtilities.invokeLater(() -> button.setEnabled(true)));
    }

    private static void logoutAction(JButton button, LogoutAction logoutAction) {
        logoutAction.logout(DEFAULT_SLUG);
    }

    private static void showInstallDialog(JFrame owner, JButton installButton, InstallAction installAction) {
        final JPanel panel = createInstallDialogPanel();
        final int result = JOptionPane.showConfirmDialog(
            owner,
            panel,
            "Install client",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            final JTextField emailField = findTextField(panel);
            final JPasswordField passwordField = findPasswordField(panel);
            final String username = emailField.getText();
            final String password = new String(passwordField.getPassword());
            runInstallAction(installButton, installAction, username, password);
        }
    }

    static void runInstallAction(JButton installButton, InstallAction installAction, String username, String password) {
        installButton.setEnabled(false);
        CompletableFuture
            .runAsync(() -> installAction.install(username, password))
            .whenComplete((_, _) -> SwingUtilities.invokeLater(() -> installButton.setEnabled(true)));
    }

    private static void installClient(Supplier<Path> configDbLocator, String username, String password) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            InstallerOptions options = InstallerOptions.forInstall(username, password);
            new ClientInstaller(configDb)
                .install(options, Path.of(System.getProperty("user.dir")), API_BASE_URL);

            showMessageDialogSync("Install completed !!");
        }
    }

    private static void repairClient(Supplier<Path> configDbLocator, String slug) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            InstallerOptions options = InstallerOptions.forRepair(slug);
            new ClientInstaller(configDb)
                .install(options, Path.of(System.getProperty("user.dir")), API_BASE_URL);

            showMessageDialogSync("Repair completed !!");
        }
    }

    private static void logout(Supplier<Path> configDbLocator, String slug) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {
            configDb.deleteTokens(slug);
        }
    }

    private static void showMessageDialogSync(String message) {
        try {
            SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(null, message));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while showing dialog", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to show dialog", e.getCause());
        }
    }

    private static void runClient(Supplier<Path> configDbLocator, Arguments args) {
        try (ConfigDb configDb = ConfigDb.open(configDbLocator.get())) {

            new ClientRunner(configDb)
                .run(RunnerOptions.parse(args));
        }
    }

    static JTextField findTextField(JComponent component) {
        if (component instanceof JTextField field && !(field instanceof JPasswordField)) {
            return field;
        }
        for (var child : component.getComponents()) {
            if (child instanceof JComponent childComponent) {
                JTextField found = findTextField(childComponent);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static JPasswordField findPasswordField(JComponent component) {
        if (component instanceof JPasswordField field) {
            return field;
        }
        for (var child : component.getComponents()) {
            if (child instanceof JComponent childComponent) {
                JPasswordField found = findPasswordField(childComponent);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
