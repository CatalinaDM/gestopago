package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.UsuarioInactivoException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.mapper.DomicilioMapper;
import com.proyecto.servicios.mapper.StringSanitizer;
import com.proyecto.servicios.mapper.UsuarioMapper;
import com.proyecto.servicios.model.auth.LoginRequest;
import com.proyecto.servicios.model.auth.LoginResponse;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.model.cliente.DomicilioDTO;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Ciclo de Vida: Registro -> Baja Lógica -> Intento de Login -> Bloqueo -> Desbloqueo")
class ClienteCicloVidaTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private DomicilioRepository domicilioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private ClienteServiceImpl clienteService;
    private AuthServiceImpl authService;
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        StringSanitizer stringSanitizer = new StringSanitizer();
        DomicilioMapper domicilioMapper = Mappers.getMapper(DomicilioMapper.class);
        CuentaMapper cuentaMapper = Mappers.getMapper(CuentaMapper.class);
        ClienteMapper clienteMapper = Mappers.getMapper(ClienteMapper.class);

        ReflectionTestUtils.setField(domicilioMapper, "stringSanitizer", stringSanitizer);
        ReflectionTestUtils.setField(clienteMapper, "domicilioMapper", domicilioMapper);
        ReflectionTestUtils.setField(clienteMapper, "cuentaMapper", cuentaMapper);
        ReflectionTestUtils.setField(clienteMapper, "stringSanitizer", stringSanitizer);

        clienteService = new ClienteServiceImpl(
                clienteRepository,
                domicilioRepository,
                cuentaRepository,
                usuarioRepository,
                clienteMapper,
                domicilioMapper,
                passwordEncoder
        );

        authService = new AuthServiceImpl(usuarioRepository, passwordEncoder, jwtUtil);
        UsuarioMapper usuarioMapper = Mappers.getMapper(UsuarioMapper.class);
        usuarioService = new UsuarioServiceImpl(usuarioRepository, usuarioMapper, passwordEncoder);
    }

    private ClienteRegistroRequest crearRequest() {
        return ClienteRegistroRequest.builder()
                .nombre("Mario")
                .apellidoPaterno("Hernandez")
                .apellidoMaterno("Ruiz")
                .fechaNacimiento(LocalDate.of(1995, 3, 20))
                .curp("HERM950320HDFRRN01")
                .rfc("HERM950320AB1")
                .sexo("H")
                .nacionalidad("Mexicana")
                .estadoCivil("Soltero")
                .email("mario.hernandez@example.com")
                .telefonoMovil("5588776655")
                .ocupacion("Contador")
                .empresa("Finanzas SA")
                .ingresoMensual(new BigDecimal("25000.00"))
                .password("Password123!")
                .domicilio(DomicilioDTO.builder()
                        .calle("Insurgentes Sur")
                        .numeroExterior("450")
                        .colonia("Roma")
                        .municipio("Cuauhtemoc")
                        .estado("CDMX")
                        .codigoPostal("06700")
                        .pais("Mexico")
                        .build())
                .build();
    }

    @Test
    @DisplayName("Ciclo 1: Cliente registrado puede hacer login exitoso; tras baja lógica el login se RECHAZA")
    void cicloVida_Registro_BajaLogica_RechazoLogin() {
        // 1. Simular registro exitoso
        when(clienteRepository.existsByCurpIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfcIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$hashedPassword");

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(100);
            return c;
        });

        ClienteResponse response = clienteService.registrarCliente(crearRequest());
        assertNotNull(response);

        // 2. Crear las entidades vinculadas en estado activo
        Cliente cliente = new Cliente();
        cliente.setId(100);
        cliente.setActivo(true);
        cliente.setCuentas(new ArrayList<>());

        Cuenta cuenta = new Cuenta();
        cuenta.setId(200);
        cuenta.setNumeroCuenta("9988776655");
        cuenta.setActivo(true);
        cuenta.setCliente(cliente);
        cliente.getCuentas().add(cuenta);

        Usuario usuario = new Usuario();
        usuario.setId(300);
        usuario.setCorreo("mario.hernandez@example.com");
        usuario.setPassword("$2a$10$hashedPassword");
        usuario.setActivo(true);
        usuario.setRol(2);
        usuario.setIntentosFallidos(0);
        usuario.setCliente(cliente);
        cliente.setUsuario(usuario);

        // 3. Login de usuario activo -> DEBE FUNCIONAR
        when(usuarioRepository.findByCorreo("mario.hernandez@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Password123!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtUtil.generarToken("mario.hernandez@example.com", 300, 100, 2)).thenReturn("jwt.token.valido");

        LoginResponse loginResp = authService.login(new LoginRequest("mario.hernandez@example.com", "Password123!"));
        assertNotNull(loginResp);
        assertEquals("jwt.token.valido", loginResp.getToken());

        // 4. Ejecutar Baja Lógica del Cliente
        when(clienteRepository.findById(100)).thenReturn(Optional.of(cliente));
        clienteService.darDeBajaCliente(100);

        // Verificamos que cliente, cuentas y usuario quedaron inactivos (false)
        assertFalse(cliente.getActivo(), "El cliente debe quedar inactivo (false)");
        assertFalse(cuenta.getActivo(), "La cuenta debe quedar inactiva (false)");
        assertFalse(usuario.getActivo(), "El usuario debe quedar inactivo (false)");

        // 5. Intento de Login de Usuario tras la baja lógica -> DEBE SER RECHAZADO
        UsuarioInactivoException ex = assertThrows(
                UsuarioInactivoException.class,
                () -> authService.login(new LoginRequest("mario.hernandez@example.com", "Password123!"))
        );
        assertEquals("AUTH-002", ex.getCodigo());
        assertTrue(ex.getMessage().contains("inactivo o bloqueado"));
    }

    @Test
    @DisplayName("Ciclo 2: 3 intentos fallidos de contraseña bloquean la cuenta; admin la desbloquea y login vuelve a funcionar")
    void cicloVida_BloqueoPorIntentos_Y_DesbloqueoPorAdmin() {
        Usuario usuario = new Usuario();
        usuario.setId(50);
        usuario.setCorreo("bloqueo@example.com");
        usuario.setPassword("$2a$10$hashedPassword");
        usuario.setActivo(true);
        usuario.setRol(2);
        usuario.setIntentosFallidos(0);

        when(usuarioRepository.findByCorreo("bloqueo@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("WrongPass", "$2a$10$hashedPassword")).thenReturn(false);

        // Intento 1 fallido
        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequest("bloqueo@example.com", "WrongPass")));
        assertEquals(1, usuario.getIntentosFallidos());
        assertTrue(usuario.getActivo());

        // Intento 2 fallido
        assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(new LoginRequest("bloqueo@example.com", "WrongPass")));
        assertEquals(2, usuario.getIntentosFallidos());
        assertTrue(usuario.getActivo());

        // Intento 3 fallido -> SE BLOQUEA
        UsuarioInactivoException exBloqueo = assertThrows(UsuarioInactivoException.class,
                () -> authService.login(new LoginRequest("bloqueo@example.com", "WrongPass")));
        assertEquals(3, usuario.getIntentosFallidos());
        assertFalse(usuario.getActivo(), "Al 3er fallo el usuario debe quedar inactivo");
        assertTrue(exBloqueo.getMessage().contains("Acceso bloqueado"));

        // Intento posterior con contraseña correcta -> RECHAZADO POR ESTAR BLOQUEADO
        UsuarioInactivoException exPost = assertThrows(UsuarioInactivoException.class,
                () -> authService.login(new LoginRequest("bloqueo@example.com", "PasswordCorrecto!")));
        assertTrue(exPost.getMessage().contains("Acceso bloqueado por exceder el límite"));

        // Administrador ejecuta desbloqueo
        when(usuarioRepository.findById(50)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        usuarioService.desbloquearUsuario(50);
        assertTrue(usuario.getActivo(), "El usuario debe volver a estar activo");
        assertEquals(0, usuario.getIntentosFallidos(), "Los intentos fallidos deben resetearse a 0");

        // Login posterior con contraseña correcta -> AHORA SÍ FUNCIONA
        when(passwordEncoder.matches("PasswordCorrecto!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtUtil.generarToken(anyString(), anyInt(), any(), anyInt())).thenReturn("jwt.token.postDesbloqueo");

        LoginResponse loginExitoso = authService.login(new LoginRequest("bloqueo@example.com", "PasswordCorrecto!"));
        assertNotNull(loginExitoso);
        assertEquals("jwt.token.postDesbloqueo", loginExitoso.getToken());
    }

    @Test
    @DisplayName("Ciclo de Vida: Baja lógica en cascada -> Reactivación en cascada por Administrador")
    void cicloVida_BajaLogica_ReactivacionCascada_Exitoso() {
        Cliente cliente = new Cliente();
        cliente.setId(10);
        cliente.setActivo(false);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("9876543210");
        cuenta.setActivo(false);
        cliente.setCuentas(new ArrayList<>(List.of(cuenta)));

        Usuario usuario = new Usuario();
        usuario.setCorreo("reactivar@example.com");
        usuario.setActivo(false);
        usuario.setIntentosFallidos(3);
        cliente.setUsuario(usuario);

        when(clienteRepository.findById(10)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        // Administrador reactiva al cliente
        clienteService.reactivarCliente(10);

        assertTrue(cliente.getActivo(), "El cliente debe volver a estar activo");
        assertTrue(cuenta.getActivo(), "La cuenta debe volver a estar activa");
        assertTrue(usuario.getActivo(), "El usuario debe volver a estar activo");
        assertEquals(0, usuario.getIntentosFallidos(), "Los intentos fallidos deben resetearse a 0");
        verify(clienteRepository).save(cliente);
    }
}
