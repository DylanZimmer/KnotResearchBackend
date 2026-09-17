package com.knots.backend.services;

public class SageMathServiceException extends RuntimeException {

    public SageMathServiceException(String message) {
        super(message);
    }

    public SageMathServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
