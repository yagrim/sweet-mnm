package org.mnm.client;

import org.mnm.events.InstallationListener;

import java.util.concurrent.atomic.AtomicBoolean;

public class InstallationMonitor implements InstallationListener {

    private final AtomicBoolean active = new AtomicBoolean(true);

    @Override
    public void pause() {
        synchronized (active) {
            active.set(!active.get());
        }
    }

    public boolean isActive() {
        return active.get();
    }

    public void reset() {
        active.set(true);
    }
}
