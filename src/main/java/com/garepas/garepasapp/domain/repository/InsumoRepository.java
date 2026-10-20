package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.Insumo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findByActivoTrue();

    List<Insumo> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    List<Insumo> findByCategoria(String categoria);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id")
    Optional<Insumo> findByIdConLock(@Param("id") Long id);

    /**
     * Bloquea varios insumos en una sola consulta. El servicio los pide siempre
     * ordenados por id para que dos transacciones que compartan insumos
     * enganchen los bloqueos en el mismo orden (si no, se produce un deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id in :ids order by i.id")
    List<Insumo> findAllByIdConLock(@Param("ids") Collection<Long> ids);

    /** Recetas que usan este insumo como ingrediente. */
    @Query("select distinct r from Receta r join r.detalles d where d.insumo.id = :insumoId")
    List<com.garepas.garepasapp.domain.model.Receta> findRecetasQueUsan(@Param("insumoId") Long insumoId);

    @Query("select count(p) from Produccion p join p.detalles d where d.insumo.id = :insumoId")
    long countProduccionesQueUsan(@Param("insumoId") Long insumoId);

    @Query("select count(r) from Receta r where r.insumoProducido.id = :insumoId")
    long countRecetasQueProducen(@Param("insumoId") Long insumoId);
}
