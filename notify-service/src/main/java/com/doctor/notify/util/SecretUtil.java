package com.doctor.notify.util;

import org.springframework.util.StringUtils;

public class SecretUtil {
    public static String maskToken(String token) {
        if (!StringUtils.hasLength(token) || token.length() < 8) return "****";
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
