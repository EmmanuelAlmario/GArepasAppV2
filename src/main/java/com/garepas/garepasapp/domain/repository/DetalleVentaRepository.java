package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.DetalleVenta;
import com.garepas.garepasapp.domain.enums.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    List<DetalleVenta> findByVentaId(Long ventaId);

    List<DetalleVenta> findByProductoId(Long productoId);

      



    @Query("select d.producto.id, d.producto.nombre, sum(d.cantidad), sum(d.subtotal) "
            + "from DetalleVenta d "
            + "where d.venta.estado = :estado and d.venta.fecha >= :desde and d.venta.fecha < :hasta "
            + "group by d.producto.id, d.producto.nombre "
            + "order by sum(d.cantidad) desc")
    List<Object[]> topProductosPorUnidades(@Param("estado") EstadoVenta estado,
                                            @Param("desde") LocalDateTime desde,
                                            @Param("hasta") LocalDateTime hasta);
}
