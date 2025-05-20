package com.doctor.booking.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentDTO {
    private Long id;
    private String patientName;
    private LocalDateTime appointmentTime;
    private String status;
}
