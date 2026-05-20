package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.RegistroInsulina;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface RegistroInsulinaRepository extends JpaRepository<RegistroInsulina, Integer> {
    
    Page<RegistroInsulina> findByUsuarioOrderByFechaHoraDesc(Usuario usuario, Pageable pageable);
    
    List<RegistroInsulina> findByUsuarioAndFechaHoraBetween(Usuario usuario, 
                                                           ZonedDateTime startDate, 
                                                           ZonedDateTime endDate);
}
