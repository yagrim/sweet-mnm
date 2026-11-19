package org.mnm.gui;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import org.mnm.cache.CacheHandler;
import org.mnm.client.RunnerOptions;
import org.mnm.config.Settings;
import org.mnm.config.SettingsStore;


class OptionsPanel extends JPanel {

    private final GeneralOptionsPanel generalPanel;
    private final LinuxOptionsPanel linuxPanel;

    OptionsPanel(JFrame owner, SettingsStore settingsStore, CredentialsHandler credentialsHandler, CacheHandler cacheHandler) {
        super(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));

        generalPanel = new GeneralOptionsPanel(settingsStore, credentialsHandler, cacheHandler, this);
        linuxPanel = new LinuxOptionsPanel(settingsStore, () -> new ProtonOptionsWindow(owner, settingsStore));

        JPanel optionColumns = new JPanel(new GridLayout(1, 2, 10, 0));
        optionColumns.add(generalPanel);
        optionColumns.add(linuxPanel);

        this.add(optionColumns, BorderLayout.CENTER);
        this.add(new SweetLogsPanel(Settings.readUiScaling(settingsStore)), BorderLayout.SOUTH);
    }

    boolean useInMemoryHashing() {
        return generalPanel.isInMemoryHashing();
    }

    RunnerOptions getRunnerOptions(String slug) {
        return new RunnerOptions(slug, null, true,
            new RunnerOptions.LinuxOptions(linuxPanel.isMangoHudEnabled(), linuxPanel.isUseClientAsPrefix(),
                new RunnerOptions.UmuOptions(linuxPanel.getUmuGameId(), linuxPanel.getUmuProtonPath(), linuxPanel.getUmuWinePrefix()),
                new RunnerOptions.ProtonOptions(linuxPanel.isProtonUseWined3dEnabled(), linuxPanel.isProtonWaylandEnabled()),
                new RunnerOptions.WineOptions(linuxPanel.getCursorSize()))
        );
    }

}
