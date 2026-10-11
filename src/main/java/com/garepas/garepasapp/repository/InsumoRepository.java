package com.garepas.garepasapp.repository;

import com.garepas.garepasapp.entity.Insumo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findByActivoTrue();

    boolean existsByNombreIgnoreCase(String nombre);

    List<Insumo> findByCategoria(String categoria);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id")
    Optional<Insumo> findByIdConLock(@Param("id") Long id);
}
