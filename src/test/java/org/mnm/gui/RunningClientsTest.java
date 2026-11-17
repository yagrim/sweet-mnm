package org.mnm.gui;

import javax.swing.SwingUtilities;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.mnm.events.ClientEventHandler;

import static org.assertj.core.api.Assertions.assertThat;

class RunningClientsTest {

    private final ClientEventHandler eventHandler = Mockito.mock(ClientEventHandler.class);
    private final RunningClients runningClients = new RunningClients(eventHandler);

    @Test
    void shouldNotBeRunningByDefault() {
        assertThat(runningClients.isRunning("mnm")).isFalse();
        assertThat(runningClients.isRunning(null)).isFalse();
    }

    @Test
    void shouldBeRunningAfterTrack() {
        runningClients.track("mnm", processExiting(new CompletableFuture<>()));

        assertThat(runningClients.isRunning("mnm")).isTrue();
        assertThat(runningClients.isRunning("other")).isFalse();
    }

    @Test
    void shouldStopRunningWhenProcessExits() throws Exception {
        var exit = new CompletableFuture<Process>();
        runningClients.track("mnm", processExiting(exit));

        exit.complete(null);
        flushEventQueue();

        assertThat(runningClients.isRunning("mnm")).isFalse();
    }

    @Test
    void shouldKeepOtherSlugsRunningWhenOneExits() throws Exception {
        var exit = new CompletableFuture<Process>();
        runningClients.track("mnm", processExiting(exit));
        runningClients.track("ptr", processExiting(new CompletableFuture<>()));

        exit.complete(null);
        flushEventQueue();

        assertThat(runningClients.isRunning("mnm")).isFalse();
        assertThat(runningClients.isRunning("ptr")).isTrue();
    }

    @Test
    void shouldFireEventsWithSlugOnStartAndOnExit() throws Exception {
        var exit = new CompletableFuture<Process>();

        runningClients.track("mnm", processExiting(exit));
        Mockito.verify(eventHandler).clientStarted("mnm");
        Mockito.verify(eventHandler, Mockito.never()).clientStopped(Mockito.any());

        exit.complete(null);
        flushEventQueue();
        Mockito.verify(eventHandler).clientStopped("mnm");
    }

    @Test
    void shouldUpdateStateOnEventDispatchThread() throws Exception {
        var exit = new CompletableFuture<Process>();
        var onEdt = new CompletableFuture<Boolean>();
        Mockito.doAnswer(invocation -> onEdt.complete(SwingUtilities.isEventDispatchThread()))
            .when(eventHandler).clientStopped("mnm");
        runningClients.track("mnm", processExiting(exit));

        // Complete from a thread that is not the EDT
        Thread.ofPlatform().start(() -> exit.complete(null)).join();
        flushEventQueue();

        assertThat(onEdt.get()).isTrue();
    }

    private static Process processExiting(CompletableFuture<Process> exit) {
        Process process = Mockito.mock(Process.class);
        Mockito.when(process.onExit()).thenReturn(exit);
        return process;
    }

    private static void flushEventQueue() throws Exception {
        // The exit callback queues a task on the EDT. This waits for it.
        SwingUtilities.invokeAndWait(() -> {
        });
        SwingUtilities.invokeAndWait(() -> {
        });
    }
}
