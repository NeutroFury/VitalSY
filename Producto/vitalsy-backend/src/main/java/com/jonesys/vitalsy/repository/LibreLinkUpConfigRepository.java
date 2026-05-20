package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.LibreLinkUpConfig;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LibreLinkUpConfigRepository extends JpaRepository<LibreLinkUpConfig, Integer> {
    Optional<LibreLinkUpConfig> findByUsuario(Usuario usuario);
    List<LibreLinkUpConfig> findByActivoTrue();
}
