package org.mnm.client;


public record Client(
        String slug,
        String version,
        Status status,
        String path
) {

    public enum Status {
        INSTALLING, COMPLETED
    }
}