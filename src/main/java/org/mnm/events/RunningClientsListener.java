package org.mnm.events;

public interface RunningClientsListener extends EventListener {

    void clientStarted(String slug);

    void clientStopped(String slug);

}
