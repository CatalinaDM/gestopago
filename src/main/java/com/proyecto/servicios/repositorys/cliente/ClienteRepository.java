package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByCurp(String curp);

    Optional<Cliente> findByRfc(String rfc);

    Optional<Cliente> findByEmail(String email);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByFechaCreacionBetween(LocalDateTime inicio, LocalDateTime fin);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByEmail(String email);

    @Query("SELECT c FROM Cliente c JOIN c.cuentas cta WHERE cta.numeroCuenta = :numeroCuenta")
    Optional<Cliente> findByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);
}
