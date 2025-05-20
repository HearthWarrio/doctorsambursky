package com.doctor.booking.web;

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

    @PostMapping("/callback")
    public ResponseEntity<String> callback(@RequestBody PaymentService.CallbackPayload payload) {
        if ("CONFIRMED".equalsIgnoreCase(payload.Status())) {
            paymentService.handleCallback(payload);
        }
        return ResponseEntity.ok("OK");
    }
}
