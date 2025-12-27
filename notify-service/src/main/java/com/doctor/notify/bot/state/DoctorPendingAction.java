package com.doctor.notify.bot.state;

import lombok.Data;

@Data
public class DoctorPendingAction {
    private DoctorPendingType type;
    private Long appointmentId;
}