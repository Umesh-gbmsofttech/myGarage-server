package com.gbm.app.exception;

import org.springframework.http.HttpStatus;

public class UpstreamServiceException extends RuntimeException {

    private final HttpStatus status;

    public UpstreamServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
