package org.mnm.events;

import org.mnm.gui.ClientStatus;

public interface RepairListener extends EventListener {

    void repairStart();

    void repairDone(ClientStatus client);

}
