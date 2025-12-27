package com.doctor.notify.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageNotificationDTO {
    @NotNull
    private Long chatId;

    @NotBlank
    private String text;
}