package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface GlucoseReadingRepository extends JpaRepository<GlucoseReading, Integer> {
    
    Page<GlucoseReading> findByUsuarioOrderByFechaHoraDesc(Usuario usuario, Pageable pageable);
    
    List<GlucoseReading> findByUsuarioOrderByFechaHoraDesc(Usuario usuario);

    List<GlucoseReading> findTop20ByUsuarioOrderByFechaHoraDesc(Usuario usuario);
    
    List<GlucoseReading> findByUsuarioAndFechaHoraBetween(Usuario usuario, 
                                                          ZonedDateTime startDate, 
                                                          ZonedDateTime endDate);
    
    GlucoseReading findTop1ByUsuarioOrderByFechaHoraDesc(Usuario usuario);
}