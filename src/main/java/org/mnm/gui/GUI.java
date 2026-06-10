package org.mnm.gui;

import javax.swing.*;
import java.util.List;

import static org.mnm.gui.GuiComponents.setFontSize;

public class GUI {

    static final String DEFAULT_SLUG = "mnm";

    static GuiCommand.Tabs createTabbedPanel(JFrame frame, ClientStatus clientStatus,
                                             GuiCommand.LoginAction loginAction, GuiCommand.LogoutAction logoutAction,
                                             GuiCommand.RepairAction repairAction, GuiCommand.RunAction runAction) {

        final ClientPanel clientPanel = new ClientPanel(frame);
        final OptionsPanel optionsPanel = new OptionsPanel(clientStatus);

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Client", clientPanel.initialize(clientStatus, repairAction, loginAction, logoutAction, runAction, optionsPanel::useInMemoryHashing, optionsPanel::getRunnerOptions, List.of(optionsPanel)));
        tabs.addTab("Options", optionsPanel);
        setFontSize(tabs, 15f);
        return new GuiCommand.Tabs(clientPanel, optionsPanel, tabs);
    }

}
