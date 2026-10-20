package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.enums.EstadoVenta;
import com.garepas.garepasapp.domain.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

      
      
    Page<Venta> findAllBy(Pageable pageable);

    @Query("SELECT d FROM DetalleVenta d JOIN FETCH d.producto WHERE d.venta.id IN :ids")
    List<com.garepas.garepasapp.domain.model.DetalleVenta> findDetallesPorVentaIds(
            @Param("ids") java.util.Collection<Long> ids);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    Optional<Venta> findById(Long id);

    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

      
    Page<Venta> findByEstadoOrderByFechaDesc(EstadoVenta estado, Pageable pageable);

      



    @Query(value = """
            select v from Venta v
            where (:estado is null or v.estado = :estado)
              and (lower(coalesce(v.nombreCliente, '')) like lower(concat('%', :q, '%'))
                   or exists (select 1 from DetalleVenta d
                              where d.venta.id = v.id
                                and lower(d.producto.nombre) like lower(concat('%', :q, '%'))))
            """,
            countQuery = """
            select count(v) from Venta v
            where (:estado is null or v.estado = :estado)
              and (lower(coalesce(v.nombreCliente, '')) like lower(concat('%', :q, '%'))
                   or exists (select 1 from DetalleVenta d
                              where d.venta.id = v.id
                                and lower(d.producto.nombre) like lower(concat('%', :q, '%'))))
            """)
    Page<Venta> buscar(@Param("q") String q, @Param("estado") EstadoVenta estado, Pageable pageable);

      




    @Query("select v.fecha, v.total from Venta v "
            + "where v.estado = :estado and v.fecha >= :desde and v.fecha < :hasta")
    List<Object[]> findImportesPorFecha(@Param("estado") EstadoVenta estado,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);

    @Query("select COALESCE(SUM(v.total), 0) from Venta v "
            + "where v.estado = :estado and v.fecha >= :desde and v.fecha < :hasta")
    BigDecimal sumTotalPorFecha(@Param("estado") EstadoVenta estado,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta);

    @Query("select count(v) from Venta v where v.jornada.id = :jornadaId and v.estado = :estado")
    long countPorJornada(@Param("jornadaId") Long jornadaId, @Param("estado") EstadoVenta estado);

    @Query("select COALESCE(SUM(v.total), 0) from Venta v where v.jornada.id = :jornadaId and v.estado = :estado")
    BigDecimal sumTotalPorJornada(@Param("jornadaId") Long jornadaId, @Param("estado") EstadoVenta estado);

      



    @Query("select v.jornada.id, count(v), COALESCE(sum(v.total), 0) from Venta v "
            + "where v.estado = :estado and v.jornada.id is not null group by v.jornada.id")
    List<Object[]> arqueoPorJornada(@Param("estado") EstadoVenta estado);

      



    @Query("select v.cliente.id, v.cliente.nombre, sum(v.saldo), count(v) from Venta v "
            + "where v.cliente is not null and v.estado = :estado and v.saldo > 0 "
            + "group by v.cliente.id, v.cliente.nombre order by sum(v.saldo) desc")
    List<Object[]> carteraPorCliente(@Param("estado") EstadoVenta estado);

      
    @Query("select v from Venta v where v.cliente.id = :clienteId and v.estado = :estado "
            + "order by v.fecha desc")
    List<Venta> findByClienteIdAndEstado(@Param("clienteId") Long clienteId,
                                          @Param("estado") EstadoVenta estado);

      
    @Query("select v from Venta v where v.estado = :estado and v.saldo > 0 "
            + "order by v.fechaVencimiento asc nulls last, v.fecha asc")
    List<Venta> findPendientesPorCobrar(@Param("estado") EstadoVenta estado);

      
    @Query("select count(v) from Venta v where v.cliente.id = :clienteId")
    long countVentasPorCliente(@Param("clienteId") Long clienteId);
}
