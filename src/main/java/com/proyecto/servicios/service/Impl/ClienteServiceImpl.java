package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.Cliente;
import com.proyecto.servicios.entity.cliente.Cuenta;
import com.proyecto.servicios.entity.cliente.Domicilio;
import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.*;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.DomicilioMapper;
import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.cliente.ClienteAccionRequest;
import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteConsultaRequest;
import com.proyecto.servicios.model.cliente.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;
import com.proyecto.servicios.repositorys.cliente.ClienteRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.DomicilioRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.service.ClienteService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    public ClienteResponse registrarCliente(GenericRequest<ClienteRegistroRequest> genericRequest) {
        ClienteRegistroRequest request = genericRequest.getDatos();
        log.info("Iniciando registro de nuevo cliente con CURP: {}, RFC: {}", request.getCurp(), request.getRfc());

        validarMayoriaDeEdad(request.getFechaNacimiento());
        validarUnicidad(request.getCurp(), request.getRfc(), request.getEmail());

        // 1. Guardar Cliente
        Cliente cliente = clienteMapper.toEntity(request);
        cliente.setActivo(true);

        // 2. Asociar Domicilio
        Domicilio domicilio = domicilioMapper.toEntity(request.getDomicilio());
        domicilio.setCliente(cliente);
        cliente.setDomicilio(domicilio);

        cliente = clienteRepository.save(cliente);

        // 3. Crear Cuenta Bancaria Única (Saldo inicial definido por el sistema en $0.00)
        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setSaldo(BigDecimal.ZERO);
        cuenta.setActivo(true);
        cuentaRepository.save(cuenta);
        cliente.getCuentas().add(cuenta);

        // 4. Crear Usuario de Acceso (Rol 2 = CLIENTE)
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(2);
        usuario.setIntentosFallidos(0);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        cliente.setUsuario(usuario);

        log.info("Cliente registrado exitosamente. ID: {}, Cuenta: {}, Usuario: {}",
                cliente.getId(), cuenta.getNumeroCuenta(), usuario.getCorreo());

        return clienteMapper.toResponse(cliente);
    }

    @Override
    public Page<ClienteResponse> consultar(GenericRequest<ClienteConsultaRequest> genericRequest) {
        ClienteConsultaRequest criteria = genericRequest != null && genericRequest.getDatos() != null
                ? genericRequest.getDatos()
                : new ClienteConsultaRequest();

        log.info("Ejecutando consulta unificada de clientes con criterios: {}", criteria);

        Specification<Cliente> spec = (root, query, cb) -> {
            query.distinct(true);
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            if (criteria.getId() != null) {
                predicates.add(cb.equal(root.get("id"), criteria.getId()));
            }

            if (criteria.getSoloActivos() != null) {
                predicates.add(cb.equal(root.get("activo"), criteria.getSoloActivos()));
            }

            if (criteria.getCurp() != null && !criteria.getCurp().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("curp")), "%" + criteria.getCurp().trim().toLowerCase() + "%"));
            }

            if (criteria.getRfc() != null && !criteria.getRfc().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("rfc")), "%" + criteria.getRfc().trim().toLowerCase() + "%"));
            }

            if (criteria.getEmail() != null && !criteria.getEmail().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + criteria.getEmail().trim().toLowerCase() + "%"));
            }

            if (criteria.getNumeroCuenta() != null && !criteria.getNumeroCuenta().isBlank()) {
                Join<Cliente, Cuenta> cuentasJoin = root.join("cuentas", JoinType.LEFT);
                predicates.add(cb.like(cuentasJoin.get("numeroCuenta"), "%" + criteria.getNumeroCuenta().trim() + "%"));
            }

            if (criteria.getFechaInicio() != null && criteria.getFechaFin() != null) {
                LocalDateTime inicio = criteria.getFechaInicio().atStartOfDay();
                LocalDateTime fin = criteria.getFechaFin().atTime(LocalTime.MAX);
                predicates.add(cb.between(root.get("fechaCreacion"), inicio, fin));
            } else if (criteria.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), criteria.getFechaInicio().atStartOfDay()));
            } else if (criteria.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), criteria.getFechaFin().atTime(LocalTime.MAX)));
            }

            if (criteria.getFiltro() != null && !criteria.getFiltro().isBlank()) {
                String f = "%" + criteria.getFiltro().trim().toLowerCase() + "%";
                Join<Cliente, Cuenta> ctaJoin = root.join("cuentas", JoinType.LEFT);
                Predicate pFiltro = cb.or(
                        cb.like(cb.lower(root.get("curp")), f),
                        cb.like(cb.lower(root.get("rfc")), f),
                        cb.like(cb.lower(root.get("email")), f),
                        cb.like(cb.lower(root.get("nombre")), f),
                        cb.like(cb.lower(root.get("apellidoPaterno")), f),
                        cb.like(cb.lower(root.get("apellidoMaterno")), f),
                        cb.like(ctaJoin.get("numeroCuenta"), f)
                );
                predicates.add(pFiltro);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int page = criteria.getPagina() != null && criteria.getPagina() >= 0 ? criteria.getPagina() : 0;
        int size = criteria.getTamano() != null && criteria.getTamano() > 0 ? criteria.getTamano() : 50;
        Pageable pageable = PageRequest.of(page, size);

        return clienteRepository.findAll(spec, pageable).map(clienteMapper::toResponse);
    }

    @Override
    @Transactional
    public GenericResponse darDeBajaCliente(GenericRequest<ClienteAccionRequest> request) {
        Integer id = request != null && request.getDatos() != null ? request.getDatos().getId() : null;
        darDeBajaCliente(id);
        return GenericResponse.success("Cliente y servicios asociados dados de baja correctamente");
    }

    @Override
    @Transactional
    public GenericResponse reactivarCliente(GenericRequest<ClienteAccionRequest> request) {
        Integer id = request != null && request.getDatos() != null ? request.getDatos().getId() : null;
        reactivarCliente(id);
        return GenericResponse.success("Cliente y servicios asociados reactivados correctamente");
    }

    @Override
    public List<ClienteResponse> obtenerTodos() {
        log.info("Consultando todos los clientes");
        List<Cliente> clientes = clienteRepository.findAll();
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public Page<ClienteResponse> obtenerPaginados(Pageable pageable) {
        log.info("Consultando clientes paginados: página {}, tamaño {}", pageable.getPageNumber(), pageable.getPageSize());
        return clienteRepository.findAll(pageable)
                .map(clienteMapper::toResponse);
    }

    @Override
    public List<ClienteResponse> obtenerActivos() {
        log.info("Consultando clientes activos");
        List<Cliente> clientes = clienteRepository.findByActivoTrue();
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public Page<ClienteResponse> obtenerActivosPaginados(Pageable pageable) {
        log.info("Consultando clientes activos paginados: página {}, tamaño {}", pageable.getPageNumber(), pageable.getPageSize());
        return clienteRepository.findByActivoTrue(pageable)
                .map(clienteMapper::toResponse);
    }

    @Override
    public Page<ClienteResponse> buscarClientesPaginados(String filtro, Pageable pageable) {
        log.info("Buscando clientes paginados con filtro: {}", filtro);
        return clienteRepository.buscarPorFiltroGeneralPaginado(filtro.trim(), pageable)
                .map(clienteMapper::toResponse);
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
    public ClienteResponse obtenerPerfil(Integer clienteId) {
        log.info("Consultando perfil de autoservicio para cliente ID: {}", clienteId);
        if (clienteId == null) {
            throw new ValidacionException("AUTH-005", "El usuario autenticado no tiene un cliente asociado");
        }
        return obtenerPorId(clienteId);
    }

    @Override
    public List<ClienteResponse> obtenerPorCurp(String curp) {
        log.info("Buscando clientes por CURP parcial/exacta: {}", curp);
        List<Cliente> clientes = clienteRepository.findByCurpContainingIgnoreCase(curp.trim());
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public List<ClienteResponse> obtenerPorRfc(String rfc) {
        log.info("Buscando clientes por RFC parcial/exacto: {}", rfc);
        List<Cliente> clientes = clienteRepository.findByRfcContainingIgnoreCase(rfc.trim());
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public List<ClienteResponse> obtenerPorCorreo(String correo) {
        log.info("Buscando clientes por Correo parcial/exacto: {}", correo);
        List<Cliente> clientes = clienteRepository.findByEmailContainingIgnoreCase(correo.trim());
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public ClienteResponse obtenerPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cliente por Número de Cuenta: {}", numeroCuenta);
        Cliente cliente = clienteRepository.findByNumeroCuenta(numeroCuenta.trim())
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
        List<Cliente> clientes = clienteRepository.buscarPorFiltroGeneral(filtro.trim());
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    public List<ClienteResponse> buscarPorCriterios(String curp, String rfc, String email, String numeroCuenta) {
        log.info("Buscando clientes por criterios combinados (CURP: {}, RFC: {}, Email: {}, Cuenta: {})", curp, rfc, email, numeroCuenta);
        List<Cliente> clientes = clienteRepository.buscarPorCriterios(
                curp != null ? curp.trim() : null,
                rfc != null ? rfc.trim() : null,
                email != null ? email.trim() : null,
                numeroCuenta != null ? numeroCuenta.trim() : null);
        return clienteMapper.toResponseList(clientes);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(GenericRequest<ClienteActualizaRequest> genericRequest) {
        ClienteActualizaRequest request = genericRequest.getDatos();
        Integer id = request.getId();
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

        // Baja lógica en cascada: Cuentas inactivas
        if (cliente.getCuentas() != null) {
            cliente.getCuentas().forEach(cuenta -> {
                cuenta.setActivo(false);
                log.info("Cuenta {} asociada al cliente ID: {} marcada como inactiva", cuenta.getNumeroCuenta(), id);
            });
        }

        // Baja lógica en cascada: Usuario de acceso inactivo
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(false);
            log.info("Usuario {} asociado al cliente ID: {} marcado como inactivo", cliente.getUsuario().getCorreo(), id);
        }

        clienteRepository.save(cliente);
        log.info("Baja lógica completada exitosamente para el cliente ID: {}", id);
    }

    @Override
    @Transactional
    public void reactivarCliente(Integer id) {
        log.info("Ejecutando reactivación en cascada para cliente ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("CLIENTE-005: Cliente no encontrado con ID: {}", id);
                    return new ClienteNoEncontradoException("ID: " + id);
                });

        cliente.setActivo(true);

        // Reactivación en cascada: Cuentas activas
        if (cliente.getCuentas() != null) {
            cliente.getCuentas().forEach(cuenta -> {
                cuenta.setActivo(true);
                log.info("Cuenta {} asociada al cliente ID: {} reactivada", cuenta.getNumeroCuenta(), id);
            });
        }

        // Reactivación en cascada: Usuario de acceso activo con 0 intentos fallidos
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(true);
            cliente.getUsuario().setIntentosFallidos(0);
            log.info("Usuario {} asociado al cliente ID: {} reactivado exitosamente", cliente.getUsuario().getCorreo(), id);
        }

        clienteRepository.save(cliente);
        log.info("Reactivación en cascada completada exitosamente para el cliente ID: {}", id);
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
