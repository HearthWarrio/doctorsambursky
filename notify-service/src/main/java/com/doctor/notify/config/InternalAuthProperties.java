package com.doctor.notify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "internal.auth")
public class InternalAuthProperties {
    private String token;
    private String header = "X-Internal-Token";
}