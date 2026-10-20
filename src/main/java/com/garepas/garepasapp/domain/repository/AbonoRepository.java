package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.Abono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbonoRepository extends JpaRepository<Abono, Long> {

    List<Abono> findByVentaIdOrderByFechaDesc(Long ventaId);

      
    @Query("select a from Abono a where a.venta.cliente.id = :clienteId order by a.fecha desc")
    List<Abono> findByClienteId(@Param("clienteId") Long clienteId);
}
