package org.mnm.events;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mnm.gui.ClientStatus;

public class ClientEventHandler
    implements LoginListener, RepairListener, Refreshable, FilesValidationListener, RepairFilesListener {

    private static final ClientEventHandler instance = new ClientEventHandler();

    // TODO create parent interface and convert lists into Map<Event, List<Event>>
    private final Set<LoginListener> loginListeners;
    private final Set<RepairListener> repairListeners;
    private final Set<Refreshable> refreshables;

    private final Set<FilesValidationListener> filesValidationListeners;
    private final Set<RepairFilesListener> repairFilesListeners;

    private ClientEventHandler() {
        synchronized (this) {
            loginListeners = Collections.synchronizedSet(new HashSet<>());
            repairListeners = Collections.synchronizedSet(new HashSet<>());
            refreshables = Collections.synchronizedSet(new HashSet<>());

            filesValidationListeners = Collections.synchronizedSet(new HashSet<>());
            repairFilesListeners = Collections.synchronizedSet(new HashSet<>());
        }
    }

    public static ClientEventHandler getInstance() {
        return instance;
    }

    public void register(Object listener) {
        if (listener instanceof LoginListener) {
            loginListeners.add((LoginListener) listener);
        }
        if (listener instanceof RepairListener) {
            repairListeners.add((RepairListener) listener);
        }
        if (listener instanceof Refreshable) {
            refreshables.add((Refreshable) listener);
        }
        if (listener instanceof FilesValidationListener) {
            filesValidationListeners.add((FilesValidationListener) listener);
        }
        if (listener instanceof RepairFilesListener) {
            repairFilesListeners.add((RepairFilesListener) listener);
        }
    }

    @Override
    public void loginStart() {
        loginListeners.forEach(listener -> listener.loginStart());
    }

    @Override
    public void loginDone(ClientStatus client) {
        loginListeners.forEach(listener -> listener.loginDone(client));
    }

    @Override
    public void logoutDone() {
        loginListeners.forEach(listener -> listener.logoutDone());
    }

    @Override
    public void repairStart() {
        repairListeners.forEach(listener -> listener.repairStart());
    }

    @Override
    public void repairDone(ClientStatus client) {
        repairListeners.forEach(listener -> listener.repairDone(client));
    }

    @Override
    public void refresh(ClientStatus client) {
        refreshables.forEach(refreshable -> refreshable.refresh(client));
    }

    @Override
    public void validationStart(int filesCount) {
        filesValidationListeners.forEach(listener -> listener.validationStart(filesCount));
    }

    @Override
    public void fileValidated() {
        filesValidationListeners.forEach(listener -> listener.fileValidated());
    }

    @Override
    public void filesToInstall(int filesCount) {
        repairFilesListeners.forEach(listener -> listener.filesToInstall(filesCount));
    }

    @Override
    public void fileInstalled() {
        repairFilesListeners.forEach(listener -> listener.fileInstalled());
    }

    public void clear() {
        loginListeners.clear();
        repairListeners.clear();
        refreshables.clear();
        filesValidationListeners.clear();
        repairFilesListeners.clear();
    }

}
