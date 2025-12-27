package com.doctor.notify.secrets;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VaultSecretsService {

    private final VaultTemplate vault;

    @Value("${vault.path:secret/application}")
    private String path;

    public String resolve(String key, String fallback) {
        if (StringUtils.hasText(fallback)) return fallback;
        Map<String, Object> data = readData(path);
        Object v = data.get(key);
        return v == null ? null : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readData(String p) {
        VaultResponse r = vault.read(p);
        if (r == null || r.getData() == null) return Map.of();
        Object data = r.getData().get("data");
        if (data instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return r.getData();
    }
}