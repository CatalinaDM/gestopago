package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.DomicilioMapper;
import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final int EDAD_MINIMA = 18;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper clienteMapper;
    private final DomicilioMapper domicilioMapper;
    private final PasswordEncoder passwordEncoder;

    public ClienteServiceImpl(
            ClienteRepository clienteRepository,
            DomicilioRepository domicilioRepository,
            CuentaRepository cuentaRepository,
            UsuarioRepository usuarioRepository,
            ClienteMapper clienteMapper,
            DomicilioMapper domicilioMapper,
            PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteMapper = clienteMapper;
        this.domicilioMapper = domicilioMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public ClienteResponse registrarCliente(ClienteRegistroRequest request) {
        log.info("Iniciando registro de nuevo cliente con CURP: {}, RFC: {}", request.getCurp(), request.getRfc());

        validarMayoriaDeEdad(request.getFechaNacimiento());
        validarUnicidad(request.getCurp(), request.getRfc(), request.getEmail());

        BigDecimal saldoInicial = request.getSaldoInicial() != null ? request.getSaldoInicial() : BigDecimal.ZERO;
        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("VALIDACION-003: Intento de registro con saldo inicial negativo: {}", saldoInicial);
            throw new ValidacionException("VALIDACION-003", "El saldo inicial no puede ser negativo");
        }

        // 1. Guardar Cliente
        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setActivo(true);

        // 2. Asociar Domicilio
        Domicilio domicilio = domicilioMapper.toEntity(request.getDomicilio());
        domicilio.setCliente(cliente);
        cliente.setDomicilio(domicilio);

        cliente = clienteRepository.save(cliente);

        // 3. Crear Cuenta Bancaria Única
        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setSaldo(saldoInicial);
        cuenta.setEstatus("ACTIVA");
        cuentaRepository.save(cuenta);
        cliente.getCuentas().add(cuenta);

        // 4. Crear Usuario de Acceso
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        cliente.setUsuario(usuario);

        log.info("Cliente registrado exitosamente. ID: {}, Cuenta: {}, Usuario: {}",
                cliente.getId(), cuenta.getNumeroCuenta(), usuario.getCorreo());

        return clienteMapper.toResponse(cliente);
    }

    @Override
    public List<ClienteResponse> obtenerTodos() {
        log.info("Consultando todos los clientes");
        List<Cliente> clientes = clienteRepository.findAll();
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public List<ClienteResponse> obtenerActivos() {
        log.info("Consultando clientes activos");
        List<Cliente> clientes = clienteRepository.findByActivoTrue();
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public ClienteResponse obtenerPorId(Integer id) {
        log.info("Consultando cliente por ID: {}", id);
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("CLIENTE-005: Cliente no encontrado con ID: {}", id);
                    return new ClienteNoEncontradoException("ID: " + id);
                });
        return clienteMapper.toResponse(cliente);
    }

    @Override
    public ClienteResponse obtenerPorCurp(String curp) {
        log.info("Consultando cliente por CURP: {}", curp);
        Cliente cliente = clienteRepository.findByCurpIgnoreCase(curp)
                .orElseThrow(() -> {
                    log.warn("CLIENTE-005: Cliente no encontrado con CURP: {}", curp);
                    return new ClienteNoEncontradoException("CURP: " + curp);
                });
        return clienteMapper.toResponse(cliente);
    }

    @Override
    public ClienteResponse obtenerPorRfc(String rfc) {
        log.info("Consultando cliente por RFC: {}", rfc);
        Cliente cliente = clienteRepository.findByRfcIgnoreCase(rfc)
                .orElseThrow(() -> {
                    log.warn("CLIENTE-005: Cliente no encontrado con RFC: {}", rfc);
                    return new ClienteNoEncontradoException("RFC: " + rfc);
                });
        return clienteMapper.toResponse(cliente);
    }

    @Override
    public ClienteResponse obtenerPorCorreo(String correo) {
        log.info("Consultando cliente por Correo: {}", correo);
        Cliente cliente = clienteRepository.findByEmailIgnoreCase(correo)
                .orElseThrow(() -> {
                    log.warn("CLIENTE-005: Cliente no encontrado con Correo: {}", correo);
                    return new ClienteNoEncontradoException("Correo: " + correo);
                });
        return clienteMapper.toResponse(cliente);
    }

    @Override
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cliente por Número de Cuenta: {}", numeroCuenta);
        Cliente cliente = clienteRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> {
                    log.warn("CLIENTE-005: Cliente no encontrado con Número de Cuenta: {}", numeroCuenta);
                    return new ClienteNoEncontradoException("Número de cuenta: " + numeroCuenta);
                });
        return clienteMapper.toResponse(cliente);
    }

    @Override
    public List<ClienteResponse> obtenerPorRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        log.info("Consultando clientes registrados entre {} y {}", fechaInicio, fechaFin);
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
        List<Cliente> clientes = clienteRepository.findByFechaCreacionBetween(inicio, fin);
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public List<ClienteResponse> buscarClientes(String filtro) {
        log.info("Buscando clientes con filtro general case-insensitive: {}", filtro);
        List<Cliente> clientes = clienteRepository.buscarPorFiltroGeneral(filtro);
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public List<ClienteResponse> buscarPorCriterios(String curp, String rfc, String email, String numeroCuenta) {
        log.info("Buscando clientes por criterios combinados (CURP: {}, RFC: {}, Email: {}, Cuenta: {})", curp, rfc, email, numeroCuenta);
        List<Cliente> clientes = clienteRepository.buscarPorCriterios(curp, rfc, email, numeroCuenta);
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Integer id, ClienteActualizaRequest request) {
        log.info("Actualizando información del cliente ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("CLIENTE-005: Cliente no encontrado con ID: {}", id);
                    return new ClienteNoEncontradoException("ID: " + id);
                });

        validarMayoriaDeEdad(request.getFechaNacimiento());

        // Validar unicidad de correo si cambió
        if (!cliente.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (clienteRepository.existsByEmailIgnoreCase(request.getEmail()) || usuarioRepository.existsByCorreo(request.getEmail())) {
                log.warn("CLIENTE-004: Correo duplicado en actualización: {}", request.getEmail());
                throw new CorreoDuplicadoException(request.getEmail());
            }
            cliente.setEmail(request.getEmail());
            if (cliente.getUsuario() != null) {
                cliente.getUsuario().setCorreo(request.getEmail());
            }
        }

        // Actualizar datos personales y laborales (CURP y RFC permanecen inmutables)
        clienteMapper.updateEntity(request, cliente);

        // Actualizar Domicilio
        if (request.getDomicilio() != null) {
            if (cliente.getDomicilio() != null) {
                domicilioMapper.updateEntity(request.getDomicilio(), cliente.getDomicilio());
            } else {
                Domicilio nuevoDomicilio = domicilioMapper.toEntity(request.getDomicilio());
                nuevoDomicilio.setCliente(cliente);
                cliente.setDomicilio(nuevoDomicilio);
            }
        }

        cliente = clienteRepository.save(cliente);
        log.info("Cliente ID: {} actualizado exitosamente", id);

        return clienteMapper.toResponse(cliente);
    }

    @Override
    @Transactional
    public void darDeBajaCliente(Integer id) {
        log.info("Ejecutando baja lógica para cliente ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("CLIENTE-005: Cliente no encontrado con ID: {}", id);
                    return new ClienteNoEncontradoException("ID: " + id);
                });

        cliente.setActivo(false);

        // Las cuentas bancarias se marcan como INACTIVAS para restringir transacciones u operaciones
        if (cliente.getCuentas() != null) {
            cliente.getCuentas().forEach(cuenta -> {
                cuenta.setEstatus("INACTIVA");
                log.info("Cuenta {} asociada al cliente ID: {} marcada como INACTIVA", cuenta.getNumeroCuenta(), id);
            });
        }

        clienteRepository.save(cliente);
        log.info("Baja lógica completada para el cliente ID: {} (usuario conserva acceso de solo consulta)", id);
    }

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new ValidacionException("VALIDACION-002", "La fecha de nacimiento es obligatoria");
        }
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            log.warn("VALIDACION-002: Edad inválida: {} años. Requerido mínimo: {} años", edad, EDAD_MINIMA);
            throw new ValidacionException("VALIDACION-002", "El cliente debe ser mayor de edad (18 años o más). Edad actual: " + edad + " años");
        }
    }

    private void validarUnicidad(String curp, String rfc, String email) {
        if (clienteRepository.existsByCurpIgnoreCase(curp)) {
            log.warn("CLIENTE-002: CURP duplicada: {}", curp);
            throw new CurpDuplicadaException(curp);
        }
        if (clienteRepository.existsByRfcIgnoreCase(rfc)) {
            log.warn("CLIENTE-003: RFC duplicado: {}", rfc);
            throw new RfcDuplicadoException(rfc);
        }
        if (clienteRepository.existsByEmailIgnoreCase(email) || usuarioRepository.existsByCorreo(email)) {
            log.warn("CLIENTE-004: Correo duplicado: {}", email);
            throw new CorreoDuplicadoException(email);
        }
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            long numero = 1000000000L + (long) (RANDOM.nextDouble() * 9000000000L);
            numeroCuenta = String.valueOf(numero);
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }
}
