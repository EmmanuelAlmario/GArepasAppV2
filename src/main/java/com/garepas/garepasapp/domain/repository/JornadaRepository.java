package com.garepas.garepasapp.domain.repository;

import com.garepas.garepasapp.domain.model.Jornada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JornadaRepository extends JpaRepository<Jornada, Long> {

    Optional<Jornada> findFirstByActivaTrueOrderByFechaAperturaDesc();

    List<Jornada> findAllByOrderByFechaAperturaDesc();
}
