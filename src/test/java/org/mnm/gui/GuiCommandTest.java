package org.mnm.gui;

import javax.swing.*;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
        Command command = new GuiCommand(() -> dbFile, (client, hasToken) -> {
            clientRef.set(client);
            hasTokens.set(hasToken);
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
        });

        command.run(Arguments.parse());

        assertThat(clientRef.get()).isNotNull();
    }

    @Test
    void shouldDisableRepairAndPlayWhenNoClientsExist() {
        var panel = GuiCommand.createButtonsPanel(null, null, false);

        assertThat(findButton(panel, "Install")).isNotNull();
        assertThat(findButton(panel, "Repair")).isNotNull();
        assertThat(findButton(panel, "Play")).isNotNull();
        assertThat(findButton(panel, "Install").isEnabled()).isTrue();
        assertThat(findButton(panel, "Repair").isEnabled()).isFalse();
        assertThat(findButton(panel, "Play").isEnabled()).isFalse();
    }

    @Test
    void shouldDisableInstallWhenAClientExists() {
        Client client = testClient();
        var panel = GuiCommand.createButtonsPanel(null, client, false);

        assertThat(findButton(panel, "Install")).isNotNull();
        assertThat(findButton(panel, "Repair")).isNotNull();
        assertThat(findButton(panel, "Play")).isNotNull();
        assertThat(findButton(panel, "Install").isEnabled()).isFalse();
        assertThat(findButton(panel, "Repair").isEnabled()).isTrue();
        assertThat(findButton(panel, "Play").isEnabled()).isTrue();
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

        assertThat(tabs.getTabCount()).isEqualTo(2);
        assertThat(tabs.getTitleAt(0)).isEqualTo("Client");
        assertThat(tabs.getTitleAt(1)).isEqualTo("Options");
        assertThat(findButton(tabs.getComponentAt(0), "Install")).isNotNull();
        assertThat(findCheckBox(tabs.getComponentAt(1), "Enable debug")).isNotNull();
        assertThat(findCheckBox(tabs.getComponentAt(1), "Enable debug").getActionCommand()).isEqualTo("debug");
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

//    @Test
//    void shouldDisableInstallButtonWhileInstallActionRuns() throws Exception {
//        CountDownLatch started = new CountDownLatch(1);
//        CountDownLatch release = new CountDownLatch(1);
//        final JButton installButton = new JButton("Install");
//        final var buttonsHandler = new ClientButtonsHandler(installButton, new JButton(), new JButton(), new JButton(), new JButton());
//
//        GuiCommand.InstallAction installAction = _ -> {
//            started.countDown();
//            try {
//                release.await(1, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                throw new IllegalStateException(e);
//            }
//        };
//
//        GuiCommand.installHandle(buttonsHandler, installAction, TEST_SLUG);
//
//        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
//        assertThat(installButton.isEnabled()).isFalse();
//
//        release.countDown();
//        waitForButtonEnabled(installButton);
//
//        assertThat(installButton.isEnabled()).isTrue();
//    }

    @Test
    void shouldInvokeRepairActionWithMnmSlugWhenRepairIsClicked() {
        AtomicReference<String> repairedSlug = new AtomicReference<>();
        Client client = testClient();

        var panel = GuiCommand.createButtonsPanel(null, client, false,
            slug -> {
                repairedSlug.set(slug);
                return testClient();
            }, (_, _) -> null,
            _ -> {
            });

        findButton(panel, "Repair").doClick();

        assertThat(repairedSlug.get()).isEqualTo("mnm");
    }

    @Test
    void shouldDisableRepairButtonWhileRepairActionRuns() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Client client = testClient();

        var panel = GuiCommand.createButtonsPanel(null, client, false,
            slug -> {
                started.countDown();
                try {
                    release.await(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return testClient();
            }, (_, _) -> null, _ -> {
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
        Client client = testClient();

        var panel = GuiCommand.createButtonsPanel(null, client, false,
            _ -> null,
            (_, _) -> null,
            _ -> {
            },
            args -> playedSlug.set(args.get("slug")));

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
