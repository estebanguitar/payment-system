package com.example.paymentsystem.pg.infrastructure.security;

import com.example.paymentsystem.pg.application.port.out.security.PayloadEncryptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** AES-256-GCM 암복호화, 무작위 IV 및 변조 탐지를 검증한다. */
class AesGcmPayloadEncryptorTest {

    private PayloadEncryptor encryptor;

    /** 정확히 32바이트인 테스트 전용 AES 키로 암호화기를 준비한다. */
    @BeforeEach
    void setUp() {
        String key = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        encryptor = new AesGcmPayloadEncryptor(key);
    }

    /** 한글이 포함된 JSON을 암호화한 뒤 원문으로 복원하는지 확인한다. */
    @Test
    void encryptAndDecryptUtf8Payload() {
        String plaintext = "{\"고객\":\"홍길동\",\"결과\":\"승인\"}";

        String encrypted = encryptor.encrypt(plaintext);

        assertThat(encrypted).startsWith("v1:");
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(plaintext);
    }

    /** 동일 평문을 반복 암호화해도 무작위 IV로 서로 다른 암호문을 만드는지 확인한다. */
    @Test
    void createDifferentCiphertextForSamePlaintext() {
        String first = encryptor.encrypt("same-payload");
        String second = encryptor.encrypt("same-payload");

        assertThat(second).isNotEqualTo(first);
    }

    /** 암호문을 변조하면 GCM 인증 태그 검증으로 복호화를 거절하는지 확인한다. */
    @Test
    void rejectTamperedCiphertext() {
        String encrypted = encryptor.encrypt("sensitive-payload");
        char replacement = encrypted.endsWith("A") ? 'B' : 'A';
        String tampered = encrypted.substring(0, encrypted.length() - 1) + replacement;

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
                .isInstanceOf(EncryptionException.class);
    }

    /** Base64가 아니거나 32바이트가 아닌 키를 생성 시점에 거절하는지 확인한다. */
    @Test
    void rejectInvalidKey() {
        assertThatThrownBy(() -> new AesGcmPayloadEncryptor("not-base64"))
                .isInstanceOf(EncryptionException.class);
        String shortKey = Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> new AesGcmPayloadEncryptor(shortKey))
                .isInstanceOf(EncryptionException.class);
    }

    /** 빈 입력과 미지원 암호문 버전을 거절하는지 확인한다. */
    @Test
    void rejectBlankAndUnsupportedPayload() {
        assertThatThrownBy(() -> encryptor.encrypt(" ")).isInstanceOf(EncryptionException.class);
        assertThatThrownBy(() -> encryptor.decrypt("v2:a:b")).isInstanceOf(EncryptionException.class);
    }
}
