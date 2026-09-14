package org.mnm.api;

import java.util.List;

public interface VerificationCodeSupplier {

    /**
     * Returns the obtained code or null if the process is canceled.
     *
     * @throws RuntimeException
     */
    String getVerificationCode(String method, List<String> methods, String challengeToken);

}
