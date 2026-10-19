package cl.duoc.digitalfix.notify.web;

import cl.duoc.digitalfix.notify.service.AvisoService;
import cl.duoc.digitalfix.notify.web.dto.AvisoDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de consulta de los avisos generados. Sirve para demostrar que el
 * consumidor esta recibiendo y procesando los eventos publicados por workorders.
 */
@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    private final AvisoService avisoService;

    public AvisoController(AvisoService avisoService) {
        this.avisoService = avisoService;
    }

    @GetMapping
    public List<AvisoDto> recientes() {
        return avisoService.recientes();
    }
}
