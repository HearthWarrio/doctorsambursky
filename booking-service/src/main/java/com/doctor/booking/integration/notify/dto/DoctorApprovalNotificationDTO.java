package com.doctor.booking.integration.notify.dto;

import lombok.Data;

@Data
public class DoctorApprovalNotificationDTO {
    private Long doctorChatId;
    private Long appointmentId;
    private String text;
}