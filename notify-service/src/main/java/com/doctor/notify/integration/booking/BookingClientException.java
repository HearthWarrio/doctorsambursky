package com.doctor.notify.integration.booking;

public class BookingClientException extends RuntimeException {
    public BookingClientException(String message) {
        super(message);
    }

    public BookingClientException(String message, Throwable cause) {
        super(message, cause);
    }
}