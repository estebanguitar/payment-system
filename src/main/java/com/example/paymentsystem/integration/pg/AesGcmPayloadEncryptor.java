package com.example.paymentsystem.integration.pg;

import com.example.paymentsystem.integration.pg.security.PayloadEncryptor;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** 무작위 IV와 인증 태그를 사용하는 AES-256-GCM PG payload 암복호화를 제공한다. */
public class AesGcmPayloadEncryptor implements PayloadEncryptor {

    private static final String VERSION = "v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_256_KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    /** Base64로 인코딩된 32바이트 키를 검증하여 암호화기를 생성한다. */
    public AesGcmPayloadEncryptor(String base64Key) {
        this(base64Key, new SecureRandom());
    }

    AesGcmPayloadEncryptor(String base64Key, SecureRandom secureRandom) {
        byte[] decodedKey = decodeKey(base64Key);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
        this.secureRandom = secureRandom;
    }

    /** 매 호출마다 새로운 12바이트 IV를 생성하여 인증 암호문을 반환한다. */
    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new EncryptionException("암호화할 PG 응답 원문은 필수입니다.");
        }
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(VERSION.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + ":" + Base64.getEncoder().encodeToString(iv)
                    + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new EncryptionException("PG 응답 암호화에 실패했습니다.", exception);
        }
    }

    /** 버전·IV·인증 태그를 검증하고 변조되지 않은 암호문만 복호화한다. */
    @Override
    public String decrypt(String encryptedPayload) {
        String[] parts = encryptedPayload == null ? new String[0] : encryptedPayload.split(":", -1);
        if (parts.length != 3 || !VERSION.equals(parts[0])) {
            throw new EncryptionException("지원하지 않거나 잘못된 암호문 형식입니다.");
        }
        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
            if (iv.length != IV_BYTES) {
                throw new EncryptionException("암호문의 IV 길이가 올바르지 않습니다.");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(VERSION.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new EncryptionException("PG 응답 복호화 또는 무결성 검증에 실패했습니다.", exception);
        }
    }

    private static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new EncryptionException("Base64 AES-256 키 설정이 필요합니다.");
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Key);
            if (decoded.length != AES_256_KEY_BYTES) {
                throw new EncryptionException("암호화 키는 정확히 32바이트여야 합니다.");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new EncryptionException("암호화 키는 올바른 Base64 형식이어야 합니다.", exception);
        }
    }
}
