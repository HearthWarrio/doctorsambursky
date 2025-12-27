package com.doctor.notify.integration.booking;

import com.doctor.notify.integration.booking.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RestBookingClient implements BookingClient {

    private final RestTemplate rest;

    @Value("${booking.base-url}")
    private String baseUrl;

    @Value("${internal.auth.token}")
    private String internalToken;

    @Value("${internal.auth.header:X-Internal-Token}")
    private String internalHeader;

    @Override
    public AppointmentDTO createAppointment(BotCreateAppointmentRequestDTO dto) {
        return post("/api/bot/appointments", dto, AppointmentDTO.class);
    }

    @Override
    public AppointmentDTO doctorAction(long id, DoctorActionRequestDTO dto) {
        return post("/api/bot/appointments/" + id + "/doctor-action", dto, AppointmentDTO.class);
    }

    @Override
    public AppointmentDTO patientReschedule(long id, PatientRescheduleRequestDTO dto) {
        return post("/api/bot/appointments/" + id + "/patient-reschedule", dto, AppointmentDTO.class);
    }

    @Override
    public AppointmentDTO getAppointment(long id) {
        try {
            HttpHeaders h = new HttpHeaders();
            String headerName = (internalHeader == null || internalHeader.isBlank())
                    ? "X-Internal-Token"
                    : internalHeader;

            if (internalToken != null && !internalToken.isBlank()) {
                h.set(headerName, internalToken);
            }
            HttpEntity<Void> e = new HttpEntity<>(h);
            ResponseEntity<AppointmentDTO> r = rest.exchange(baseUrl + "/api/bot/appointments/" + id, HttpMethod.GET, e, AppointmentDTO.class);
            return r.getBody();
        } catch (HttpStatusCodeException ex) {
            throw mapHttp(ex);
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("booking-service недоступен", ex);
        }
    }

    private <T> T post(String path, Object body, Class<T> type) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            String headerName = (internalHeader == null || internalHeader.isBlank())
                    ? "X-Internal-Token"
                    : internalHeader;

            if (internalToken != null && !internalToken.isBlank()) {
                h.set(headerName, internalToken);
            }
            HttpEntity<Object> e = new HttpEntity<>(body, h);
            ResponseEntity<T> r = rest.exchange(baseUrl + path, HttpMethod.POST, e, type);
            return r.getBody();
        } catch (HttpStatusCodeException ex) {
            throw mapHttp(ex);
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("booking-service недоступен", ex);
        }
    }

    private BookingClientException mapHttp(HttpStatusCodeException ex) {
        if (ex.getStatusCode() == HttpStatus.CONFLICT) {
            return new SlotUnavailableException("Время занято или действие невозможно");
        }
        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new BadRequestException("Неверные данные запроса");
        }
        return new BookingClientException("Ошибка booking-service: " + ex.getStatusCode().value());
    }
}