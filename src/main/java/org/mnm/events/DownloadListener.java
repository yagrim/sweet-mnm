package org.mnm.events;


public interface DownloadListener extends EventListener {

    void dataToDownload(long bytes);

    void dataDownloaded(long bytes);
}
