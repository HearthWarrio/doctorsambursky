package com.doctor.notify.integration.booking;

public class SlotUnavailableException extends BookingClientException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}