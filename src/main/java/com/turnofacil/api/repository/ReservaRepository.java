package com.turnofacil.api.repository;

import com.turnofacil.api.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByEstado(String estado);

    Optional<Reserva> findById(Long id);
}