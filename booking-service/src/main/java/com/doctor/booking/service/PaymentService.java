package com.doctor.booking.service;

import com.doctor.booking.dto.PaymentCallbackDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final RestTemplate rest;
    private final AppointmentService appointmentService;

    @Value("${tinkoff.terminalKey}") private String terminalKey;
    @Value("${tinkoff.password}")    private String password;

    /** Ответ от Tinkoff Init */
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

    /**
     * Инициация предоплаты.
     */
    public InitResponse initPayment(Long orderId, int amountKopecks) {
        Map<String, Object> req = new HashMap<>();
        req.put("TerminalKey", terminalKey);
        req.put("Password", password);
        req.put("Amount", amountKopecks);
        req.put("OrderId", orderId.toString());
        req.put("Description", "Предоплата за приём");
        req.put("SuccessURL", "https://doctorsambursky.ru/success");
        req.put("FailURL",    "https://doctorsambursky.ru/fail");

        InitResponse resp = rest.postForObject(
                "https://securepay.tinkoff.ru/v2/Init",
                req,
                InitResponse.class
        );
        if (resp == null || !resp.isSuccess()) {
            throw new IllegalStateException("Ошибка инициализации платежа");
        }
        return resp;
    }

    /**
     * Возврат средств.
     */
    public void refundPayment(String paymentId, int amountKopecks) {
        Map<String, Object> req = new HashMap<>();
        req.put("TerminalKey", terminalKey);
        req.put("PaymentId", paymentId);
        req.put("Amount", amountKopecks);
        req.put("Password", password);

        rest.postForObject(
                "https://securepay.tinkoff.ru/v2/Cancel",
                req,
                Map.class
        );
    }

    /**
     * Обработка колбэка от Tinkoff.
     */
    public void handleCallback(PaymentCallbackDTO dto) {
        if ("CONFIRMED".equalsIgnoreCase(dto.getStatus())) {
            appointmentService.confirmPayment(dto.getPaymentId());
        } else if ("CANCELED".equalsIgnoreCase(dto.getStatus())
                || "REJECTED".equalsIgnoreCase(dto.getStatus())) {
            appointmentService.cancelByPaymentId(dto.getPaymentId());
            refundPayment(dto.getPaymentId(), 1000 * 100);
        }
    }
}
