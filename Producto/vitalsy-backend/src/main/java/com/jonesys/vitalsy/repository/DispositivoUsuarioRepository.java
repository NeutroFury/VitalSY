package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.DispositivoUsuario;
import com.jonesys.vitalsy.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DispositivoUsuarioRepository extends JpaRepository<DispositivoUsuario, Integer> {

    /**
     * Obtiene todos los tokens FCM activos de un usuario.
     * Usado por FcmNotificationService para soportar multi-dispositivo.
     */
    List<DispositivoUsuario> findByUsuarioAndActivoTrue(Usuario usuario);

    /**
     * Busca un dispositivo por su token FCM (para upsert).
     */
    Optional<DispositivoUsuario> findByFcmToken(String fcmToken);
}
