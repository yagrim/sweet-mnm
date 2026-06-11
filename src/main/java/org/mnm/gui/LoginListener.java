package org.mnm.gui;

interface LoginListener {

    void loginStart();

    // TODO Rename to Login Successful
    void loginDone(ClientStatus client);

    void logoutDone();
}
