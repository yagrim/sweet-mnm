package org.mnm.gui;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import org.mnm.config.OS;
import org.mnm.config.SettingsStore;

import static org.mnm.config.SettingsStore.DEFAULT_UMU_GAMEID;
import static org.mnm.config.SettingsStore.DEFAULT_UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.MANGOHUD_KEY;
import static org.mnm.config.SettingsStore.UMU_GAMEID;
import static org.mnm.config.SettingsStore.UMU_PROTONPATH;
import static org.mnm.config.SettingsStore.UMU_USE_CLIENT_AS_PREFIX;
import static org.mnm.config.SettingsStore.UMU_WINEPREFIX;
import static org.mnm.gui.ClientPanel.SCALE;

public class LinuxOptionsPanel extends BaseOptionsPanel {

    private final JCheckBox mangoHudOption = new JCheckBox("Enable MangoHud");
    private final JCheckBox useClientAsPrefix;

    private final TextOption umuGameId;
    private final TextOption umuProtonPath;
    private final TextOption umuWinePrefix;

    private final SettingsStore settingsStore;

    public LinuxOptionsPanel(SettingsStore settingsStore) {
        super("Linux");
        this.settingsStore = settingsStore;

        mangoHudOption.setActionCommand("mangohud");
        mangoHudOption.addActionListener(_ ->
            this.settingsStore.putBoolean(MANGOHUD_KEY, mangoHudOption.isSelected()));

        mangoHudOption.setSelected(this.settingsStore.getBoolean(MANGOHUD_KEY, false));

        umuWinePrefix = new TextOption("UMU WinePrefix", this.settingsStore, UMU_WINEPREFIX);
        useClientAsPrefix = new CheckboxOption("Use game location for WinePrefix", this.settingsStore, UMU_USE_CLIENT_AS_PREFIX, true);
        useClientAsPrefix.addActionListener(_ -> refresh(umuWinePrefix, useClientAsPrefix));
        refresh(umuWinePrefix, useClientAsPrefix);

        addLinuxComponent(mangoHudOption);
        umuGameId = new TextOption("UMU GameId", this.settingsStore, UMU_GAMEID, DEFAULT_UMU_GAMEID);
        addLinuxComponent(umuGameId);
        umuProtonPath = new TextOption("UMU ProtonPath", this.settingsStore, UMU_PROTONPATH, DEFAULT_UMU_PROTONPATH);
        addLinuxComponent(umuProtonPath);
        addLinuxComponent(useClientAsPrefix);
        addLinuxComponent(umuWinePrefix);
    }

    private static void refresh(TextOption umuWinePrefix, JCheckBox useGameLocationForPrefix) {
        umuWinePrefix.setEnabled(!useGameLocationForPrefix.isSelected());
    }

    private void addLinuxComponent(JComponent component) {
        if (OS.isWindows()) {
            component.setEnabled(false);
        }
        this.add(component);
        this.add(Box.createVerticalStrut(SCALE));
    }

    public boolean isMangoHudEnabled() {
        return mangoHudOption.isSelected();
    }

    public Boolean isUseClientAsPrefix() {
        return useClientAsPrefix.isSelected();
    }

    public String getUmuGameId() {
        return umuGameId.getText();
    }

    public String getProtonPath() {
        return umuProtonPath.getText();
    }

    public String getWinePrefix() {
        return umuWinePrefix.getText();
    }
}
