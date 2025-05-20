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
                .orElseGet(() -> repo.save(new Patient(){{
                    setName(name); setPhone(phone); setEmail(email);
                }}));
    }
}
