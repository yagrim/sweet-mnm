package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;

import org.mnm.config.Settings;
import org.mnm.config.SettingsStore;

import static org.mnm.gui.GuiComponents.setFontSize;
import static org.mnm.gui.Style.SCALE;

class MainTabs extends JTabbedPane {

    static final String DEFAULT_SLUG = "mnm";

    private final ClientPanel clientPanel;
    private final OptionsPanel optionsPanel;

    MainTabs(SettingsStore settingsStore,
             ClientPanel clientPanel,
             OptionsPanel optionsPanel) {
        setBorder(BorderFactory.createEmptyBorder(1 * SCALE, SCALE, 0, SCALE));

        float uiScaling = Settings.readUiScaling(settingsStore);
        setFontSize(this, 15f * uiScaling);

        this.optionsPanel = optionsPanel;
        this.clientPanel = clientPanel;

        this.addTab("Client", clientPanel);
        this.addTab("Options", optionsPanel);
    }

}
