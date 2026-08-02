package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.GridLayout;

import org.mnm.client.RunnerOptions;
import org.mnm.config.SettingsStore;

import static org.mnm.gui.MainTabs.DEFAULT_SLUG;

class OptionsPanel extends JPanel {

    private final GeneralOptionsPanel generalPanel;
    private final LinuxOptionsPanel linuxPanel;

    OptionsPanel(SettingsStore settingsStore, CredentialsHandler credentialsHandler) {
        super();
        this.setLayout(new GridLayout(1, 2, 10, 0));
        this.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));

        generalPanel = new GeneralOptionsPanel(settingsStore, credentialsHandler, this);
        linuxPanel = new LinuxOptionsPanel(settingsStore);

        this.add(generalPanel);
        this.add(linuxPanel);
    }

    boolean useInMemoryHashing() {
        return generalPanel.isInMemoryHashing();
    }

    RunnerOptions getRunnerOptions() {
        return new RunnerOptions(DEFAULT_SLUG, null, false,
            new RunnerOptions.LinuxOptions(linuxPanel.isMangoHudEnabled(), linuxPanel.isUseClientAsPrefix(),
                new RunnerOptions.UmuOptions(linuxPanel.getUmuGameId(), linuxPanel.getUmuProtonPath(), linuxPanel.getUmuWinePrefix()))
        );
    }

}
