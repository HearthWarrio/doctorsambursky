package com.doctor.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final RestTemplate rest;
    @Value("${tinkoff.terminalKey}") private String terminalKey;
    @Value("${tinkoff.password}")    private String password;

    public static class InitResponse {
        public boolean Success;
        public String PaymentId;
        public String PaymentURL;
    }

    public InitResponse initPayment(Long orderId, int amountKopecks) {
        var req = new HashMap<String, Object>();
        req.put("TerminalKey", terminalKey);
        req.put("Password", password);
        req.put("Amount", amountKopecks);
        req.put("OrderId", orderId.toString());
        req.put("Description", "Предоплата за приём");
        req.put("SuccessURL", "https://doctorsambursky.ru/success");
        req.put("FailURL",    "https://doctorsambursky.ru/fail");
        InitResponse resp = rest.postForObject(
                "https://securepay.tinkoff.ru/v2/Init", req, InitResponse.class
        );
        if (resp == null || !resp.Success) throw new RuntimeException("Ошибка платежа");
        return resp;
    }

    public record CallbackPayload(String Status, String PaymentId) {}

    public void handleCallback(CallbackPayload pl) {
        // статус CONFIRMED → дальнейшая логика
    }
}
