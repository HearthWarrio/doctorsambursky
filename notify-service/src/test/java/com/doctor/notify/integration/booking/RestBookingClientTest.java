package com.doctor.notify.integration.booking;

import com.doctor.notify.integration.booking.dto.AppointmentDTO;
import com.doctor.notify.integration.booking.dto.BotCreateAppointmentRequestDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

class RestBookingClientTest {

    @Test
    void mapConflictToSlotUnavailable() {
        RestTemplate rest = Mockito.mock(RestTemplate.class);
        RestBookingClient c = new RestBookingClient(rest);

        ReflectionTestUtils.setField(c, "baseUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(c, "internalToken", "test-token");
        ReflectionTestUtils.setField(c, "internalHeader", "X-Internal-Token");

        Mockito.doThrow(HttpClientErrorException.create(
                        HttpStatus.CONFLICT,
                        "Conflict",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ))
                .when(rest)
                .exchange(
                        Mockito.anyString(),
                        Mockito.eq(HttpMethod.POST),
                        Mockito.any(HttpEntity.class),
                        Mockito.eq(AppointmentDTO.class)
                );

        Assertions.assertThrows(SlotUnavailableException.class, () -> {
            BotCreateAppointmentRequestDTO dto = new BotCreateAppointmentRequestDTO();
            c.createAppointment(dto);
        });
    }
}
