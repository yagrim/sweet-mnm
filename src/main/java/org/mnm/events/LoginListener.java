package org.mnm.events;

import org.mnm.gui.ClientStatus;

public interface LoginListener extends EventListener {

    void loginStart();

    // TODO Rename to Login Successful
    void loginDone(ClientStatus client);

    void logoutDone();
}
