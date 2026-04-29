package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.ParametroClinico;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParametroClinicoRepository extends JpaRepository<ParametroClinico, Integer> {
    
    Optional<ParametroClinico> findByUsuario(Usuario usuario);
}
