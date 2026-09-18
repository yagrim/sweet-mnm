package org.mnm.api;

import java.util.List;

public interface TokenSupplier {

    String getToken(String method, List<String> methods, String challengeToken);

}
