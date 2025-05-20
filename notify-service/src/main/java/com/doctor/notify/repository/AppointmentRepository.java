package com.doctor.notify.repository;

import com.doctor.notify.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByAppointmentTimeBetween(LocalDateTime start, LocalDateTime end);
}
