package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.enums.ErrorCode;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.NipInvalidoException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.pago.AutorizacionNipResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.service.AutorizacionTransaccionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AutorizacionTransaccionServiceImpl implements AutorizacionTransaccionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern FORMATO_NIP = Pattern.compile("^\\d{4,6}$");

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seguridad.nip.longitud:4}")
    private int longitudNip = 4;

    @Value("${seguridad.nip.max-intentos:3}")
    private int maxIntentosNip = 3;

    @Value("${seguridad.nip.bloqueo-minutos:15}")
    private int bloqueoMinutos = 15;

    public AutorizacionTransaccionServiceImpl(
            UsuarioRepository usuarioRepository,
            CuentaRepository cuentaRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Optional<String> asegurarNipAlIniciarSesion(Usuario usuario, String passwordPlano) {
        if (usuario == null || Boolean.FALSE.equals(usuario.getActivo())) {
            return Optional.empty();
        }

        // Si el usuario ya tiene un NIP registrado, no se genera otro automáticamente en cada login
        if (usuario.getNipHash() != null && !usuario.getNipHash().isBlank()) {
            return Optional.empty();
        }

        String nipGenerado = generarNipAleatorioSeguro(passwordPlano, usuario.getPassword());
        usuario.setNipHash(passwordEncoder.encode(nipGenerado));
        usuario.setIntentosFallidosNip(0);
        usuario.setNipBloqueadoHasta(null);
        usuario.setFechaActualizacionNip(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("NIP transaccional inicial configurado con hash BCrypt para el usuario ID: {}", usuario.getId());
        return Optional.of(nipGenerado);
    }

    @Override
    @Transactional
    public AutorizacionNipResponse emitirNipParaPago(Integer usuarioId, Integer clienteId, String numeroCuenta) {
        if (usuarioId == null) {
            throw new AccesoDenegadoException("AUTH-003", "Se requiere sesión autenticada para autorizar una operación");
        }

        Usuario usuario = obtenerUsuarioActivo(usuarioId);

        if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                    .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
            if (clienteId == null || cuenta.getCliente() == null || !clienteId.equals(cuenta.getCliente().getId())) {
                throw new AccesoDenegadoException("AUTH-004", "No tiene permisos para operar con la cuenta indicada");
            }
            if (Boolean.FALSE.equals(cuenta.getActivo())) {
                throw new CuentaInactivaException(numeroCuenta);
            }
        }

        verificarSiEstaBloqueado(usuario);

        String nip = generarNipAleatorioSeguro(null, usuario.getPassword());
        usuario.setNipHash(passwordEncoder.encode(nip));
        usuario.setIntentosFallidosNip(0);
        usuario.setNipBloqueadoHasta(null);
        usuario.setFechaActualizacionNip(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("Autorización NIP transaccional preparada para usuario ID: {}", usuario.getId());

        AutorizacionNipResponse response = AutorizacionNipResponse.builder()
                .nipTransaccional(nip)
                .mascaraVisual("•".repeat(nip.length()))
                .build();
        response.setCodigo(0);
        response.setMensaje("Código de autorización transaccional generado");
        return response;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = NipInvalidoException.class)
    public void validarAutorizacion(Integer usuarioId, String nipIngresado) {
        if (usuarioId == null) {
            throw new AccesoDenegadoException("AUTH-003", "Usuario no autenticado para autorizar la operación");
        }

        Usuario usuario = obtenerUsuarioActivo(usuarioId);
        verificarSiEstaBloqueado(usuario);

        if (usuario.getNipHash() == null || usuario.getNipHash().isBlank()) {
            log.warn("AUTH-NIP-001: Intento de operación sin NIP configurado para usuario ID: {}", usuarioId);
            throw new NipInvalidoException(ErrorCode.NIP_NO_CONFIGURADO, ErrorCode.NIP_NO_CONFIGURADO.getMessage());
        }

        if (nipIngresado == null || !FORMATO_NIP.matcher(nipIngresado).matches()) {
            registrarIntentoFallido(usuario);
            throw new NipInvalidoException(ErrorCode.NIP_INVALIDO, "El formato del NIP de autorización es inválido");
        }

        if (!passwordEncoder.matches(nipIngresado, usuario.getNipHash())) {
            registrarIntentoFallido(usuario);
            if (usuario.getNipBloqueadoHasta() != null && usuario.getNipBloqueadoHasta().isAfter(LocalDateTime.now())) {
                throw new NipInvalidoException(
                        ErrorCode.NIP_BLOQUEADO,
                        "El NIP transaccional ha sido bloqueado temporalmente por exceder el número de intentos permitidos"
                );
            }
            throw new NipInvalidoException(ErrorCode.NIP_INVALIDO, ErrorCode.NIP_INVALIDO.getMessage());
        }

        if (usuario.getIntentosFallidosNip() != null && usuario.getIntentosFallidosNip() > 0) {
            usuario.setIntentosFallidosNip(0);
            usuario.setNipBloqueadoHasta(null);
            usuarioRepository.save(usuario);
        }

        log.info("Autorización transaccional validada exitosamente para usuario ID: {}", usuarioId);
    }

    private Usuario obtenerUsuarioActivo(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(String.valueOf(usuarioId)));

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException(usuario.getCorreo());
        }
        if (usuario.getCliente() != null && Boolean.FALSE.equals(usuario.getCliente().getActivo())) {
            throw new AccesoDenegadoException("CLIENTE-INACTIVO", ErrorCode.CLIENTE_INACTIVO.getMessage());
        }
        return usuario;
    }

    private void verificarSiEstaBloqueado(Usuario usuario) {
        if (usuario.getNipBloqueadoHasta() != null) {
            if (usuario.getNipBloqueadoHasta().isAfter(LocalDateTime.now())) {
                log.warn("AUTH-NIP-002: Rechazo por NIP bloqueado temporalmente para usuario ID: {}", usuario.getId());
                throw new NipInvalidoException(
                        ErrorCode.NIP_BLOQUEADO,
                        ErrorCode.NIP_BLOQUEADO.getMessage()
                );
            } else {
                usuario.setNipBloqueadoHasta(null);
                usuario.setIntentosFallidosNip(0);
                usuarioRepository.save(usuario);
            }
        }
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = (usuario.getIntentosFallidosNip() != null ? usuario.getIntentosFallidosNip() : 0) + 1;
        usuario.setIntentosFallidosNip(intentos);

        int limite = maxIntentosNip > 0 ? maxIntentosNip : 3;
        if (intentos >= limite) {
            int minutos = bloqueoMinutos > 0 ? bloqueoMinutos : 15;
            usuario.setNipBloqueadoHasta(LocalDateTime.now().plusMinutes(minutos));
            log.warn("AUTH-NIP-003: NIP bloqueado temporalmente por alcanzar {} intentos fallidos para usuario ID: {}",
                    intentos, usuario.getId());
        } else {
            log.warn("AUTH-NIP-004: Intento fallido de NIP ({}/{}) para usuario ID: {}",
                    intentos, limite, usuario.getId());
        }
        usuarioRepository.save(usuario);
    }

    private String generarNipAleatorioSeguro(String passwordPlano, String passwordHash) {
        int digitos = (longitudNip >= 4 && longitudNip <= 6) ? longitudNip : 4;
        int limiteSuperior = (int) Math.pow(10, digitos);
        String formato = "%0" + digitos + "d";

        String candidato;
        int intentos = 0;
        do {
            int numero = SECURE_RANDOM.nextInt(limiteSuperior);
            candidato = String.format(formato, numero);
            intentos++;
        } while (intentos < 20 && coincideConPassword(candidato, passwordPlano, passwordHash));

        return candidato;
    }

    private boolean coincideConPassword(String nip, String passwordPlano, String passwordHash) {
        if (passwordPlano != null && nip.equals(passwordPlano)) {
            return true;
        }
        return passwordHash != null && passwordEncoder.matches(nip, passwordHash);
    }
}

