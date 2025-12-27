package com.doctor.notify.integration.booking.dto;

import lombok.Data;

@Data
public class DoctorActionRequestDTO {
    private DoctorActionType action;
    private String reason;
    private String proposedTime;
}