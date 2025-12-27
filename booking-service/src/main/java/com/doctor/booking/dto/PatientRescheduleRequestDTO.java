package com.doctor.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientRescheduleRequestDTO {

    @NotBlank
    private String requestedTime;
}