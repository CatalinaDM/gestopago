package com.proyecto.servicios.service;

public interface AesCipherService {

    String encrypt(String plainText);

    String decrypt(String cipherTextBase64);
}

