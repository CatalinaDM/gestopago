package com.proyecto.servicios.service.Impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.NipInvalidoException;
import com.proyecto.servicios.model.pago.AutorizacionNipResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutorizacionTransaccionServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    private PasswordEncoder passwordEncoder;
    private AutorizacionTransaccionServiceImpl autorizacionService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        autorizacionService = new AutorizacionTransaccionServiceImpl(
                usuarioRepository,
                cuentaRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Genera NIP de 4 dígitos al iniciar sesión si no existe, guarda solo hash BCrypt y no regenera si ya existe")
    void asegurarNipAlIniciarSesion_GeneraSoloLaPrimeraVez() {
        Usuario usuario = new Usuario();
        usuario.setId(7);
        usuario.setActivo(true);
        usuario.setPassword(passwordEncoder.encode("MiPasswordSegura123!"));

        Optional<String> nipOpt = autorizacionService.asegurarNipAlIniciarSesion(usuario, "MiPasswordSegura123!");

        assertTrue(nipOpt.isPresent());
        String nip = nipOpt.get();
        assertTrue(nip.matches("^\\d{4}$"), "El NIP debe tener exactamente 4 dígitos incluyendo ceros iniciales");
        assertNotNull(usuario.getNipHash());
        assertNotEquals(nip, usuario.getNipHash(), "Nunca debe guardarse el NIP en texto plano");
        assertTrue(passwordEncoder.matches(nip, usuario.getNipHash()));

        // Segunda llamada: como ya tiene nipHash, no debe generar otro
        String hashExistente = usuario.getNipHash();
        Optional<String> segundoIntento = autorizacionService.asegurarNipAlIniciarSesion(usuario, "MiPasswordSegura123!");
        assertTrue(segundoIntento.isEmpty());
        assertEquals(hashExistente, usuario.getNipHash());
    }

    @Test
    @DisplayName("Valida correctamente un NIP con ceros iniciales (0042) y rechaza NIP incorrecto bloqueando al 3er intento")
    void validarAutorizacion_ConCerosInicialesYBloqueoPorIntentos() {
        Usuario usuario = new Usuario();
        usuario.setId(15);
        usuario.setCorreo("cliente@banco.com");
        usuario.setActivo(true);
        Cliente cliente = new Cliente();
        cliente.setId(5);
        cliente.setActivo(true);
        usuario.setCliente(cliente);
        usuario.setNipHash(passwordEncoder.encode("0042"));

        when(usuarioRepository.findById(15)).thenReturn(Optional.of(usuario));

        // Validación exitosa con ceros iniciales "0042"
        assertDoesNotThrow(() -> autorizacionService.validarAutorizacion(15, "0042"));

        // 3 intentos fallidos consecutivos deben bloquear el NIP
        assertThrows(NipInvalidoException.class, () -> autorizacionService.validarAutorizacion(15, "9999"));
        assertEquals(1, usuario.getIntentosFallidosNip());

        assertThrows(NipInvalidoException.class, () -> autorizacionService.validarAutorizacion(15, "8888"));
        assertEquals(2, usuario.getIntentosFallidosNip());

        NipInvalidoException exBloqueo = assertThrows(NipInvalidoException.class,
                () -> autorizacionService.validarAutorizacion(15, "7777"));
        assertEquals(3, usuario.getIntentosFallidosNip());
        assertNotNull(usuario.getNipBloqueadoHasta());
        assertTrue(exBloqueo.getMessage().toLowerCase().contains("bloqueado"));

        // Incluso ingresando el NIP correcto mientras está bloqueado debe rechazarse
        assertThrows(NipInvalidoException.class, () -> autorizacionService.validarAutorizacion(15, "0042"));
    }

    @Test
    @DisplayName("Emitir NIP tipo SoftToken rechaza usuarios no autenticados y nunca registra el NIP en logs")
    void emitirNipParaPago_SeguridadYSinDatosSensiblesEnLogs() {
        assertThrows(AccesoDenegadoException.class,
                () -> autorizacionService.emitirNipParaPago(null, 1, "1234567890"));

        Logger logger = (Logger) LoggerFactory.getLogger(AutorizacionTransaccionServiceImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        Cliente cliente = new Cliente();
        cliente.setId(10);
        cliente.setActivo(true);

        Usuario usuario = new Usuario();
        usuario.setId(20);
        usuario.setActivo(true);
        usuario.setCliente(cliente);
        usuario.setPassword(passwordEncoder.encode("ClaveAcceso1!"));

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setActivo(true);
        cuenta.setCliente(cliente);

        when(usuarioRepository.findById(20)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AutorizacionNipResponse resp = autorizacionService.emitirNipParaPago(20, 10, "1234567890");

        assertNotNull(resp.getNipTransaccional());
        assertEquals("••••", resp.getMascaraVisual());
        assertTrue(passwordEncoder.matches(resp.getNipTransaccional(), usuario.getNipHash()));

        for (ILoggingEvent event : listAppender.list) {
            String msg = event.getFormattedMessage();
            assertFalse(msg.contains(resp.getNipTransaccional()), "El log nunca debe exponer el NIP en texto plano");
            assertFalse(msg.contains(usuario.getNipHash()), "El log nunca debe exponer el hash BCrypt del NIP");
        }

        logger.detachAppender(listAppender);
    }
}

