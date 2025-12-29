package com.doctor.booking.integration.notify;

import com.doctor.booking.integration.notify.dto.DoctorApprovalNotificationDTO;
import com.doctor.booking.integration.notify.dto.SendMessageNotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RestNotifyClient implements NotifyClient {

    private final RestTemplate rest;

    @Value("${notify.base-url}")
    private String baseUrl;

    @Value("${internal.auth.header:X-Internal-Token}")
    private String internalHeader;

    @Value("${internal.auth.token}")
    private String internalToken;

    @Override
    public void sendDoctorApproval(DoctorApprovalNotificationDTO dto) {
        postWithInternalAuth("/internal/telegram/doctor-approval", dto);
    }

    @Override
    public void sendMessage(SendMessageNotificationDTO dto) {
        postWithInternalAuth("/internal/telegram/send-message", dto);
    }

    private void postWithInternalAuth(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(internalHeader, internalToken);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        rest.postForEntity(baseUrl + path, entity, Void.class);
    }
}
