package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.ConfiguracionNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracionNegocioRepository extends JpaRepository<ConfiguracionNegocio, Long> {

      
    Optional<ConfiguracionNegocio> findTopByOrderByIdAsc();
}
