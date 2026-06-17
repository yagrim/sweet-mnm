package org.mnm.events;

import org.mnm.gui.ClientStatus;

interface LoginListener {

    void loginStart();

    // TODO Rename to Login Successful
    void loginDone(ClientStatus client);

    void logoutDone();
}
