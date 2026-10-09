package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCurpIgnoreCase(String curp);

    Optional<Cliente> findByRfcIgnoreCase(String rfc);

    Optional<Cliente> findByEmailIgnoreCase(String email);

    List<Cliente> findByCurpContainingIgnoreCase(String curp);

    List<Cliente> findByRfcContainingIgnoreCase(String rfc);

    List<Cliente> findByEmailContainingIgnoreCase(String email);

    List<Cliente> findByActivoTrue();

    Page<Cliente> findByActivoTrue(Pageable pageable);

    List<Cliente> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    boolean existsByCurpIgnoreCase(String curp);

    boolean existsByRfcIgnoreCase(String rfc);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Cliente c JOIN c.cuentas cta WHERE UPPER(cta.numeroCuenta) = UPPER(:numeroCuenta)")
    Optional<Cliente> findByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

    @Query("SELECT DISTINCT c FROM Cliente c LEFT JOIN c.cuentas cta WHERE " +
           "(:filtro IS NULL OR :filtro = '' OR " +
           "LOWER(c.curp) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.rfc) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoMaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "cta.numeroCuenta LIKE CONCAT('%', :filtro, '%'))")
    List<Cliente> buscarPorFiltroGeneral(@Param("filtro") String filtro);

    @Query(value = "SELECT DISTINCT c FROM Cliente c LEFT JOIN c.cuentas cta WHERE " +
           "(:filtro IS NULL OR :filtro = '' OR " +
           "LOWER(c.curp) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.rfc) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoMaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "cta.numeroCuenta LIKE CONCAT('%', :filtro, '%'))",
           countQuery = "SELECT count(DISTINCT c) FROM Cliente c LEFT JOIN c.cuentas cta WHERE " +
           "(:filtro IS NULL OR :filtro = '' OR " +
           "LOWER(c.curp) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.rfc) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoMaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "cta.numeroCuenta LIKE CONCAT('%', :filtro, '%'))")
    Page<Cliente> buscarPorFiltroGeneralPaginado(@Param("filtro") String filtro, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Cliente c LEFT JOIN c.cuentas cta WHERE " +
           "(:curp IS NULL OR LOWER(c.curp) LIKE LOWER(CONCAT('%', :curp, '%'))) AND " +
           "(:rfc IS NULL OR LOWER(c.rfc) LIKE LOWER(CONCAT('%', :rfc, '%'))) AND " +
           "(:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:numeroCuenta IS NULL OR cta.numeroCuenta LIKE CONCAT('%', :numeroCuenta, '%'))")
    List<Cliente> buscarPorCriterios(
            @Param("curp") String curp,
            @Param("rfc") String rfc,
            @Param("email") String email,
            @Param("numeroCuenta") String numeroCuenta);
}
