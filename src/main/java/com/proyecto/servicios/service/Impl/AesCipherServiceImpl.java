package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.enums.ErrorCode;
import com.proyecto.servicios.exception.BusinessException;
import com.proyecto.servicios.service.AesCipherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class AesCipherServiceImpl implements AesCipherService {

    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM = "AES";

    private final SecretKey secretKey;
    private final IvParameterSpec ivParameterSpec;

    public AesCipherServiceImpl(
            @Value("${gestopago.aes.secret-key}") String secretKeyBase64,
            @Value("${gestopago.aes.iv}") String ivBase64) {
        if (secretKeyBase64 == null || secretKeyBase64.isBlank() || ivBase64 == null || ivBase64.isBlank()) {
            throw new IllegalArgumentException("Las propiedades de cifrado AES (secret-key e iv) son obligatorias");
        }
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64.trim());
        byte[] ivBytes = Base64.getDecoder().decode(ivBase64.trim());
        this.secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITHM);
        this.ivParameterSpec = new IvParameterSpec(ivBytes);
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new BusinessException(ErrorCode.GESTOPAGO_CIFRADO_ERROR, "El contenido a cifrar no puede ser nulo");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("AES-001: Error durante el proceso de cifrado AES ({})", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.GESTOPAGO_CIFRADO_ERROR, "No fue posible cifrar la carga útil de la transacción");
        }
    }

    @Override
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null || cipherTextBase64.isBlank()) {
            throw new BusinessException(ErrorCode.GESTOPAGO_CIFRADO_ERROR, "El contenido a descifrar no puede ser vacío");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decodedBytes = Base64.getDecoder().decode(cipherTextBase64.trim());
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-002: Error durante el proceso de descifrado AES ({})", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.GESTOPAGO_CIFRADO_ERROR, "No fue posible descifrar el contenido");
        }
    }
}

