package com.novelgeneration.novel.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyEncryptor {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final String configuredKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public ApiKeyEncryptor(@Value("${ai.config.encryption-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, nonce));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(nonce) + "."
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("API Key 加密失败", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            throw new IllegalArgumentException("加密 API Key 不能为空");
        }
        try {
            String[] parts = cipherText.split("\\.", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("加密 API Key 格式不正确");
            }
            byte[] nonce = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("API Key 解密失败", e);
        }
    }

    public String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "未配置";
        }
        if (apiKey.length() <= 4) {
            return "****";
        }
        int prefixLength = Math.min(3, apiKey.length() - 4);
        return apiKey.substring(0, prefixLength) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private SecretKeySpec key() {
        byte[] bytes = configuredKey == null ? new byte[0] : configuredKey.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 32) {
            throw new IllegalStateException("模型配置加密密钥未配置或长度不正确");
        }
        return new SecretKeySpec(bytes, "AES");
    }
}
