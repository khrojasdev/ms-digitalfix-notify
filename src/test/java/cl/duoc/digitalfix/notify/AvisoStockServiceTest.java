package cl.duoc.digitalfix.notify;

import cl.duoc.digitalfix.notify.service.AvisoStockService;
import cl.duoc.digitalfix.notify.service.dto.AlertaStockDto;
import cl.duoc.digitalfix.notify.web.dto.AvisoStockDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que la alerta de stock recibida se traduce a un aviso legible y
 * queda disponible para consulta.
 */
class AvisoStockServiceTest {

    @Test
    void registraYRedactaElAvisoDeStock() {
        AvisoStockService service = new AvisoStockService();

        service.registrar(new AlertaStockDto(10L, 7L, "SKU-9", "Fusible", 1, 5, Instant.now()));

        List<AvisoStockDto> avisos = service.recientes();
        assertThat(avisos).hasSize(1);
        assertThat(avisos.get(0).sku()).isEqualTo("SKU-9");
        assertThat(avisos.get(0).empresaId()).isEqualTo(7L);
        assertThat(avisos.get(0).mensaje()).contains("bajo el minimo").contains("1 de 5");
    }
}
