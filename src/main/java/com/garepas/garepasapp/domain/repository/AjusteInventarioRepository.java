package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.AjusteInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long> {

    Page<AjusteInventario> findAllByOrderByFechaDesc(Pageable pageable);
}
