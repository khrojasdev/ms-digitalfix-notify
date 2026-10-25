package cl.duoc.digitalfix.notify.web;

import cl.duoc.digitalfix.notify.service.AvisoService;
import cl.duoc.digitalfix.notify.service.AvisoStockService;
import cl.duoc.digitalfix.notify.web.dto.AvisoDto;
import cl.duoc.digitalfix.notify.web.dto.AvisoStockDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de consulta de los avisos generados. Sirve para demostrar que los
 * consumidores estan recibiendo y procesando los mensajes: los eventos de orden
 * que publica workorders (topic) y las alertas de stock que publica catalog
 * (direct).
 */
@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    private final AvisoService avisoService;
    private final AvisoStockService avisoStockService;

    public AvisoController(AvisoService avisoService, AvisoStockService avisoStockService) {
        this.avisoService = avisoService;
        this.avisoStockService = avisoStockService;
    }

    @GetMapping
    public List<AvisoDto> recientes() {
        return avisoService.recientes();
    }

    @GetMapping("/stock")
    public List<AvisoStockDto> stock() {
        return avisoStockService.recientes();
    }
}
