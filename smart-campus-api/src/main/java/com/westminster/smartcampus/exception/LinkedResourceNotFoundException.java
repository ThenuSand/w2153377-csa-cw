package com.westminster.smartcampus.exception;

/**
 * Thrown when a request body references another resource (e.g. a
 * roomId on a new sensor) that does not exist. Mapped to HTTP 422.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String referenceType;
    private final String referenceId;

    public LinkedResourceNotFoundException(String referenceType, String referenceId) {
        super("Referenced " + referenceType + " with id '" + referenceId + "' was not found.");
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
}
