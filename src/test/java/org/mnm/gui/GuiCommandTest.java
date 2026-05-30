package org.mnm.gui;

import javax.swing.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.client.InstallerOptions;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.config.Client.Status.COMPLETED;

// TODO update tests to cover
// Test buttons stat change after install, logout
@Disabled
class GuiCommandTest {

    private static final String TEST_SLUG = "test-mnm";

    @Test
    void shouldStartGui(@TempDir Path tempDir) {
        Path dbFile = tempDir.resolve("config.db");
        AtomicReference<Client> clientRef = new AtomicReference<>();
        AtomicBoolean hasTokens = new AtomicBoolean(false);
        Command command = new GuiCommand(() -> dbFile,
            (client, hasToken) -> {
                clientRef.set(client);
                hasTokens.set(hasToken);
            }, (_, _) -> {
        });

        command.run(Arguments.parse());

        assertThat(clientRef.get()).isNotNull();
        assertThat(hasTokens.get()).isTrue();
    }

    @Test
    void shouldPassTrueWhenAClientExists(@TempDir Path tempDir) {
        Path dbFile = tempDir.resolve("config.db");
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client("sample", "1.0.0", COMPLETED, tempDir));
        }

        AtomicReference<Client> clientRef = new AtomicReference<>();
        AtomicBoolean hasTokens = new AtomicBoolean(false);
        Command command = new GuiCommand(() -> dbFile, (client, hasToken) -> {
            clientRef.set(client);
            hasTokens.set(hasToken);
        }, (_, _) -> {
        });

        command.run(Arguments.parse());

        assertThat(clientRef.get()).isNotNull();
    }

    @Test
    void shouldCreateTabbedPanelWithMainAndOptionsTabs() {
        Client client = testClient();
        var tabs = GuiCommand.createTabbedPanel(null, client, false,
            _ -> null,
            (_, _) -> {
                return null;
            }, _ -> {
            }, _ -> {
            });

        assertThat(tabs).isNotNull();
        assertThat(tabs.clientPanel()).isNotNull();
        assertThat(tabs.optionsPanel()).isNotNull();
        assertThat(tabs.root()).isNotNull();

        JTabbedPane tabPanel = tabs.root();
        assertThat(tabPanel.getTabCount()).isEqualTo(2);
        assertThat(tabPanel.getTitleAt(0)).isEqualTo("Client");
        assertThat(tabPanel.getTitleAt(1)).isEqualTo("Options");

        assertThat(findButton(tabPanel.getComponentAt(0), "Install")).isNotNull();
        assertThat(findCheckBox(tabPanel.getComponentAt(1), "Enable debug")).isNotNull();
        assertThat(findCheckBox(tabPanel.getComponentAt(1), "Enable debug").getActionCommand()).isEqualTo("debug");
    }

    @Test
    void shouldBuildInstallOptionsWithUsernameAndPasswordOnly() {
        InstallerOptions options = InstallerOptions.forInstall("alice@example.com", "secret");

        assertThat(options.username()).isEqualTo("alice@example.com");
        assertThat(options.password()).isEqualTo("secret");
        assertThat(options.slug()).isNull();
        assertThat(options.fileCheck()).isNotNull();
        assertThat(options.toString()).contains("xxhsum");
    }

    @Test
    void shouldBuildRepairOptionsWithSlugOnly() {
        InstallerOptions options = InstallerOptions.forRepair("mnm");

        assertThat(options.username()).isNull();
        assertThat(options.password()).isNull();
        assertThat(options.slug()).isEqualTo("mnm");
        assertThat(options.fileCheck()).isNotNull();
        assertThat(options.toString()).contains("xxhsum");
    }

    @Test
    void shouldExposeCommandMetadata() {
        Command command = new GuiCommand();

        assertThat(command.name()).isEqualTo("gui");
        assertThat(command.description()).isEqualTo("Opens a simple Swing interface");
        assertThat(command.help()).contains("sweet gui");
    }

    private static Client testClient() {
        return new Client("client", "1.2.3", COMPLETED, Path.of("."));
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

    private static javax.swing.JCheckBox findCheckBox(java.awt.Component component, String text) {
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

    private static void waitForButtonEnabled(JButton button) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (button.isEnabled()) {
                return;
            }
            Thread.sleep(20);
        }
    }

}
