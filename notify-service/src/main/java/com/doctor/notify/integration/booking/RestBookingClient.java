package com.doctor.notify.integration.booking;

import com.doctor.notify.integration.booking.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class RestBookingClient implements BookingClient {

    private static final ObjectMapper OM = new ObjectMapper();

    private final RestTemplate rest;

    private final String baseUrl;
    private final String internalToken;
    private final String internalHeader;

    public RestBookingClient(
            RestTemplate rest,
            @Value("${booking.base-url}") String baseUrl,
            @Value("${internal.auth.token}") String internalToken,
            @Value("${internal.auth.header:X-Internal-Token}") String internalHeader
    ) {
        this.rest = rest;
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
        this.internalHeader = StringUtils.hasText(internalHeader) ? internalHeader : "X-Internal-Token";
    }

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
            HttpEntity<Void> e = new HttpEntity<>(authHeaders());
            ResponseEntity<AppointmentDTO> r = rest.exchange(
                    baseUrl + "/api/bot/appointments/" + id,
                    HttpMethod.GET,
                    e,
                    AppointmentDTO.class
            );
            return r.getBody();
        } catch (HttpStatusCodeException ex) {
            throw mapHttp(ex);
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("booking-service недоступен", ex);
        }
    }

    @Override
    public List<AppointmentDTO> getSchedule(String fromIso, String toIso) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/api/bot/schedule")
                    .queryParam("from", fromIso)
                    .queryParam("to", toIso)
                    .toUriString();

            HttpEntity<Void> e = new HttpEntity<>(authHeaders());
            ResponseEntity<List<AppointmentDTO>> r = rest.exchange(
                    url,
                    HttpMethod.GET,
                    e,
                    new ParameterizedTypeReference<>() {}
            );
            return r.getBody();
        } catch (HttpStatusCodeException ex) {
            throw mapHttp(ex);
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("booking-service недоступен", ex);
        }
    }

    @Override
    public AppointmentDTO cancelAppointment(long id, CancelAppointmentRequestDTO dto) {
        return post("/api/bot/appointments/" + id + "/cancel", dto, AppointmentDTO.class);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        try {
            HttpHeaders h = authHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> e = new HttpEntity<>(body, h);
            ResponseEntity<T> r = rest.exchange(baseUrl + path, HttpMethod.POST, e, type);
            return r.getBody();
        } catch (HttpStatusCodeException ex) {
            throw mapHttp(ex);
        } catch (RestClientException ex) {
            throw new ServiceUnavailableException("booking-service недоступен", ex);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        if (StringUtils.hasText(internalToken)) {
            h.set(internalHeader, internalToken);
        }
        return h;
    }

    private BookingClientException mapHttp(HttpStatusCodeException ex) {
        String raw = ex.getResponseBodyAsString();
        String code = null;
        String msg = null;

        try {
            if (raw != null && !raw.isBlank()) {
                JsonNode root = OM.readTree(raw);
                code = textOrNull(root, "error");
                msg = textOrNull(root, "message");
            }
        } catch (Exception ignored) {
            // если тело не JSON – живём дальше
        }

        String fallback = "Ошибка booking-service: " + ex.getStatusCode().value();
        String message = (msg == null || msg.isBlank()) ? fallback : msg;

        if (ex.getStatusCode().is5xxServerError()) {
            return new ServiceUnavailableException(message, ex);
        }

        if (ex.getStatusCode() == HttpStatus.CONFLICT) {
            if ("SLOT_UNAVAILABLE".equalsIgnoreCase(code)) {
                return new SlotUnavailableException(message);
            }
            return new BookingClientException(message);
        }

        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return new BadRequestException(message);
        }

        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new BookingClientException(message);
        }

        return new BookingClientException(message);
    }

    private static String textOrNull(JsonNode root, String field) {
        if (root == null) return null;
        JsonNode n = root.get(field);
        if (n == null || n.isNull()) return null;
        String s = n.asText();
        return (s == null || s.isBlank()) ? null : s;
    }
}