package com.doctor.booking.web;

import com.doctor.booking.dto.*;
import com.doctor.booking.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bot/appointments")
@Validated
@RequiredArgsConstructor
public class BotAppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentDTO> create(@Valid @RequestBody BotCreateAppointmentRequestDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createFromBot(req));
    }

    @PostMapping("/{id}/doctor-action")
    public ResponseEntity<AppointmentDTO> doctorAction(@PathVariable("id") long id,
                                                       @Valid @RequestBody DoctorActionRequestDTO req) {
        return ResponseEntity.ok(appointmentService.doctorAction(id, req));
    }

    @PostMapping("/{id}/patient-reschedule")
    public ResponseEntity<AppointmentDTO> patientReschedule(@PathVariable("id") long id,
                                                            @Valid @RequestBody PatientRescheduleRequestDTO req) {
        return ResponseEntity.ok(appointmentService.patientReschedule(id, req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> get(@PathVariable("id") long id) {
        return ResponseEntity.ok(appointmentService.getForBot(id));
    }
}