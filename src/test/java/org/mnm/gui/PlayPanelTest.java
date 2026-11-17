package org.mnm.gui;

import javax.swing.JButton;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.mnm.client.RunnerOptions;
import org.mnm.client.RunnerOptions.LinuxOptions;
import org.mnm.config.Client;
import org.mnm.config.CursorSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
            return null;
        }, () -> new RunnerOptions(randomSlug, null, false, new LinuxOptions(false, false, null, new RunnerOptions.ProtonOptions(false, false), new RunnerOptions.WineOptions(CursorSize.DEFAULT))));
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

    @Test
    void shouldDisablePlayWhileGameRunsAndEnableAfterItExits() throws Exception {
        var exit = new CompletableFuture<Process>();
        var panel = panelWithProcess(exit);
        panel.refresh(new ClientStatus(client(UPDATED), true, null));

        play.doClick();

        assertThat(play.isEnabled()).isFalse();
        // The game still runs: refresh must not enable "Play"
        panel.refresh(new ClientStatus(client(UPDATED), true, null));
        assertThat(play.isEnabled()).isFalse();

        exit.complete(null);
        waitUntil(() -> play.isEnabled());
    }

    @Test
    void shouldKeepPlayDisabledAfterGameExitsWhenClientIsNotReady() throws Exception {
        var exit = new CompletableFuture<Process>();
        var panel = panelWithProcess(exit);
        panel.refresh(new ClientStatus(client(UPDATED), true, null));

        play.doClick();
        panel.refresh(new ClientStatus(client(NEEDS_UPDATE), true, null));
        exit.complete(null);
        flushEventQueue();
        flushEventQueue();

        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void shouldEnablePlayForAnotherSlugWhileGameRuns() throws Exception {
        var exit = new CompletableFuture<Process>();
        var panel = panelWithProcess(exit);
        panel.refresh(new ClientStatus(client(UPDATED), true, null));

        play.doClick();
        assertThat(play.isEnabled()).isFalse();

        panel.refresh(new ClientStatus(new Client("other", "1.2.3", UPDATED, Path.of(".")), true, null));
        assertThat(play.isEnabled()).isTrue();

        // Back to the running client
        panel.refresh(new ClientStatus(client(UPDATED), true, null));
        assertThat(play.isEnabled()).isFalse();
    }

    @Test
    void shouldNotTrackProcessWhenActionFails() {
        var panel = new PlayPanel(options -> {
            throw new IllegalStateException("boom");
        }, () -> runnerOptions(TEST_SLUG));
        play = getButton(panel, "play");
        panel.refresh(new ClientStatus(client(UPDATED), true, null));

        assertThatThrownBy(() -> play.doClick()).isInstanceOf(IllegalStateException.class);

        assertThat(play.isEnabled()).isTrue();
    }

    private PlayPanel panelWithProcess(CompletableFuture<Process> exit) {
        Process process = Mockito.mock(Process.class);
        Mockito.when(process.onExit()).thenReturn(exit);
        var panel = new PlayPanel(options -> process, () -> runnerOptions(TEST_SLUG));
        play = getButton(panel, "play");
        return panel;
    }

    private static RunnerOptions runnerOptions(String slug) {
        return new RunnerOptions(slug, null, false, new LinuxOptions(false, false, null,
            new RunnerOptions.ProtonOptions(false, false), new RunnerOptions.WineOptions(CursorSize.DEFAULT)));
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            flushEventQueue();
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static void flushEventQueue() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
        });
    }

    private PlayPanel initComponents() {
        PlayPanel playPanel = new PlayPanel(options -> null, () -> null);
        play = getButton(playPanel, "play");
        return playPanel;
    }

    private static Client client(Client.Status updated) {
        return new Client(TEST_SLUG, "1.2.3", updated, Path.of("."));
    }
}
