package com.doctor.booking.web;

import com.doctor.booking.dto.PaymentCallbackDTO;
import com.doctor.booking.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentCallbackController {
    private final PaymentService paymentService;

    /**
     * Приходит JSON от Tinkoff вида:
     * {
     *   "Status": "CONFIRMED",
     *   "PaymentId": "12345678",
     *   ...
     * }
     */
    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@RequestBody PaymentCallbackDTO payload) {
        // Вся логика разбора статуса внутри paymentService
        paymentService.handleCallback(payload);
        return ResponseEntity.ok().build();
    }
}
