package com.doctor.booking.dto;

import lombok.Data;

@Data
public class PaymentResponseDTO {
    private Long appointmentId;
    private String paymentUrl;
}
