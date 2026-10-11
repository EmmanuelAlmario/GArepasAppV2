package com.garepas.garepasapp.repository;

import com.garepas.garepasapp.entity.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    List<Venta> findAllByOrderByFechaDesc();

    // Sin @EntityGraph: la paginación con fetch de colecciones se aplica en memoria
    // y eso rompe el tamaño real de la página. Los detalles se cargan aparte.
    Page<Venta> findAllBy(Pageable pageable);

    @Query("SELECT d FROM DetalleVenta d JOIN FETCH d.producto WHERE d.venta.id IN :ids")
    java.util.List<com.garepas.garepasapp.entity.DetalleVenta> findDetallesPorVentaIds(
            @Param("ids") java.util.Collection<Long> ids);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    Optional<Venta> findById(Long id);

    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin")
    BigDecimal sumTotalByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
