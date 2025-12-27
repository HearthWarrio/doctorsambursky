package com.doctor.booking.web;

import com.doctor.booking.dto.AppointmentDTO;
import com.doctor.booking.dto.AvailableSlotDTO;
import com.doctor.booking.dto.BookingRequest;
import com.doctor.booking.dto.PaymentResponseDTO;
import com.doctor.booking.service.AppointmentService;
import com.doctor.booking.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
@RequiredArgsConstructor
public class BookingController {
    private final AppointmentService svc;

    @PostMapping("/appointments")
    public ResponseEntity<PaymentResponseDTO> book(@Valid @RequestBody BookingRequest req) {
        return ResponseEntity.ok(svc.book(req));
    }

    @GetMapping("/appointments/available")
    public ResponseEntity<List<AvailableSlotDTO>> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "60") int stepMinutes
    ) {
        return ResponseEntity.ok(svc.getAvailable(from, to, stepMinutes));
    }

    @GetMapping("/appointments/{date}")
    public ResponseEntity<List<AppointmentDTO>> byDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date
    ) {
        return ResponseEntity.ok(
                svc.listByDay(date.atStartOfDay().plusHours(0))
        );
    }
}
