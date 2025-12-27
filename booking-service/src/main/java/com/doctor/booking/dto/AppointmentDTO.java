package com.doctor.booking.dto;

import com.doctor.booking.entity.AppointmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentDTO {
    private Long id;

    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private String patientAddress;
    private String patientTelegramUsername;
    private String patientWhatsappNumber;
    private Long patientTelegramChatId;

    private LocalDateTime appointmentTime;
    private AppointmentStatus status;

    private String declineReason;
    private LocalDateTime rescheduleProposedTime;
    private LocalDateTime doctorDecisionDeadlineAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String paymentId;
    private Integer paidAmount;

    private String cancelReason;
}