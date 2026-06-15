package org.mnm.gui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.mnm.gui.GuiCommand.LoginAction;
import org.mnm.gui.GuiCommand.LogoutAction;
import org.mnm.gui.GuiCommand.PlayAction;
import org.mnm.gui.GuiCommand.RepairAction;

import static org.mnm.gui.GuiComponents.setFontSize;

class MainTabs extends JTabbedPane {

    static final String DEFAULT_SLUG = "mnm";

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
