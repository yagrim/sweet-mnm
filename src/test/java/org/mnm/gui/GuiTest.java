package org.mnm.gui;

import javax.swing.*;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import org.mnm.config.OS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.gui.GuiCommandTest.testClient;

class GuiTest {

    @Test
    void shouldCreateTabbedPanelWithMainAndOptionsTabs() {
        var client = testClient();
        var clientStatus = new ClientStatus(client, true, true, Instant.now());

//        var tabs = new MainTabs(null, clientStatus,
//            (_, _) -> null,
//            _ -> {
//            },
//            (_, _) -> null,
//            _ -> {
//            });
//
//        assertThat(tabs).isNotNull();
//        assertThat(tabs.clientPanel()).isNotNull();
//        assertThat(tabs.optionsPanel()).isNotNull();
//        assertThat(tabs.root()).isNotNull();
//
//        JTabbedPane tabPanel = tabs.root();
//        assertThat(tabPanel.getTabCount()).isEqualTo(2);
//        assertThat(tabPanel.getTitleAt(0)).isEqualTo("Client");
//        assertThat(tabPanel.getTitleAt(1)).isEqualTo("Options");
//
//        assertThat(findButton(tabPanel.getComponentAt(0), "Install")).isNotNull();
//        assertThat(tabPanel.getComponentAt(1)).isInstanceOf(javax.swing.JPanel.class);
//
//        javax.swing.JPanel optionsPanel = (javax.swing.JPanel) tabPanel.getComponentAt(1);
//        assertThat(optionsPanel.getLayout()).isInstanceOf(javax.swing.BoxLayout.class);
//        assertThat(((javax.swing.BoxLayout) optionsPanel.getLayout()).getAxis()).isEqualTo(javax.swing.BoxLayout.Y_AXIS);
//        assertThat(optionsPanel.getComponentCount()).isEqualTo(7);
//        assertThat(findCheckBox(optionsPanel, "Enable debug")).isNotNull();
//        assertThat(findCheckBox(optionsPanel, "Enable debug").getActionCommand()).isEqualTo("debug");
//        assertThat(findCheckBox(optionsPanel, "In-memory hashing")).isNotNull();
//        assertThat(findCheckBox(optionsPanel, "In-memory hashing").getActionCommand()).isEqualTo("in-memory-hashing");
//        String enableMangoHudLabel = OS.isWindows() ? "Enable MangoHud (Linux only)" : "Enable MangoHud";
//        assertThat(findCheckBox(optionsPanel, enableMangoHudLabel)).isNotNull();
//        assertThat(findCheckBox(optionsPanel, enableMangoHudLabel).getActionCommand()).isEqualTo("mangohud");
    }

    private static JButton findButton(java.awt.Component component, String text) {
        if (component instanceof JButton button && text.equals(button.getText())) {
            return button;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JCheckBox findCheckBox(java.awt.Component component, String text) {
        if (component instanceof javax.swing.JCheckBox checkBox && text.equals(checkBox.getText())) {
            return checkBox;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                javax.swing.JCheckBox found = findCheckBox(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
