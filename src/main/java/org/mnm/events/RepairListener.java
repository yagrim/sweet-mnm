package org.mnm.events;

import org.mnm.gui.ClientStatus;

public interface RepairListener {

    void repairStart();

    void repairDone(ClientStatus client);

}
