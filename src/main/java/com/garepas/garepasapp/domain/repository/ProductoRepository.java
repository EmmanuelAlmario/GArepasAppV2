package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = {"receta", "receta.detalles", "receta.detalles.insumo"})
    List<Producto> findAll();

    @EntityGraph(attributePaths = {"receta", "receta.detalles", "receta.detalles.insumo"})
    List<Producto> findByActivoTrue();

    // Menu publico: solo columnas escalares, sin cargar recetas.
    List<Producto> findByActivoTrueOrderByNombreAsc();

    @EntityGraph(attributePaths = {"receta", "receta.detalles", "receta.detalles.insumo"})
    Optional<Producto> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdConLock(@Param("id") Long id);

    /**
     * Bloquea varios productos en una sola consulta y en orden por id, para
     * que el punto de venta enganche siempre los bloqueos en el mismo orden.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id in :ids order by p.id")
    List<Producto> findAllByIdConLock(@Param("ids") Collection<Long> ids);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Producto> findByRecetaId(Long recetaId);

    /** Productos que se elaboran al momento (bajo pedido) y por lo tanto no se almacenan. */
    @Query("select p from Producto p where p.bajoPedido = true and p.activo = true order by p.nombre")
    List<Producto> findBajoPedidoActivos();

    @Query("select count(v) from DetalleVenta v where v.producto.id = :productoId")
    long countVentasQueUsan(@Param("productoId") Long productoId);

    @Query("select count(p) from Produccion p where p.producto.id = :productoId")
    long countProduccionesQueUsan(@Param("productoId") Long productoId);
}
