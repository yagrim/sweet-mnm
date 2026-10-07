package org.mnm.events;

/**
 * Listener to monitor final files data processing.
 */
public interface AssemblyListener extends EventListener {

    void dataToAssemble(long bytes);

    void dataAssembled(long bytes);
}
