package com.doctor.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackDTO {
    /** Статус платежа: CONFIRMED, CANCELED, REJECTED и т.п. */
    @JsonProperty("Status")
    private String status;

    /** Внутренний ID платежа в Tinkoff */
    @JsonProperty("PaymentId")
    private String paymentId;
}
