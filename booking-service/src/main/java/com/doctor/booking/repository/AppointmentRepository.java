package com.doctor.booking.repository;

import com.doctor.booking.entity.Appointment;
import com.doctor.booking.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByAppointmentTimeAndStatusIn(LocalDateTime time, Collection<AppointmentStatus> statuses);

    boolean existsByRescheduleProposedTimeAndStatus(LocalDateTime time, AppointmentStatus status);

    List<Appointment> findByStatusAndDoctorDecisionDeadlineAtBefore(AppointmentStatus status, LocalDateTime time);

    List<Appointment> findByAppointmentTimeBetweenAndStatusIn(LocalDateTime start, LocalDateTime end, Collection<AppointmentStatus> statuses);

    List<Appointment> findByRescheduleProposedTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, AppointmentStatus status);
}