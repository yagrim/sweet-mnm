package org.mnm.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ClientEventHandler
    implements LoginListener, RepairListener, Refreshable {

    private static final ClientEventHandler instance = new ClientEventHandler();

    private final List<LoginListener> loginListeners;
    private final List<RepairListener> repairListeners;
    private final List<Refreshable> refreshables;

    private ClientEventHandler() {
        synchronized (this) {
            loginListeners = Collections.synchronizedList(new ArrayList<>());
            repairListeners = Collections.synchronizedList(new ArrayList<>());
            refreshables = Collections.synchronizedList(new ArrayList<>());
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
}
