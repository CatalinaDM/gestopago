package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.model.pago.AutorizacionNipResponse;

import java.util.Optional;

/**
 * Interfaz centralizada para la autorización de operaciones financieras.
 * Desacoplada del flujo de pagos para permitir futura extensión a WebAuthn (biometría),
 * OTP u otros factores de autenticación reforzada.
 */
public interface AutorizacionTransaccionService {

    /**
     * Genera un NIP transaccional aleatorio de 4 a 6 dígitos al iniciar sesión únicamente
     * si el usuario aún no tiene un NIP registrado. Almacena solo el hash BCrypt.
     */
    Optional<String> asegurarNipAlIniciarSesion(Usuario usuario, String passwordPlano);

    /**
     * Genera y prepara el NIP transaccional para auto-rellenado tipo SoftToken en el flujo de pago
     * de un usuario autenticado, guardando únicamente su hash BCrypt.
     */
    AutorizacionNipResponse emitirNipParaPago(Integer usuarioId, Integer clienteId, String numeroCuenta);

    /**
     * Valida el NIP transaccional ingresado en la operación contra el hash BCrypt almacenado,
     * aplicando control de intentos fallidos y bloqueo temporal.
     */
    void validarAutorizacion(Integer usuarioId, String nipIngresado);
}

