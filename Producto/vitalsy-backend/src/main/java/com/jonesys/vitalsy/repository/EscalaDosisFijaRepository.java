package com.jonesys.vitalsy.repository;

import com.jonesys.vitalsy.model.EscalaDosisFija;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscalaDosisFijaRepository extends JpaRepository<EscalaDosisFija, Integer> {

    @Query(value = "SELECT * FROM escala_dosis_fija e " +
            "WHERE e.usuario_id = :usuarioId " +
            "AND e.nombre_comida_personalizado = :nombreComida " +
            "AND :glicemia BETWEEN e.glicemia_min AND e.glicemia_max " +
            "ORDER BY ABS(e.carbohidratos_gr - :carbohidratos) ASC " +
            "LIMIT 1", nativeQuery = true)
    Optional<EscalaDosisFija> buscarDosisPorTabla(
            @Param("usuarioId") Integer usuarioId,
            @Param("nombreComida") String nombreComida,
            @Param("glicemia") Integer glicemia,
            @Param("carbohidratos") Double carbohidratos);

    @Query("SELECT DISTINCT e.nombreComidaPersonalizado FROM EscalaDosisFija e WHERE e.usuario.id = :usuarioId")
    List<String> findComidasByUsuarioId(@Param("usuarioId") Integer usuarioId);

    void deleteAllByUsuario_Id(Integer usuarioId);
}
