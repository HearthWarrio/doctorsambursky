package com.doctor.booking.exception;

public class BotUnavailableException extends RuntimeException {
    public BotUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}