package com.doctor.notify.integration.booking.dto;

import lombok.Data;

@Data
public class BotCreateAppointmentRequestDTO {
    private String patientName;
    private String phone;
    private String address;
    private String telegramUsername;
    private String whatsappNumber;
    private String email;
    private Long patientTelegramChatId;
    private String requestedTime;
}