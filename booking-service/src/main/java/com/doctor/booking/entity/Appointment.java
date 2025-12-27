package com.doctor.booking.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "reschedule_proposed_time")
    private LocalDateTime rescheduleProposedTime;

    @Column(name = "doctor_decision_deadline_at")
    private LocalDateTime doctorDecisionDeadlineAt;
}