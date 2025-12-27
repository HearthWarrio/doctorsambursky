package com.doctor.booking.web;

import com.doctor.booking.dto.AppointmentDTO;
import com.doctor.booking.service.AppointmentService;
import com.doctor.booking.web.dto.CancelAppointmentRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotDoctorController {

    private final AppointmentService appointmentService;

    @GetMapping("/schedule")
    public ResponseEntity<List<AppointmentDTO>> schedule(
            @RequestParam("from") LocalDateTime from,
            @RequestParam("to") LocalDateTime to
    ) {
        return ResponseEntity.ok(appointmentService.getSchedule(from, to));
    }

    @PostMapping("/appointments/{id}/cancel")
    public ResponseEntity<AppointmentDTO> cancel(
            @PathVariable("id") long id,
            @Valid @RequestBody CancelAppointmentRequestDTO dto
    ) {
        return ResponseEntity.ok(appointmentService.cancelByDoctor(id, dto.getReason()));
    }
}