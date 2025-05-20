package com.doctor.booking.repository;

import com.doctor.booking.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    boolean existsByAppointmentTime(LocalDateTime time);
    List<Appointment> findByStatusAndCreatedAtBefore(Appointment.Status status, LocalDateTime cutoff);
    List<Appointment> findByAppointmentTimeBetween(LocalDateTime start, LocalDateTime end);
}
