package com.doctor.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;

@Configuration
public class VaultConfig {

    @Value("${vault.uri}")
    private String vaultUri;

    @Value("${vault.token}")
    private String vaultToken;

    @Bean
    public VaultEndpoint vaultEndpoint() {
        return VaultEndpoint.from(URI.create(vaultUri));
    }

    @Bean
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(vaultToken);
    }

    @Bean
    public VaultTemplate vaultTemplate(VaultEndpoint endpoint, ClientAuthentication clientAuthentication) {
        return new VaultTemplate(endpoint, clientAuthentication);
    }
}
