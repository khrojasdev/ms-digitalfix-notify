package cl.duoc.digitalfix.notify.web.dto;

import java.time.LocalDateTime;

/**
 * Aviso de stock ya generado, para exponerlo por la API de consulta.
 */
public record AvisoStockDto(
        Long repuestoId,
        String sku,
        String mensaje,
        Long empresaId,
        LocalDateTime fechaAviso
) {}
