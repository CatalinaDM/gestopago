package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.MovimientoCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoCuentaRepository extends JpaRepository<MovimientoCuenta, Integer> {

    List<MovimientoCuenta> findByTransaccionPagoId(Integer transaccionPagoId);
}

