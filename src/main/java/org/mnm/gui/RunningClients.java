package org.mnm.gui;

import javax.swing.SwingUtilities;
import java.util.HashSet;
import java.util.Set;

import org.mnm.events.ClientEventHandler;

/**
 * Registers client slugs of clients that are currently running.
 * <p>
 * A second start in the same Wine prefix waits for the first game and then starts again.
 * So the UI must not start a client that already runs.
 * <p>
 * Only the Event Dispatch Thread can use this class. It is not thread-safe.
 * The process exit callback runs on the EDT because of that.
 */
class RunningClients {

    private final Set<String> slugs = new HashSet<>();
    private final ClientEventHandler eventHandler;

    RunningClients(ClientEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * Marks the slug as running until the process ends.
     */
    void track(String slug, Process process) {
        slugs.add(slug);
        eventHandler.clientStarted(slug);
        // Wrap with invokeLater because UI updates need to run in Swing's Event Dispatch Thread
        process.onExit().thenRun(() -> SwingUtilities.invokeLater(() -> {
            slugs.remove(slug);
            eventHandler.clientStopped(slug);
        }));
    }

    boolean isRunning(String slug) {
        return slugs.contains(slug);
    }

}
