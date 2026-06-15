package com.jonesys.vitalsy.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * AttributeConverter JPA de defensa en profundidad para ZonedDateTime ↔ Timestamp JDBC.
 *
 * PROPÓSITO:
 * Actúa como red de seguridad adicional entre Hibernate y el driver PostgreSQL JDBC.
 * Garantiza que el offset del ZonedDateTime se preserve de forma determinista durante
 * la serialización al driver JDBC, independientemente de la versión de Hibernate
 * o de actualizaciones futuras del driver postgresql.
 *
 * FLUJO DE DATOS:
 *   Java (ZonedDateTime con offset) → convertToDatabaseColumn → UTC Timestamp → PostgreSQL TIMESTAMPTZ
 *   PostgreSQL TIMESTAMPTZ          → convertToEntityAttribute → ZonedDateTime en UTC
 *
 * NOTA sobre la rehidratación en UTC:
 *   El valor se rehidrata en UTC (ZoneOffset.UTC). Esto es intencional.
 *   La conversión a la zona horaria local del paciente se aplica ÚNICAMENTE en la
 *   capa de servicio (ej. TimelineAssemblerService, IaService) usando usuario.getZoneId().
 *   Mezclar la conversión de zona en el converter causaría comportamiento no determinista
 *   al acceder a las entidades fuera del contexto de un usuario específico.
 *
 * autoApply = true:
 *   Se aplica automáticamente a TODOS los campos ZonedDateTime de TODAS las entidades.
 *   No requiere anotación @Convert en cada campo.
 */
@Converter(autoApply = true)
public class ZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, Timestamp> {

    /**
     * Java → PostgreSQL: Convierte ZonedDateTime a Timestamp JDBC.
     *
     * Estrategia: Se extrae el Instant (punto absoluto en el tiempo) y se
     * entrega al driver sin conversión de zona. PostgreSQL almacena el valor
     * en UTC internamente (comportamiento estándar de TIMESTAMPTZ) y aplica
     * la zona de la sesión solo al mostrarlo, lo cual no nos afecta porque
     * hemos fijado la sesión a UTC via Hikari (connection-init-sql).
     */
    @Override
    public Timestamp convertToDatabaseColumn(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return Timestamp.from(zonedDateTime.toInstant());
    }

    /**
     * PostgreSQL → Java: Convierte Timestamp JDBC a ZonedDateTime.
     *
     * El Timestamp leído de PostgreSQL siempre representa un punto UTC absoluto.
     * Lo rehidratamos como ZonedDateTime en UTC. La conversión a la zona
     * del paciente sucede en la capa de negocio, no aquí.
     */
    @Override
    public ZonedDateTime convertToEntityAttribute(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC);
    }
}
