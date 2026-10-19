package cl.duoc.digitalfix.notify.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Evento de orden tal como lo publica ms-digitalfix-workorders en el exchange
 * de negocio. notify solo lee los campos que necesita para armar el aviso;
 * ignoreUnknown evita acoplarse al formato exacto del productor si este agrega
 * campos nuevos mas adelante.
 *
 * tipoEvento: CREADA, ASIGNADA, INICIADA, CERRADA, CANCELADA
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenEventoDto(
        String eventKey,
        String tipoEvento,
        Long ordenId,
        String autorId,
        Long empresaId,
        LocalDateTime fecha,
        String correlacionId
) {}
