package com.doctor.booking.service;

import com.doctor.booking.entity.Admin;
import com.doctor.booking.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDetailsService implements UserDetailsService {
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found: " + username));

        var authorities = Arrays.stream(admin.getRoles().split(","))
                .map(String::trim)
                .map(r -> "ROLE_" + r)  // Spring требует префикс ROLE_
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return User.withUsername(admin.getUsername())
                .password(admin.getPassword())
                .authorities(authorities)
                .build();
    }
}
