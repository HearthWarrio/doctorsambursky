package com.doctor.booking.service;

import com.doctor.booking.dto.AppointmentDTO;
import com.doctor.booking.dto.BookingRequest;
import com.doctor.booking.dto.PaymentResponseDTO;
import com.doctor.booking.entity.Appointment;
import com.doctor.booking.entity.Appointment.Status;
import com.doctor.booking.entity.Patient;
import com.doctor.booking.mapper.AppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final PatientService patientService;
    private final PaymentService paymentService;
    private final com.doctor.booking.repository.AppointmentRepository apptRepo;
    private final AppointmentMapper mapper;

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
        a.setPaymentId(init.PaymentId);
        apptRepo.save(a);

        var dto = new PaymentResponseDTO();
        dto.setAppointmentId(a.getId());
        dto.setPaymentUrl(init.PaymentURL);
        return dto;
    }

    @Transactional
    public List<AppointmentDTO> getAvailable(LocalDateTime from, LocalDateTime to, int stepMinutes) {
        // генерируем слоты, фильтруем по базе
        return List.of(); // для простоты
    }

    @Transactional
    public void confirmPayment(String paymentId) {
        Appointment a = apptRepo.findAll().stream()
                .filter(x -> paymentId.equals(x.getPaymentId()))
                .findFirst()
                .orElseThrow();
        a.setStatus(Status.CONFIRMED);
        apptRepo.save(a);
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void cancelUnpaid() {
        var cutoff = LocalDateTime.now().minusMinutes(15);
        var list = apptRepo.findByStatusAndCreatedAtBefore(Status.PENDING, cutoff);
        for (var a : list) {
            a.setStatus(Status.CANCELLED);
            apptRepo.save(a);
        }
    }

    public List<AppointmentDTO> listByDay(LocalDateTime day) {
        var start = day.withHour(0).withMinute(0);
        var end   = start.plusDays(1);
        return apptRepo.findByAppointmentTimeBetween(start, end).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
