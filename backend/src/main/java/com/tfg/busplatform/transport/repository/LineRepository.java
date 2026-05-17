package com.tfg.busplatform.transport.repository;

import com.tfg.busplatform.transport.model.Line;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LineRepository extends JpaRepository<Line, Long> {
    Optional<Line> findByCode(String code);
}
