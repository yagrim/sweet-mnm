package org.mnm.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.mnm.gui.ClientStatus;

public class ClientEventHandler
    implements LoginListener, RepairListener, Refreshable, FilesValidationListener, RepairFilesListener {

    private static final ClientEventHandler instance = new ClientEventHandler();

    private final List<LoginListener> loginListeners;
    private final List<RepairListener> repairListeners;
    private final List<Refreshable> refreshables;

    private final List<FilesValidationListener> filesValidationListeners;
    private final List<RepairFilesListener> repairFilesListeners;

    private ClientEventHandler() {
        synchronized (this) {
            loginListeners = Collections.synchronizedList(new ArrayList<>());
            repairListeners = Collections.synchronizedList(new ArrayList<>());
            refreshables = Collections.synchronizedList(new ArrayList<>());

            filesValidationListeners = Collections.synchronizedList(new ArrayList<>());
            repairFilesListeners = Collections.synchronizedList(new ArrayList<>());
        }
    }

    public static ClientEventHandler getInstance() {
        return instance;
    }

    public void register(LoginListener listener) {
        loginListeners.add(listener);
    }

    public void register(RepairListener listener) {
        repairListeners.add(listener);
    }

    public void register(Refreshable listener) {
        refreshables.add(listener);
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

    // TODO move out
    class FilesCounter {

        private final AtomicInteger count;

        public FilesCounter(int count) {
            this.count = new AtomicInteger(count);
        }

        public void increment() {
            this.count.decrementAndGet();
        }

        public void reset() {
            this.count.set(0);
        }

        public int get() {
            return this.count.get();
        }
    }
}
