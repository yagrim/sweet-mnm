package org.mnm.events;

public interface RepairFilesListener {

    void filesToInstall(int filesCount);

    void fileInstalled();
}
