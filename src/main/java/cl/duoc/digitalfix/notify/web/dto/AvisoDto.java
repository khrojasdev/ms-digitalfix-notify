package cl.duoc.digitalfix.notify.web.dto;

import java.time.LocalDateTime;

/**
 * Representacion de un aviso ya generado, para exponerlo por la API de consulta.
 */
public record AvisoDto(
        Long ordenId,
        String tipoEvento,
        String mensaje,
        Long empresaId,
        LocalDateTime fechaEvento,
        LocalDateTime fechaAviso
) {}
