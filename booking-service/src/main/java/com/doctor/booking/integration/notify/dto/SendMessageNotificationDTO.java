package com.doctor.booking.integration.notify.dto;

import lombok.Data;

@Data
public class SendMessageNotificationDTO {
    private Long chatId;
    private String text;
}