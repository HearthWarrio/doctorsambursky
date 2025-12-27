package com.doctor.notify.bot.state;

public enum PatientStep {
    IDLE,
    WAIT_NAME,
    WAIT_PHONE,
    WAIT_ADDRESS,
    WAIT_WHATSAPP,
    WAIT_EMAIL,
    WAIT_TIME
}