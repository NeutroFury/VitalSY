package com.jonesys.vitalsy.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Repositorio nativo para la consulta UNION ALL que fusiona registros_glucemia,
 * registros_insulina y registros_nutricion en una única línea de tiempo cronológica.
 *
 * DISEÑO: Se usa EntityManager en lugar de una interfaz JpaRepository porque:
 *   1. Las consultas UNION ALL devuelven Object[] heterogéneo, no una entidad JPA única.
 *   2. Spring Data JPA no puede aplicar paginación automática sobre UNION ALL nativos.
 *   3. EntityManager da control total sobre el tipo de parámetro ZonedDateTime
 *      que se pasa al driver JDBC, respetando nuestra configuración de timezone.
 *
 * ORDEN DE COLUMNAS RETORNADAS (Object[] por fila):
 *   [0] timestamp       → java.sql.Timestamp (TIMESTAMPTZ en UTC desde la BD)
 *   [1] event_type      → String
 *   [2] numeric_value   → BigDecimal / Double (valor principal)
 *   [3] numeric_value2  → BigDecimal / Double / null (valor secundario)
 *   [4] label           → String / null
 *   [5] subtype         → String / null
 *   [6] notes           → String / null
 */
@Repository
public class TimelineRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Consulta maestra UNION ALL que unifica los tres tipos de eventos clínicos.
     *
     * LÓGICA DE CLASIFICACIÓN DE INSULINA:
     *   El campo tipo_insulina en la BD es texto libre (ej. "Rápida", "Basal").
     *   El CASE usa LOWER() + LIKE para cubrir variaciones ortográficas sin tildes
     *   y distintas capitalizaciones que el usuario haya introducido.
     *
     * NOTA sobre ORDER BY:
     *   Se ordena por la columna posicional 1 (timestamp ASC) para que el
     *   LLM siempre reciba la causa antes del efecto (comida → insulina → glucosa).
     *
     * @param userId  ID del paciente cuyo historial se consulta
     * @param start   Inicio de la ventana de tiempo (inclusive)
     * @param end     Fin de la ventana de tiempo (inclusive)
     * @return Lista de Object[] con las 7 columnas definidas arriba, ordenada ASC
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findUnifiedTimeline(Integer userId, ZonedDateTime start, ZonedDateTime end) {

        String sql = """
                SELECT
                    rg.fecha_hora                              AS timestamp,
                    'GLUCOSE'                                  AS event_type,
                    CAST(rg.valor_mgdl AS DOUBLE PRECISION)   AS numeric_value,
                    NULL::DOUBLE PRECISION                     AS numeric_value2,
                    rg.tendencia                               AS label,
                    rg.tipo_registro                           AS subtype,
                    rg.comentarios                             AS notes
                FROM registros_glucemia rg
                WHERE rg.usuario_id = :userId
                  AND rg.fecha_hora BETWEEN :start AND :end

                UNION ALL

                SELECT
                    ri.fecha_hora,
                    CASE
                        WHEN LOWER(ri.tipo_insulina) LIKE '%r%pid%'
                          OR LOWER(ri.tipo_insulina) LIKE '%bolus%'  THEN 'INSULIN_BOLUS'
                        WHEN LOWER(ri.tipo_insulina) LIKE '%bas%'
                          OR LOWER(ri.tipo_insulina) LIKE '%lent%'   THEN 'INSULIN_BASAL'
                        ELSE 'INSULIN'
                    END,
                    ri.unidades_aplicadas,
                    ri.unidades_sugeridas,
                    ri.tipo_insulina,
                    ri.sitio_aplicacion,
                    NULL::TEXT
                FROM registros_insulina ri
                WHERE ri.usuario_id = :userId
                  AND ri.fecha_hora BETWEEN :start AND :end

                UNION ALL

                SELECT
                    rn.fecha_hora,
                    'MEAL',
                    rn.carbohidratos_gr,
                    CAST(rn.calorias_kcal AS DOUBLE PRECISION),
                    rn.descripcion_comida,
                    rn.momento_dia,
                    rn.estado_animo
                FROM registros_nutricion rn
                WHERE rn.usuario_id = :userId
                  AND rn.fecha_hora BETWEEN :start AND :end

                ORDER BY 1 ASC
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("start",  start);
        query.setParameter("end",    end);

        return query.getResultList();
    }
}
