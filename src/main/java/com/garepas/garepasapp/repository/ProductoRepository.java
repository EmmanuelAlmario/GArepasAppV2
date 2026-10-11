package com.garepas.garepasapp.repository;

import com.garepas.garepasapp.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = {"receta", "receta.detalles", "receta.detalles.insumo"})
    List<Producto> findAll();

    @EntityGraph(attributePaths = {"receta", "receta.detalles", "receta.detalles.insumo"})
    List<Producto> findByActivoTrue();

    @EntityGraph(attributePaths = {"receta", "receta.detalles", "receta.detalles.insumo"})
    Optional<Producto> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdConLock(@Param("id") Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Producto> findByRecetaId(Long recetaId);
}
