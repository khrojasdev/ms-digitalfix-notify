package cl.duoc.digitalfix.notify.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Alerta de stock tal como la publica ms-digitalfix-catalog en el exchange
 * direct. notify la consume para avisar que un repuesto quedo bajo su minimo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertaStockDto(
        Long repuestoId,
        Long empresaId,
        String sku,
        String nombre,
        int stock,
        int stockMinimo,
        Instant fecha
) {}
