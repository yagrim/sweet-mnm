package org.mnm.events;

public interface FilesValidationListener extends EventListener {

    void validationStart(int filesCount);

    void fileValidated();

}
