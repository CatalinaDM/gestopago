package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Integer> {

    boolean existsByUpc(String upc);

    Optional<TransaccionPago> findByUpc(String upc);

    List<TransaccionPago> findByEstadoAndFechaCreacionBefore(EstadoTransaccion estado, LocalDateTime fechaLimite);
}

