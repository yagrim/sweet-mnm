package org.mnm.gui;

import javax.swing.JFrame;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.mnm.client.RunnerOptions;
import org.mnm.config.Client;

import static org.assertj.core.api.Assertions.assertThat;

class MainTabsTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldCreateTabbedPanelWithMainAndOptionsTabs() {
        JFrame root = new JFrame();
        var tabs = new MainTabs(root,
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
            },
            new GuiCommand.PlayAction() {
                @Override
                public void run(RunnerOptions options) {

                }
            });

        assertThat(tabs).isNotNull();
    }

}
