package org.mnm.config;


public record Client(
    String slug,
    String version,
    Status status,
    String path
) {

    public enum Status {
        INSTALLING, REPAIRING, COMPLETED
    }
}
