package com.doctor.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BotCreateAppointmentRequestDTO {

    @NotBlank
    private String patientName;

    @NotBlank
    private String phone;

    @NotBlank
    private String address;

    private String telegramUsername;

    private String whatsappNumber;

    private String email;

    @NotNull
    private Long patientTelegramChatId;

    @NotBlank
    private String requestedTime;
}