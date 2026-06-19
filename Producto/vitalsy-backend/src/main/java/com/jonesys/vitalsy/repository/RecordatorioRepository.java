package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.Recordatorio;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecordatorioRepository extends JpaRepository<Recordatorio, Long> {
    List<Recordatorio> findByUsuario(Usuario usuario);
}
