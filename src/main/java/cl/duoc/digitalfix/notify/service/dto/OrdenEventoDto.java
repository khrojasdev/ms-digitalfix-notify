package cl.duoc.digitalfix.notify.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * Evento de orden tal como lo publica ms-digitalfix-workorders en el exchange
 * de negocio. notify solo lee los campos que necesita para armar el aviso;
 * ignoreUnknown evita acoplarse al formato exacto del productor si este agrega
 * campos nuevos mas adelante.
 *
 * tipoEvento: CREADA, ASIGNADA, EN_DESPLAZAMIENTO, EN_EJECUCION, CERRADA, CANCELADA
 * (INICIADA es el nombre anterior de EN_EJECUCION).
 *
 * Los campos desde estadoAnterior en adelante los agrega workorders desde que
 * existe la asignacion de tecnico; en eventos antiguos llegan en null.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenEventoDto(
        String eventKey,
        String tipoEvento,
        Long ordenId,
        String autorId,
        Long empresaId,
        LocalDateTime fecha,
        String correlacionId,
        String estadoAnterior,
        String estadoNuevo,
        Long servicioId,
        String creadorId,
        String comentarios,
        Long tecnicoId,
        String tecnicoNombre,
        String tecnicoEmail,
        String tecnicoTelefono,
        String nota            // comentario opcional de quien cambio el estado
) {
    /** Evento sin datos de tecnico (formato anterior). */
    public OrdenEventoDto(String eventKey, String tipoEvento, Long ordenId, String autorId,
                          Long empresaId, LocalDateTime fecha, String correlacionId) {
        this(eventKey, tipoEvento, ordenId, autorId, empresaId, fecha, correlacionId,
                null, null, null, null, null, null, null, null, null, null);
    }

    /** Es una reasignacion: la orden ya estaba ASIGNADA y cambio de tecnico. */
    public boolean esReasignacion() {
        return "ASIGNADA".equalsIgnoreCase(tipoEvento) && "ASIGNADA".equalsIgnoreCase(estadoAnterior);
    }
}
