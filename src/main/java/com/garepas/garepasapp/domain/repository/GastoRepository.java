package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.Gasto;
import com.garepas.garepasapp.domain.enums.CategoriaGasto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findAllByOrderByFechaDesc();

    Page<Gasto> findAllByOrderByFechaDesc(Pageable pageable);

    List<Gasto> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Gasto> findByCategoria(CategoriaGasto categoria);

    List<Gasto> findByProduccionId(Long produccionId);

    void deleteByProduccionId(Long produccionId);

      
    @Query("select g.fecha, g.monto from Gasto g where g.fecha >= :desde and g.fecha < :hasta")
    List<Object[]> findImportesPorFecha(@Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);

    @Query("select g.categoria, sum(g.monto) from Gasto g "
            + "where g.fecha >= :desde and g.fecha < :hasta group by g.categoria order by sum(g.monto) desc")
    List<Object[]> sumarPorCategoria(@Param("desde") LocalDateTime desde,
                                     @Param("hasta") LocalDateTime hasta);

    @Query("select COALESCE(SUM(g.monto), 0) from Gasto g where g.fecha >= :desde and g.fecha < :hasta")
    BigDecimal sumMontoPorFecha(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

      
    @Query(value = """
            select g from Gasto g
            where lower(g.descripcion) like lower(concat('%', :q, '%'))
              or (:categoria is null or g.categoria = :categoria)
            """,
            countQuery = """
            select count(g) from Gasto g
            where lower(g.descripcion) like lower(concat('%', :q, '%'))
              or (:categoria is null or g.categoria = :categoria)
            """)
    Page<Gasto> buscar(@Param("q") String q, @Param("categoria") CategoriaGasto categoria, Pageable pageable);
}
