package org.mnm.events;

import org.mnm.gui.ClientStatus;

public interface Refreshable extends EventListener {

    void refresh(ClientStatus client);

}
