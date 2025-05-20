package com.doctor.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentDto {
    private Long id;
    private String patientName;
    private LocalDateTime dateTime;
    private String status;   // PENDING, CONFIRMED, CANCELLED, COMPLETED
    private Boolean paid;
    private Integer paidAmount;
}
