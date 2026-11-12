package org.mnm.cli;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.ApiConnector;
import org.mnm.api.TokenSupplier;
import org.mnm.config.Messages;

public class CliTwoFactorTokenSupplier implements TokenSupplier {

    private static final Logger logger = LoggerFactory.getLogger(CliTwoFactorTokenSupplier.class);

    private final ApiConnector apiConnector;
    private final LineReader lineReader;

    public CliTwoFactorTokenSupplier(ApiConnector apiConnector, LineReader lineReader) {
        this.apiConnector = apiConnector;
        this.lineReader = lineReader;
    }

    @Override
    public String getToken(String method, List<String> methods, String challengeToken) {

        logger.info("Authentication method: {}", method);
        System.out.println(Messages.TwoFactorAuthentication.instructions(method));
        System.out.print("Code: ");
        String line = lineReader.readLine();
        String token = apiConnector.twoFactorAuthentication(method, line, challengeToken);
        System.out.println("Successfully authenticated");
        return token;
    }

}
