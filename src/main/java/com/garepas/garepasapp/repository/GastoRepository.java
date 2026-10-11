package com.garepas.garepasapp.repository;

import com.garepas.garepasapp.entity.Gasto;
import com.garepas.garepasapp.enums.CategoriaGasto;
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

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM Gasto g WHERE g.fecha BETWEEN :inicio AND :fin")
    BigDecimal sumMontoByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
