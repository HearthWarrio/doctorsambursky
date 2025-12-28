package com.doctor.notify.integration.booking.dto;

import lombok.Data;

@Data
public class AppointmentDTO {
    private Long id;
    private String status;

    private String appointmentTime;
    private String rescheduleProposedTime;

    private String patientName;
    private String phone;
    private String address;
    private Long patientTelegramChatId;

    private String declineReason;
}
