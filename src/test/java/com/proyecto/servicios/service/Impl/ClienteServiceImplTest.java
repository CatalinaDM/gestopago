package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.mapper.DomicilioMapper;
import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.model.cliente.DomicilioDTO;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

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

    private ClienteServiceImpl clienteService;

    @BeforeEach
    void setUp() {
        com.proyecto.servicios.mapper.StringSanitizer stringSanitizer = new com.proyecto.servicios.mapper.StringSanitizer();
        DomicilioMapper domicilioMapper = Mappers.getMapper(DomicilioMapper.class);
        CuentaMapper cuentaMapper = Mappers.getMapper(CuentaMapper.class);
        ClienteMapper clienteMapper = Mappers.getMapper(ClienteMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(domicilioMapper, "stringSanitizer", stringSanitizer);
        org.springframework.test.util.ReflectionTestUtils.setField(clienteMapper, "domicilioMapper", domicilioMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(clienteMapper, "cuentaMapper", cuentaMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(clienteMapper, "stringSanitizer", stringSanitizer);

        clienteService = new ClienteServiceImpl(
                clienteRepository,
                domicilioRepository,
                cuentaRepository,
                usuarioRepository,
                clienteMapper,
                domicilioMapper,
                passwordEncoder
        );
    }

    @Test
    void registrarCliente_Exitoso() {
        ClienteRegistroRequest request = crearRequestValido();
        when(clienteRepository.existsByCurpIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfcIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(1);
            return c;
        });

        ClienteResponse response = clienteService.registrarCliente(request);

        assertNotNull(response);
        assertEquals("Juan Carlos Lopez Garcia", response.getNombreCompleto());
        assertEquals("juan.lopez@example.com", response.getEmail());

        verify(clienteRepository).save(any(Cliente.class));
        verify(cuentaRepository).save(any(Cuenta.class));
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrarCliente_ErrorMenorDeEdad() {
        ClienteRegistroRequest request = crearRequestValido();
        request.setFechaNacimiento(LocalDate.now().minusYears(17));

        ValidacionException exception = assertThrows(
                ValidacionException.class,
                () -> clienteService.registrarCliente(request)
        );

        assertEquals("VALIDACION-002", exception.getCodigo());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void registrarCliente_ErrorCurpDuplicada() {
        ClienteRegistroRequest request = crearRequestValido();
        when(clienteRepository.existsByCurpIgnoreCase(request.getCurp())).thenReturn(true);

        CurpDuplicadaException exception = assertThrows(
                CurpDuplicadaException.class,
                () -> clienteService.registrarCliente(request)
        );

        assertEquals("CLIENTE-002", exception.getCodigo());
    }

    @Test
    void registrarCliente_ErrorRfcDuplicado() {
        ClienteRegistroRequest request = crearRequestValido();
        when(clienteRepository.existsByCurpIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfcIgnoreCase(request.getRfc())).thenReturn(true);

        RfcDuplicadoException exception = assertThrows(
                RfcDuplicadoException.class,
                () -> clienteService.registrarCliente(request)
        );

        assertEquals("CLIENTE-003", exception.getCodigo());
    }

    @Test
    void registrarCliente_ErrorCorreoDuplicado() {
        ClienteRegistroRequest request = crearRequestValido();
        when(clienteRepository.existsByCurpIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfcIgnoreCase(anyString())).thenReturn(false);
        when(clienteRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(true);

        CorreoDuplicadoException exception = assertThrows(
                CorreoDuplicadoException.class,
                () -> clienteService.registrarCliente(request)
        );

        assertEquals("CLIENTE-004", exception.getCodigo());
    }

    @Test
    void obtenerPorId_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));

        ClienteResponse response = clienteService.obtenerPorId(1);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Juan Lopez Garcia", response.getNombreCompleto());
    }

    @Test
    void obtenerPorId_NoEncontrado() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.obtenerPorId(99));
    }

    @Test
    void obtenerPorCurp_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.findByCurpContainingIgnoreCase("ABCD900101HDFRRN01")).thenReturn(List.of(cliente));

        List<ClienteResponse> response = clienteService.obtenerPorCurp("ABCD900101HDFRRN01");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("ABCD900101HDFRRN01", response.get(0).getCurp());
    }

    @Test
    void obtenerPorRfc_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.findByRfcContainingIgnoreCase("ABCD900101AB1")).thenReturn(List.of(cliente));

        List<ClienteResponse> response = clienteService.obtenerPorRfc("ABCD900101AB1");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("ABCD900101AB1", response.get(0).getRfc());
    }

    @Test
    void obtenerPorCorreo_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.findByEmailContainingIgnoreCase("juan@example.com")).thenReturn(List.of(cliente));

        List<ClienteResponse> response = clienteService.obtenerPorCorreo("juan@example.com");

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("juan@example.com", response.get(0).getEmail());
    }

    @Test
    void obtenerPorNumeroCuenta_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cliente));

        ClienteResponse response = clienteService.obtenerPorNumeroCuenta("1234567890");

        assertNotNull(response);
        assertEquals(1, response.getId());
    }

    @Test
    void obtenerActivos_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.findByActivoTrue()).thenReturn(List.of(cliente));

        List<ClienteResponse> response = clienteService.obtenerActivos();

        assertNotNull(response);
        assertEquals(1, response.size());
    }

    @Test
    void buscarClientes_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.buscarPorFiltroGeneral("juan")).thenReturn(List.of(cliente));

        List<ClienteResponse> response = clienteService.buscarClientes("juan");

        assertEquals(1, response.size());
    }

    @Test
    void buscarPorCriterios_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.buscarPorCriterios(any(), any(), any(), any())).thenReturn(List.of(cliente));

        List<ClienteResponse> response = clienteService.buscarPorCriterios("ABCD", null, null, null);

        assertEquals(1, response.size());
    }

    @Test
    void actualizarCliente_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(cliente);

        ClienteActualizaRequest request = ClienteActualizaRequest.builder()
                .nombre("Juan")
                .segundoNombre("Pedro")
                .apellidoPaterno("Lopez")
                .apellidoMaterno("Garcia")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .sexo("MASCULINO")
                .nacionalidad("MEXICANA")
                .estadoCivil("CASADO")
                .email("juan@example.com")
                .telefonoMovil("5512345678")
                .ocupacion("Ingeniero")
                .empresa("Tech Corp")
                .ingresoMensual(new BigDecimal("35000.00"))
                .domicilio(crearDomicilioDTO())
                .build();

        ClienteResponse response = clienteService.actualizarCliente(1, request);

        assertNotNull(response);
        assertEquals("Juan Pedro Lopez Garcia", response.getNombreCompleto());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void darDeBajaCliente_Exitoso() {
        Cliente cliente = crearClienteEntidad();
        Usuario usuario = new Usuario();
        usuario.setActivo(true);
        cliente.setUsuario(usuario);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setActivo(true);
        cliente.getCuentas().add(cuenta);

        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));

        clienteService.darDeBajaCliente(1);

        assertFalse(cliente.getActivo());
        assertFalse(cliente.getUsuario().getActivo());
        assertFalse(cuenta.getActivo());
        verify(clienteRepository).save(cliente);
    }

    private ClienteRegistroRequest crearRequestValido() {
        return ClienteRegistroRequest.builder()
                .nombre("Juan")
                .segundoNombre("Carlos")
                .apellidoPaterno("Lopez")
                .apellidoMaterno("Garcia")
                .fechaNacimiento(LocalDate.of(1995, 5, 20))
                .curp("LOGJ950520HDFRRN01")
                .rfc("LOGJ950520AB1")
                .sexo("MASCULINO")
                .nacionalidad("MEXICANA")
                .estadoCivil("SOLTERO")
                .email("juan.lopez@example.com")
                .telefonoMovil("5512345678")
                .ocupacion("Desarrollador")
                .empresa("Software SA")
                .ingresoMensual(new BigDecimal("25000.00"))
                .password("Password123!")
                .domicilio(crearDomicilioDTO())
                .build();
    }

    private DomicilioDTO crearDomicilioDTO() {
        return DomicilioDTO.builder()
                .calle("Av. Reforma")
                .numeroExterior("123")
                .numeroInterior("4B")
                .colonia("Juárez")
                .municipio("Cuauhtémoc")
                .estado("CDMX")
                .codigoPostal("06600")
                .pais("México")
                .build();
    }

    private Cliente crearClienteEntidad() {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setNombre("Juan");
        cliente.setApellidoPaterno("Lopez");
        cliente.setApellidoMaterno("Garcia");
        cliente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        cliente.setCurp("ABCD900101HDFRRN01");
        cliente.setRfc("ABCD900101AB1");
        cliente.setEmail("juan@example.com");
        cliente.setTelefonoMovil("5512345678");
        cliente.setActivo(true);
        cliente.setCuentas(new ArrayList<>());
        return cliente;
    }
}
