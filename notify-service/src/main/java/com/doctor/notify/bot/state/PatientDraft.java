package com.doctor.notify.bot.state;

import lombok.Data;

@Data
public class PatientDraft {
    private String patientName;
    private String phone;
    private String address;
    private String whatsappNumber;
    private String email;
    private String telegramUsername;
}