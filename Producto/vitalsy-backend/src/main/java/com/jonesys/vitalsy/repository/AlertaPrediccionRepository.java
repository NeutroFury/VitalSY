package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.AlertaPrediccion;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertaPrediccionRepository extends JpaRepository<AlertaPrediccion, Integer> {
    
    Page<AlertaPrediccion> findByUsuarioOrderByFechaHoraDesc(Usuario usuario, Pageable pageable);
    
    List<AlertaPrediccion> findByUsuarioAndLeidaFalseOrderByFechaHoraDesc(Usuario usuario);
}
