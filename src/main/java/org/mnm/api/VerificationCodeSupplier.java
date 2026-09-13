package org.mnm.api;

import java.util.List;

public interface VerificationCodeSupplier {

    String getVerificationCode(String method, List<String> methods, String challengeToken);

}
