package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.GlucoseReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlucoseReadingRepository extends JpaRepository<GlucoseReading, Long> {
    // Aquí podrías agregar métodos de búsqueda personalizados más adelante 
}