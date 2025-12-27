package com.doctor.booking.service;

import com.doctor.booking.entity.Patient;
import com.doctor.booking.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository repo;

    public Patient findOrCreate(String name, String phone, String email) {
        return repo.findByPhone(phone)
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setName(name);
                    p.setPhone(phone);
                    p.setEmail(email);
                    return repo.save(p);
                });
    }

    public Patient findOrCreateTelegramPatient(Long telegramChatId,
                                               String name,
                                               String phone,
                                               String address,
                                               String telegramUsername,
                                               String whatsappNumber,
                                               String email) {
        return repo.findByTelegramChatId(telegramChatId)
                .orElseGet(() -> {
                    Patient p = new Patient();
                    p.setTelegramChatId(telegramChatId);
                    p.setName(name);
                    p.setPhone(phone);
                    p.setAddress(address);
                    p.setTelegramUsername(telegramUsername);
                    p.setWhatsappNumber(whatsappNumber);
                    p.setEmail(email);
                    return repo.save(p);
                });
    }
}