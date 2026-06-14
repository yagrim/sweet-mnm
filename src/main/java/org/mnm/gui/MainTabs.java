package org.mnm.gui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.client.RunnerOptions;
import org.mnm.gui.GuiCommand.LoginAction;
import org.mnm.gui.GuiCommand.LogoutAction;
import org.mnm.gui.GuiCommand.PlayAction;
import org.mnm.gui.GuiCommand.RepairAction;

import static org.mnm.gui.GuiComponents.setFontSize;

class MainTabs extends JTabbedPane {

    private static final Logger logger = LoggerFactory.getLogger(MainTabs.class);

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
        void run(PlayAction runAction, Supplier<RunnerOptions> optionsSupplier);
    }

    private final ClientPanel clientPanel;
    private final OptionsPanel optionsPanel;

    MainTabs(JFrame frame,
             LoginAction loginAction, LogoutAction logoutAction,
             RepairAction repairAction,
             PlayAction playAction) {

        setFontSize(this, 15f);

        this.optionsPanel = new OptionsPanel();
        this.clientPanel = new ClientPanel(frame,
            loginAction,
            logoutAction,
            repairAction, () -> optionsPanel.useInMemoryHashing(),
            playAction, () -> optionsPanel.getRunnerOptions()
        );

        this.addTab("Client", clientPanel);
        this.addTab("Options", optionsPanel);
    }

}
