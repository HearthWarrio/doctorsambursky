package com.doctor.notify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.annotation.VaultPropertySource;

@Configuration
@VaultPropertySource(value = "secret/application", propertyNamePrefix = "vault.")
public class VaultConfig {
    @Value("${vault.telegram.token}")
    private String telegramToken;
    @Value("${vault.telegram.username}")
    private String telegramUsername;
    @Value("${vault.doctor.chat-id}")
    private String doctorChatId;

    public String getTelegramToken() {
        return telegramToken;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public String getDoctorChatId() {
        return doctorChatId;
    }
}
