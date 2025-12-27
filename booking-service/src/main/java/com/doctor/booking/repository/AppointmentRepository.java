package com.doctor.booking.repository;

import com.doctor.booking.entity.Appointment;
import com.doctor.booking.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByAppointmentTime(LocalDateTime time);

    List<Appointment> findByStatusAndCreatedAtBefore(AppointmentStatus status, LocalDateTime cutoff);

    List<Appointment> findByAppointmentTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<Appointment> findByPaymentId(String paymentId);
}