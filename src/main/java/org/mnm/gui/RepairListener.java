package org.mnm.gui;

/**
 * Handles events related to an Installation or Repair.
 */
interface RepairListener {

    void repairStart();

    void repairDone(ClientStatus client);

}
