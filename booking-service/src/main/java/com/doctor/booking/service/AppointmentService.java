package com.doctor.booking.service;

import com.doctor.booking.dto.AppointmentDTO;
import com.doctor.booking.dto.AvailableSlotDTO;
import com.doctor.booking.dto.BookingRequest;
import com.doctor.booking.dto.PaymentResponseDTO;
import com.doctor.booking.entity.Appointment;
import com.doctor.booking.entity.Appointment.Status;
import com.doctor.booking.entity.Patient;
import com.doctor.booking.mapper.AppointmentMapper;
import com.doctor.booking.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final PatientService patientService;
    private final PaymentService paymentService;
    private final AppointmentRepository apptRepo;
    private final AppointmentMapper mapper;

    /**
     * Бронирование + инициация платежа.
     */
    @Transactional
    public PaymentResponseDTO book(BookingRequest req) {
        Patient p = patientService.findOrCreate(req.getName(), req.getPhone(), req.getEmail());
        if (apptRepo.existsByAppointmentTime(req.getAppointmentTime())) {
            throw new IllegalArgumentException("Время занято");
        }
        Appointment a = new Appointment();
        a.setPatient(p);
        a.setAppointmentTime(req.getAppointmentTime());
        a.setStatus(Appointment.Status.PENDING);
        a.setCreatedAt(LocalDateTime.now());
        a = apptRepo.save(a);

        var init = paymentService.initPayment(a.getId(), 1000 * 100);
        a.setPaymentId(init.getPaymentId());
        apptRepo.save(a);

        var dto = new PaymentResponseDTO();
        dto.setAppointmentId(a.getId());
        dto.setPaymentUrl(init.getPaymentUrl());
        return dto;
    }

    @Transactional
    public List<AvailableSlotDTO> getAvailable(LocalDateTime from, LocalDateTime to, int stepMinutes) {
        var slots = new ArrayList<LocalDateTime>();
        for (var t = from; !t.isAfter(to); t = t.plusMinutes(stepMinutes)) {
            slots.add(t);
        }
        var busy = apptRepo.findByAppointmentTimeBetween(from, to)
                .stream()
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());
        return slots.stream()
                .filter(s -> !busy.contains(s))
                .map(s -> {
                    var slot = new AvailableSlotDTO();
                    slot.setAppointmentTime(s);
                    return slot;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirmPayment(String paymentId) {
        apptRepo.findAll().stream()
                .filter(a -> paymentId.equals(a.getPaymentId()))
                .findFirst()
                .ifPresent(a -> {
                    a.setStatus(Status.CONFIRMED);
                    a.setPaidAmount(1000 * 100);
                    apptRepo.save(a);
                });
    }

    /** Нужен для handleCallback */
    @Transactional
    public void cancelByPaymentId(String paymentId) {
        apptRepo.findAll().stream()
                .filter(a -> paymentId.equals(a.getPaymentId()))
                .findFirst()
                .ifPresent(a -> {
                    a.setStatus(Status.CANCELLED);
                    apptRepo.save(a);
                });
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void cancelUnpaid() {
        var cutoff = LocalDateTime.now().minusMinutes(15);
        apptRepo.findByStatusAndCreatedAtBefore(Status.PENDING, cutoff)
                .forEach(a -> {
                    a.setStatus(Status.CANCELLED);
                    apptRepo.save(a);
                });
    }

    public List<AppointmentDTO> listByDay(LocalDateTime day) {
        var start = day.withHour(0).withMinute(0);
        var end   = start.plusDays(1);
        return apptRepo.findByAppointmentTimeBetween(start, end).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
