package com.doctor.notify.bot.state;

import lombok.Data;

@Data
public class PatientSession {
    private PatientStep step = PatientStep.IDLE;
    private PatientDraft draft = new PatientDraft();
    private Long lastAppointmentId;
}