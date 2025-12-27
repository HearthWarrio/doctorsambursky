package com.doctor.notify.integration.booking;

public class BadRequestException extends BookingClientException {
    public BadRequestException(String message) {
        super(message);
    }
}