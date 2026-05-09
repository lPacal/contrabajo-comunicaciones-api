package com.contrabajo.comunicaciones_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ChatCryptoService {

    private static final String PREFIX = "ENCv1:";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec aesKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public ChatCryptoService(
            @Value("${chat.encryption.secret:${jwt.secret}}") String secret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Falta chat.encryption.secret (o jwt.secret) para cifrar mensajes.");
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            this.aesKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo inicializar clave de cifrado de chat.", ex);
        }
    }

    public String encryptForStorage(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cifrar mensaje de chat.", ex);
        }
    }

    public String decryptForRead(String storedValue) {
        if (storedValue == null) return null;
        if (!storedValue.startsWith(PREFIX)) {
            // Compatibilidad: mensajes historicos guardados en texto plano.
            return storedValue;
        }
        try {
            String payload = storedValue.substring(PREFIX.length());
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Formato de mensaje cifrado invalido.");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plainBytes = cipher.doFinal(encrypted);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo descifrar mensaje de chat.", ex);
        }
    }
}

