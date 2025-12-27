package com.doctor.booking.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AvailableSlotDTO {
    private LocalDateTime appointmentTime;
}
