package com.doctor.booking.integration.notify;

import com.doctor.booking.exception.BotUnavailableException;
import com.doctor.booking.integration.notify.dto.DoctorApprovalNotificationDTO;
import com.doctor.booking.integration.notify.dto.SendMessageNotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RestNotifyClient implements NotifyClient {

    private final RestTemplate rest;

    @Value("${notify.base-url}")
    private String baseUrl;

    @Override
    public void sendDoctorApproval(DoctorApprovalNotificationDTO dto) {
        try {
            rest.postForEntity(baseUrl + "/internal/telegram/doctor-approval", dto, Void.class);
        } catch (RestClientException e) {
            throw new BotUnavailableException("notify-service недоступен при отправке запроса врачу", e);
        }
    }

    @Override
    public void sendMessage(SendMessageNotificationDTO dto) {
        try {
            rest.postForEntity(baseUrl + "/internal/telegram/send-message", dto, Void.class);
        } catch (RestClientException e) {
            throw new BotUnavailableException("notify-service недоступен при отправке сообщения", e);
        }
    }
}