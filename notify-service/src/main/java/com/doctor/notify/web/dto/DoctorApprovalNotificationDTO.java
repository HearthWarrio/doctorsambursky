package com.doctor.notify.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DoctorApprovalNotificationDTO {
    @NotNull
    private Long doctorChatId;

    @NotNull
    private Long appointmentId;

    @NotBlank
    private String text;
}