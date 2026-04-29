package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.GlucoseReading;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface GlucoseReadingRepository extends JpaRepository<GlucoseReading, Integer> {
    
    Page<GlucoseReading> findByUsuarioOrderByFechaHoraDesc(Usuario usuario, Pageable pageable);
    
    List<GlucoseReading> findByUsuarioAndFechaHoraBetween(Usuario usuario, 
                                                          ZonedDateTime startDate, 
                                                          ZonedDateTime endDate);
    
    @Query("SELECT g FROM GlucoseReading g WHERE g.usuario = :usuario ORDER BY g.fechaHora DESC LIMIT 1")
    GlucoseReading findLatestByUsuario(@Param("usuario") Usuario usuario);
}