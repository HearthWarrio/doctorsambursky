package com.doctor.notify.integration.booking;

public class ServiceUnavailableException extends BookingClientException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}