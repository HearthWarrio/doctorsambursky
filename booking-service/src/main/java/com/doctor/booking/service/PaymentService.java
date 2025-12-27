package com.doctor.booking.service;

import com.doctor.booking.dto.PaymentCallbackDTO;
import com.doctor.booking.entity.AppointmentStatus;
import com.doctor.booking.repository.AppointmentRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RestTemplate rest;
    private final AppointmentRepository appointmentRepository;

    @Value("${tinkoff.terminalKey}") private String terminalKey;
    @Value("${tinkoff.password}")    private String password;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitResponse {
        @JsonProperty("Success")
        private boolean success;
        @JsonProperty("PaymentId")
        private String paymentId;
        @JsonProperty("PaymentURL")
        private String paymentUrl;
    }

    public InitResponse initPayment(Long orderId, int amountKopecks) {
        Map<String, Object> req = new HashMap<>();
        req.put("TerminalKey", terminalKey);
        req.put("Password", password);
        req.put("Amount", amountKopecks);
        req.put("OrderId", orderId.toString());
        req.put("Description", "Предоплата за приём");
        req.put("SuccessURL", "https://doctorsambursky.ru/success");
        req.put("FailURL", "https://doctorsambursky.ru/fail");

        InitResponse resp = rest.postForObject("https://securepay.tinkoff.ru/v2/Init", req, InitResponse.class);
        if (resp == null || !resp.isSuccess()) {
            throw new IllegalStateException("Ошибка инициализации платежа");
        }
        return resp;
    }

    public void refundPayment(String paymentId, int amountKopecks) {
        Map<String, Object> req = new HashMap<>();
        req.put("TerminalKey", terminalKey);
        req.put("PaymentId", paymentId);
        req.put("Amount", amountKopecks);
        req.put("Password", password);

        rest.postForObject("https://securepay.tinkoff.ru/v2/Cancel", req, Map.class);
    }

    public void handleCallback(PaymentCallbackDTO dto) {
        appointmentRepository.findByPaymentId(dto.getPaymentId()).ifPresent(a -> {
            if ("CONFIRMED".equalsIgnoreCase(dto.getStatus())) {
                a.setStatus(AppointmentStatus.CONFIRMED);
                a.setPaidAmount(1000 * 100);
                a.setUpdatedAt(LocalDateTime.now());
                appointmentRepository.save(a);
            } else if ("CANCELED".equalsIgnoreCase(dto.getStatus()) || "REJECTED".equalsIgnoreCase(dto.getStatus())) {
                a.setStatus(AppointmentStatus.CANCELLED);
                a.setUpdatedAt(LocalDateTime.now());
                appointmentRepository.save(a);
                refundPayment(dto.getPaymentId(), 1000 * 100);
            }
        });
    }
}