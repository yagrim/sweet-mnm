package org.mnm.events;

public interface RepairFilesListener extends EventListener {

    void filesToInstall(int filesCount);

    void fileInstalled();
}
