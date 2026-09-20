package com.novelgeneration.novel.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiKeyEncryptorTest {

    @Test
    void encryptAndDecryptRoundTrip() {
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("12345678901234567890123456789012");

        String encrypted = encryptor.encrypt("sk-secret-key");

        assertNotEquals("sk-secret-key", encrypted);
        assertEquals("sk-secret-key", encryptor.decrypt(encrypted));
    }

    @Test
    void encryptionUsesRandomNonce() {
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("12345678901234567890123456789012");

        assertNotEquals(encryptor.encrypt("same-key"), encryptor.encrypt("same-key"));
    }

    @Test
    void masksKeyForUiWithoutExposingIt() {
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("12345678901234567890123456789012");

        assertEquals("sk-****alue", encryptor.mask("sk-test-value"));
        assertEquals("未配置", encryptor.mask(""));
    }

    @Test
    void rejectsInvalidEncryptionKeyLength() {
        ApiKeyEncryptor encryptor = new ApiKeyEncryptor("too-short");

        assertThrows(IllegalStateException.class, () -> encryptor.encrypt("secret"));
    }
}
