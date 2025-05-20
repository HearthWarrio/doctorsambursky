package com.doctor.booking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt-хэш пароля */
    @Column(nullable = false)
    private String password;

    /** роли через запятую, например: "ADMIN" или "ADMIN,MANAGER" */
    @Column(nullable = false)
    private String roles;
}
