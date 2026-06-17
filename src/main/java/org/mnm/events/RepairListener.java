package org.mnm.events;

import org.mnm.gui.ClientStatus;

interface RepairListener {

    void repairStart();

    void repairDone(ClientStatus client);

}
