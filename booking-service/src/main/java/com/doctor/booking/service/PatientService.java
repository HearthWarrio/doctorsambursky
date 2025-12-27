package com.doctor.booking.service;

import com.doctor.booking.entity.Patient;
import com.doctor.booking.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        // Prefer telegramChatId, but if patient already exists from the web flow (phone unique), link the chatId.
        Optional<Patient> byChat = repo.findByTelegramChatId(telegramChatId);
        if (byChat.isPresent()) {
            Patient p = byChat.get();
            mergeTelegramFields(p, name, phone, address, telegramUsername, whatsappNumber, email, telegramChatId);
            return repo.save(p);
        }

        Optional<Patient> byPhone = repo.findByPhone(phone);
        if (byPhone.isPresent()) {
            Patient p = byPhone.get();
            mergeTelegramFields(p, name, phone, address, telegramUsername, whatsappNumber, email, telegramChatId);
            return repo.save(p);
        }

        Patient p = new Patient();
        mergeTelegramFields(p, name, phone, address, telegramUsername, whatsappNumber, email, telegramChatId);
        return repo.save(p);
    }

    private static void mergeTelegramFields(Patient p,
                                            String name,
                                            String phone,
                                            String address,
                                            String telegramUsername,
                                            String whatsappNumber,
                                            String email,
                                            Long telegramChatId) {
        if (telegramChatId != null) {
            p.setTelegramChatId(telegramChatId);
        }

        if (phone != null && !phone.isBlank()) {
            p.setPhone(phone);
        }
        if (name != null && !name.isBlank()) {
            p.setName(name);
        }
        if (address != null && !address.isBlank()) {
            p.setAddress(address);
        }
        if (telegramUsername != null && !telegramUsername.isBlank()) {
            p.setTelegramUsername(telegramUsername);
        }
        if (whatsappNumber != null && !whatsappNumber.isBlank()) {
            p.setWhatsappNumber(whatsappNumber);
        }
        if (email != null && !email.isBlank()) {
            // email is nullable in DB; keep existing value if present to avoid accidental uniqueness conflicts
            if (p.getEmail() == null || p.getEmail().isBlank()) {
                p.setEmail(email);
            }
        }
    }
}