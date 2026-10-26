package cl.duoc.digitalfix.notify.web.dto;

import java.time.LocalDateTime;

/**
 * Ticket de despacho a terreno: lo que recibe el tecnico al ser asignado.
 * El texto es el cuerpo del correo que se le envia.
 */
public record TicketDespachoDto(
        String numero,
        Long ordenId,
        Long empresaId,
        Long servicioId,
        Long tecnicoId,
        String tecnicoNombre,
        String tecnicoEmail,
        String tecnicoTelefono,
        String clienteId,
        String comentarios,
        boolean reasignacion,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaEmision,
        String asunto,
        String texto
) {}
