package org.mnm.gui;

import org.mnm.config.Client;

/**
 * Handles events related to an Installation or Repair.
 */
public interface RepairListener {

    void repairStart();

    void repairDone(Client client);

}
