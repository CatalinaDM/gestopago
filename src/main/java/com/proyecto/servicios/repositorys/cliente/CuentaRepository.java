package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByClienteId(Integer clienteId);

    List<Cuenta> findByEstatus(String estatus);

    boolean existsByNumeroCuenta(String numeroCuenta);
}
