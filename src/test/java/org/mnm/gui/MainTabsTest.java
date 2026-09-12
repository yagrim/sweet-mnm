package org.mnm.gui;

import javax.swing.JFrame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.mnm.config.Client;
import org.mnm.config.SettingsStore;

import static org.assertj.core.api.Assertions.assertThat;

class MainTabsTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldCreateTabbedPanelWithMainAndOptionsTabs() {
        SettingsStore settingsStore = new SettingsStore() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public void put(String key, String value) {
            }

            @Override
            public void delete(String key) {
            }
        };

        JFrame root = new JFrame();
        CredentialsHandler credentialsHandler = new CredentialsHandler(settingsStore);
        ClientPanel clientPanel = new ClientPanel(root,
            new GuiCommand.LoginAction() {
                @Override
                public ClientStatus login(String username, String password) {
                    return null;
                }
            },
            new GuiCommand.LogoutAction() {
                @Override
                public void logout(String slug) {

                }
            },
            new GuiCommand.RepairAction() {
                @Override
                public ClientStatus repair(String slug, Client.Status status, boolean inMemoryHashing) {
                    return null;
                }
            }, () -> false, credentialsHandler, 1f);

        var tabs = new MainTabs(settingsStore, clientPanel, new OptionsPanel(settingsStore, credentialsHandler));

        assertThat(tabs).isNotNull();
    }

}
