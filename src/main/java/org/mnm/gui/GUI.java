package org.mnm.gui;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.client.RunnerOptions;
import org.mnm.gui.GuiCommand.LoginAction;
import org.mnm.gui.GuiCommand.LogoutAction;
import org.mnm.gui.GuiCommand.RepairAction;
import org.mnm.gui.GuiCommand.RunAction;
import org.mnm.tools.PanicException;

import static org.mnm.gui.GuiComponents.setFontSize;
import static org.mnm.gui.MessageWindow.showErrorMessageDialogSync;
import static org.mnm.gui.MessageWindow.showInfoMessageDialogSync;
import static org.mnm.tools.StringUtils.isEmpty;

public class GUI {

    private static final Logger logger = LoggerFactory.getLogger(GUI.class);

    static final String DEFAULT_SLUG = "mnm";

    @FunctionalInterface
    interface LoginButtonAction {
        void login(LoginAction action, List<LoginListener> listeners);
    }

    @FunctionalInterface
    interface LogoutButtonAction {
        void logout(LogoutAction action, List<LoginListener> listeners);
    }

    @FunctionalInterface
    interface RunButtonAction {
        void run(RunAction runAction, Supplier<RunnerOptions> optionsSupplier);
    }

    static GuiCommand.Tabs createTabbedPanel(JFrame frame,
                                             LoginAction loginAction, LogoutAction logoutAction,
                                             RepairAction repairAction, RunAction runAction) {

        final ClientPanel clientPanel = new ClientPanel(frame);
        final OptionsPanel optionsPanel = new OptionsPanel();

        // init Listeners
        // ClientButtonsHandler: LoginListener, LogoutListener, RepairListener
        // OptionsPanel: RepairListener
        // infoPanel: LoginListener, LogoutListener, RepairListener
        // TODO move play button to PlayPannel, implements LoginListener, LogoutListener, RepairListener

        List<LoginListener> loginListeners = List.of(clientPanel);
        List<RepairListener> repairListeners = List.of(clientPanel, optionsPanel);

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Client", clientPanel);
        tabs.addTab("Options", optionsPanel);
        setFontSize(tabs, 15f);
        return new GuiCommand.Tabs(clientPanel, optionsPanel, tabs);
    }


    private void handleInstall(RepairAction installAction, BooleanSupplier inMemoryHashing, List<RepairListener> listeners) {
        listeners.forEach(listener -> listener.repairStart());

        CompletableFuture
            .supplyAsync(() -> installAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean()))
            .whenComplete((client, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Installation completed");
                listeners.forEach(listener -> listener.repairDone(client));
            }));
    }

    private void handleRepair(RepairAction repairAction, BooleanSupplier inMemoryHashing, List<RepairListener> listeners) {
        listeners.forEach(listener -> listener.repairStart());

        CompletableFuture
            .supplyAsync(() -> repairAction.repair(DEFAULT_SLUG, inMemoryHashing.getAsBoolean()))
            .whenComplete((client, _) -> SwingUtilities.invokeLater(() -> {
                showInfoMessageDialogSync("Repair completed");
                listeners.forEach(listener -> listener.repairDone(client));
            }));
    }

    private void handleLogin(JFrame parent, GuiCommand.LoginAction loginAction, List<LoginListener> listeners) {
        listeners.forEach(LoginListener::loginStart);

        final CredentialsPanel credentialsPanel = new CredentialsPanel();
        final int result = credentialsPanel.show(parent);

        if (result == JOptionPane.OK_OPTION && !isEmpty(credentialsPanel.getUsername()) && !isEmpty(credentialsPanel.getPassword())) {
            try {
                final ClientStatus client = loginAction.login(credentialsPanel.getUsername(), credentialsPanel.getPassword());

                listeners.forEach(listener -> listener.loginDone(client));
            } catch (Exception e) {
                // TODO XXX remove direct call, should be through interface
//                this.refresh();
                logger.error("", e);
                showErrorMessageDialogSync("Error: " + e.getMessage());
            }
        } else {
//            this.refresh();
        }
    }

    private static void handleLogout(LogoutAction action, List<LoginListener> listeners) {
        action.logout(DEFAULT_SLUG);
        listeners.forEach(listener -> listener.logoutDone());
    }

    private static void runAction(RunAction runAction, Supplier<RunnerOptions> optionsSupplier) {
        try {
            RunnerOptions options = new RunnerOptions(DEFAULT_SLUG, null, false, optionsSupplier.get().enableMangoHud());
            runAction.run(options);
        } catch (PanicException e) {
            logger.error("", e);
            showErrorMessageDialogSync("Error: " + e.getMessage());
        }
    }
}
