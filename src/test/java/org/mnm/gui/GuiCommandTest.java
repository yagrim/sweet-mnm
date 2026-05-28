package org.mnm.gui;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mnm.cli.Arguments;
import org.mnm.cli.Command;
import org.mnm.client.InstallerOptions;
import org.mnm.config.Client;
import org.mnm.config.ConfigDb;

import static org.assertj.core.api.Assertions.assertThat;

class GuiCommandTest {

    @Test
    void shouldPassFalseWhenNoClientsExist(@TempDir Path tempDir) {
        Path dbFile = tempDir.resolve("config.db");
        AtomicBoolean hasClients = new AtomicBoolean(true);
        Command command = new GuiCommand(() -> dbFile, hasClients::set);

        command.run(Arguments.parse());

        assertThat(hasClients.get()).isFalse();
    }

    @Test
    void shouldPassTrueWhenAClientExists(@TempDir Path tempDir) {
        Path dbFile = tempDir.resolve("config.db");
        try (ConfigDb configDb = ConfigDb.open(dbFile)) {
            configDb.addClient(new Client("sample", "1.0.0", Client.Status.COMPLETED, tempDir));
        }

        AtomicBoolean hasClients = new AtomicBoolean(false);
        Command command = new GuiCommand(() -> dbFile, hasClients::set);

        command.run(Arguments.parse());

        assertThat(hasClients.get()).isTrue();
    }

    @Test
    void shouldDisableRepairAndPlayWhenNoClientsExist() {
        var panel = GuiCommand.createButtonsPanel(null, false);

        assertThat(findButton(panel, "Install")).isNotNull();
        assertThat(findButton(panel, "Repair")).isNotNull();
        assertThat(findButton(panel, "Play")).isNotNull();
        assertThat(findButton(panel, "Install").isEnabled()).isTrue();
        assertThat(findButton(panel, "Repair").isEnabled()).isFalse();
        assertThat(findButton(panel, "Play").isEnabled()).isFalse();
    }

    @Test
    void shouldDisableInstallWhenAClientExists() {
        var panel = GuiCommand.createButtonsPanel(null, true);

        assertThat(findButton(panel, "Install")).isNotNull();
        assertThat(findButton(panel, "Repair")).isNotNull();
        assertThat(findButton(panel, "Play")).isNotNull();
        assertThat(findButton(panel, "Install").isEnabled()).isFalse();
        assertThat(findButton(panel, "Repair").isEnabled()).isTrue();
        assertThat(findButton(panel, "Play").isEnabled()).isTrue();
    }

    @Test
    void shouldCreateTabbedPanelWithMainAndOptionsTabs() {
        var tabs = GuiCommand.createTabbedPanel(null, true,
            (_, _) -> {
            }, args -> {
            }, slug -> {
            }, _ -> {
            });

        assertThat(tabs.getTabCount()).isEqualTo(2);
        assertThat(tabs.getTitleAt(0)).isEqualTo("Main");
        assertThat(tabs.getTitleAt(1)).isEqualTo("Options");
        assertThat(findButton(tabs.getComponentAt(0), "Install")).isNotNull();
        assertThat(findCheckBox(tabs.getComponentAt(1), "Enable debug")).isNotNull();
        assertThat(findCheckBox(tabs.getComponentAt(1), "Enable debug").getActionCommand()).isEqualTo("debug");
    }

    @Test
    void shouldCreateInstallDialogWithEmailAndPasswordFields() {
        var panel = GuiCommand.createInstallDialogPanel();

        assertThat(findLabel(panel, "Email")).isNotNull();
        assertThat(findLabel(panel, "Password")).isNotNull();
        assertThat(findTextField(panel)).isNotNull();
        assertThat(findPasswordField(panel)).isNotNull();
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
    void shouldDisableInstallButtonWhileInstallActionRuns() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var installButton = new javax.swing.JButton("Install");

        GuiCommand.runInstallAction(installButton,
            (username, password) -> {
                started.countDown();
                try {
                    release.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }, "alice@example.com", "secret");

        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(installButton.isEnabled()).isFalse();

        release.countDown();
        waitForButtonEnabled(installButton);

        assertThat(installButton.isEnabled()).isTrue();
    }

    @Test
    void shouldInvokeRepairActionWithMnmSlugWhenRepairIsClicked() {
        AtomicReference<String> repairedSlug = new AtomicReference<>();
        var panel = GuiCommand.createButtonsPanel(null, true,
            (_, _) -> {
            }, repairedSlug::set, _ -> {
            }, _ -> {
            });

        findButton(panel, "Repair").doClick();

        assertThat(repairedSlug.get()).isEqualTo("mnm");
    }

    @Test
    void shouldDisableRepairButtonWhileRepairActionRuns() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var panel = GuiCommand.createButtonsPanel(null, true,
            (_, _) -> {
            }, slug -> {
                started.countDown();
                try {
                    release.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }, _ -> {
            }, _ -> {
            });
        var repairButton = findButton(panel, "Repair");

        repairButton.doClick();

        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(repairButton.isEnabled()).isFalse();

        release.countDown();
        waitForButtonEnabled(repairButton);

        assertThat(repairButton.isEnabled()).isTrue();
    }

    @Test
    void shouldInvokeRunActionWithMnmSlugWhenPlayIsClicked() {
        AtomicReference<String> playedSlug = new AtomicReference<>();
        var panel = GuiCommand.createButtonsPanel(null, true,
            (_, _) -> {
            }, _ -> {
            }, _ -> {
            }, args -> playedSlug.set(args.get("slug")));

        findButton(panel, "Play").doClick();

        assertThat(playedSlug.get()).isEqualTo("mnm");
    }

    @Test
    void shouldExposeCommandMetadata() {
        Command command = new GuiCommand();

        assertThat(command.name()).isEqualTo("gui");
        assertThat(command.description()).isEqualTo("Opens a simple Swing interface");
        assertThat(command.help()).contains("sweet gui");
    }

    private static javax.swing.JButton findButton(java.awt.Component component, String text) {
        if (component instanceof javax.swing.JButton button && text.equals(button.getText())) {
            return button;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                javax.swing.JButton found = findButton(child, text);
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

    private static javax.swing.JLabel findLabel(java.awt.Component component, String text) {
        if (component instanceof javax.swing.JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                javax.swing.JLabel found = findLabel(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JTextField findTextField(java.awt.Component component) {
        if (component instanceof javax.swing.JTextField field && !(field instanceof javax.swing.JPasswordField)) {
            return field;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                javax.swing.JTextField found = findTextField(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static javax.swing.JPasswordField findPasswordField(java.awt.Component component) {
        if (component instanceof javax.swing.JPasswordField field) {
            return field;
        }
        if (component instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                javax.swing.JPasswordField found = findPasswordField(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void waitForButtonEnabled(javax.swing.JButton button) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (button.isEnabled()) {
                return;
            }
            Thread.sleep(20);
        }
    }
}
