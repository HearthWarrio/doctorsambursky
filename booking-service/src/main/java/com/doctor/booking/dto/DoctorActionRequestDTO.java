package com.doctor.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DoctorActionRequestDTO {

    @NotNull
    private DoctorActionType action;

    private String reason;

    private String proposedTime;
}