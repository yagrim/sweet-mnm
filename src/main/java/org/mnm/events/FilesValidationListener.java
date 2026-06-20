package org.mnm.events;

public interface FilesValidationListener {

    void validationStart(int filesCount);

    void fileValidated();

}
