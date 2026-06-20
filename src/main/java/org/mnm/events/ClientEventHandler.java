package org.mnm.events;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mnm.gui.ClientStatus;

public class ClientEventHandler
    implements LoginListener, RepairListener, Refreshable, FilesValidationListener, RepairFilesListener {

    private static final ClientEventHandler instance = new ClientEventHandler();

    private final Set<EventListener> listeners;

    private ClientEventHandler() {
        synchronized (this) {
            listeners = Collections.synchronizedSet(new HashSet<>());
        }
    }

    public static ClientEventHandler getInstance() {
        return instance;
    }

    public void register(EventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void loginStart() {
        listeners.stream()
            .filter(l -> l instanceof LoginListener)
            .forEach(listener -> ((LoginListener) listener).loginStart());
    }

    @Override
    public void loginDone(ClientStatus client) {
        listeners.stream()
            .filter(l -> l instanceof LoginListener)
            .forEach(listener -> ((LoginListener) listener).loginDone(client));
    }

    @Override
    public void logoutDone() {
        listeners.stream()
            .filter(l -> l instanceof LoginListener)
            .forEach(listener -> ((LoginListener) listener).logoutDone());
    }

    @Override
    public void repairStart() {
        listeners.stream()
            .filter(l -> l instanceof RepairListener)
            .forEach(listener -> ((RepairListener) listener).repairStart());
    }

    @Override
    public void repairDone(ClientStatus client) {
        listeners.stream()
            .filter(l -> l instanceof RepairListener)
            .forEach(listener -> ((RepairListener) listener).repairDone(client));
    }

    @Override
    public void refresh(ClientStatus client) {
        listeners.stream()
            .filter(l -> l instanceof Refreshable)
            .forEach(listener -> ((Refreshable) listener).refresh(client));
    }

    @Override
    public void validationStart(int filesCount) {
        listeners.stream()
            .filter(l -> l instanceof FilesValidationListener)
            .forEach(listener -> ((FilesValidationListener) listener).validationStart(filesCount));
    }

    @Override
    public void fileValidated() {
        listeners.stream()
            .filter(l -> l instanceof FilesValidationListener)
            .forEach(listener -> ((FilesValidationListener) listener).fileValidated());
    }

    @Override
    public void filesToInstall(int filesCount) {
        listeners.stream()
            .filter(l -> l instanceof RepairFilesListener)
            .forEach(listener -> ((RepairFilesListener) listener).filesToInstall(filesCount));
    }

    @Override
    public void fileInstalled() {
        listeners.stream()
            .filter(l -> l instanceof RepairFilesListener)
            .forEach(listener -> ((RepairFilesListener) listener).fileInstalled());
    }

    public void clear() {
        listeners.clear();
    }

}
