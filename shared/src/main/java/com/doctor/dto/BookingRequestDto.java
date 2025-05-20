package com.doctor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequestDto {
    @NotBlank(message = "ФИО обязательно")
    private String name;

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Неверный формат телефона")
    private String phone;

    @Email(message = "Неверный формат email")
    private String email;

    @NotNull(message = "Дата и время обязаны быть указаны")
    private LocalDateTime dateTime;

    @NotBlank(message = "Согласие с условиями обязательно")
    private Boolean agreeTerms;

    private String telegramUsername;
    private String whatsappNumber;
}
