package org.mnm.gui;

import javax.swing.JButton;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.mnm.client.RunnerOptions;
import org.mnm.config.Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mnm.ApiServerStubs.TEST_SLUG;
import static org.mnm.config.Client.Status.NEEDS_UPDATE;
import static org.mnm.config.Client.Status.UPDATED;
import static org.mnm.gui.ReflectionTestTools.getButton;

class PlayPanelTest {

    private JButton play;

    @Test
    void shouldDisablePlayByDefault() {
        initComponents();

        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void shouldInvokeAction() {
        final String randomSlug = UUID.randomUUID().toString();
        final AtomicReference<String> optionSupplied = new AtomicReference<>();
        final AtomicBoolean actionPerformed = new AtomicBoolean(false);

        PlayPanel playPanel = new PlayPanel(options -> {
            optionSupplied.set(options.slug());
            actionPerformed.set(true);
        }, () -> new RunnerOptions(randomSlug, null, false, false));
        play = getButton(playPanel, "play");

        play.setEnabled(true);
        play.doClick();

        assertThat(actionPerformed).isTrue();
        assertThat(optionSupplied).hasValue(randomSlug);
    }

    @Test
    void shouldEnablePlayWhenTokenIsValidAndClientIsUpToDate() {
        ClientStatus clientStatus = new ClientStatus(client(UPDATED), true, null);

        var panel = initComponents();
        panel.refresh(clientStatus);

        assertThat(play.isEnabled()).isTrue();
    }

    @Test
    void shouldDisablePlayWhenTokenIsNotValid() {
        ClientStatus clientStatus = new ClientStatus(null, false, null);

        var panel = initComponents();
        panel.refresh(clientStatus);

        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void shouldDisablePlayWhenTokenIsValidAndClientIsNotUpToDate() {
        ClientStatus clientStatus = new ClientStatus(client(NEEDS_UPDATE), true, null);

        var panel = initComponents();
        panel.refresh(clientStatus);

        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void shouldDisablePlayAfterLogout() {
        ClientStatus clientStatus = new ClientStatus(client(UPDATED), true, null);

        var panel = initComponents();
        panel.refresh(clientStatus);
        panel.logoutDone();

        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void shouldEnableAfterLoginWhenClientIsUpToDate() {
        ClientStatus clientStatus = new ClientStatus(client(UPDATED), true, null);

        var panel = initComponents();

        panel.loginStart();
        assertThat(play.isEnabled()).isFalse();

        panel.loginDone(clientStatus);
        assertThat(play.isEnabled()).isTrue();
    }

    @Test
    void shouldDisableAfterLoginWhenClientIsNotUpToDate() {
        ClientStatus clientStatus = new ClientStatus(client(NEEDS_UPDATE), true, null);

        var panel = initComponents();

        panel.loginStart();
        assertThat(play.isEnabled()).isFalse();

        panel.loginDone(clientStatus);
        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void shouldEnableAfterRepairWhenClientIsUpToDate() {
        ClientStatus clientStatus = new ClientStatus(client(UPDATED), true, null);

        var panel = initComponents();

        panel.repairStart();
        assertThat(play.isEnabled()).isFalse();

        panel.repairDone(clientStatus);
        assertThat(play.isEnabled()).isTrue();
    }

    @Test
    void shouldEnableAfterRepairWhenClientIsNotUpToDate() {
        ClientStatus clientStatus = new ClientStatus(client(NEEDS_UPDATE), true, null);

        var panel = initComponents();

        panel.repairStart();
        assertThat(play.isEnabled()).isFalse();

        panel.repairDone(clientStatus);
        assertThat(play.isEnabled()).isTrue();
    }


    private PlayPanel initComponents() {
        PlayPanel playPanel = new PlayPanel(options -> {
        }, () -> null);
        play = getButton(playPanel, "play");
        return playPanel;
    }

    private static Client client(Client.Status updated) {
        return new Client(TEST_SLUG, "1.2.3", updated, Path.of("."));
    }
}
