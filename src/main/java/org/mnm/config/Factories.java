package org.mnm.config;

import org.mnm.api.ApiConnector;
import org.mnm.api.RestClient;

public class Factories {

    public static ApiConnector apiConnector() {
        return new ApiConnector(new RestClient());
    }
}
