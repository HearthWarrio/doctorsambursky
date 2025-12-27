package com.doctor.booking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "patients")
@Data
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String phone;

    @Column(unique = true)
    private String email;

    @Column
    private String address;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;
}