package org.mnm.gui;

import javax.swing.*;

import org.mnm.config.Client;

import static org.mnm.gui.GuiComponents.setFontSize;

public class GUI {

    static final String DEFAULT_SLUG = "mnm";

    static GuiCommand.Tabs createTabbedPanel(JFrame frame, Client client, boolean hasToken,
                                             GuiCommand.RepairAction repairAction,
                                             GuiCommand.LoginAction loginAction, GuiCommand.LogoutAction logoutAction, GuiCommand.RunAction runAction) {

        final ClientPanel clientPanel = new ClientPanel(frame);
        final OptionsPanel optionsPanel = new OptionsPanel();

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Client", clientPanel.create(client, hasToken, repairAction, loginAction, logoutAction, runAction));
        tabs.addTab("Options", optionsPanel.create());
        setFontSize(tabs, 15f);
        return new GuiCommand.Tabs(clientPanel, optionsPanel, tabs);
    }

}
