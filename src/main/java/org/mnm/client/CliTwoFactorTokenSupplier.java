package org.mnm.client;

import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mnm.api.ApiConnector;
import org.mnm.api.TokenSupplier;
import org.mnm.config.Messages;

public class CliTwoFactorTokenSupplier implements TokenSupplier {

    private static final Logger logger = LoggerFactory.getLogger(CliTwoFactorTokenSupplier.class);

    private final ApiConnector apiConnector;

    public CliTwoFactorTokenSupplier(ApiConnector apiConnector) {
        this.apiConnector = apiConnector;
    }

    @Override
    public String getToken(String method, List<String> methods, String challengeToken) {

        if ("email".contains(method)) {
            logger.info("Authentication method: {}", method);
            System.out.println(Messages.TwoFactorAuthentication.EMAIL_INSTRUCTIONS);
            System.out.print("Code: ");
            String line = readLine();
            String token = apiConnector.twoFactorAuthentication(method, line, challengeToken);
            System.out.println("Successfully authenticated");
            return token;
        } else {
            throw new RuntimeException("Unsupported authentication method: " + method);
        }
    }


    private String readLine() {
        Scanner s = new Scanner(System.in);
        String line = s.nextLine().trim();

        s.close();
        return line;
    }
}
