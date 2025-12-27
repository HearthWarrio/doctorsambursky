package com.doctor.booking.entity;

public enum AppointmentStatus {
    PENDING,
    PENDING_DOCTOR,
    CONFIRMED,
    DECLINED,
    RESCHEDULE_REQUESTED,
    RESCHEDULE_PROPOSED,
    CANCELLED,
    COMPLETED
}