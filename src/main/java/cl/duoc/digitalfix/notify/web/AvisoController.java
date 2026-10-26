package cl.duoc.digitalfix.notify.web;

import cl.duoc.digitalfix.notify.service.AvisoService;
import cl.duoc.digitalfix.notify.service.AvisoStockService;
import cl.duoc.digitalfix.notify.web.dto.AvisoDto;
import cl.duoc.digitalfix.notify.web.dto.AvisoStockDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;

/**
 * API de consulta de los avisos generados. Sirve para demostrar que los
 * consumidores estan recibiendo y procesando los mensajes: los eventos de orden
 * que publica workorders (topic) y las alertas de stock que publica catalog
 * (direct).
 *
 * Los avisos de todas las empresas se guardan juntos, asi que cada consulta se
 * filtra por la empresa que manda el BFF en X-Company-Id: una empresa nunca ve
 * los avisos de otra. Sin la cabecera (llamada directa al microservicio, que
 * solo es alcanzable desde la red interna) se devuelven todos.
 */
@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    static final String EMPRESA = "X-Company-Id";

    private final AvisoService avisoService;
    private final AvisoStockService avisoStockService;

    public AvisoController(AvisoService avisoService, AvisoStockService avisoStockService) {
        this.avisoService = avisoService;
        this.avisoStockService = avisoStockService;
    }

    @GetMapping
    public List<AvisoDto> recientes(@RequestHeader(value = EMPRESA, required = false) Long empresaId) {
        return deLaEmpresa(avisoService.recientes(), AvisoDto::empresaId, empresaId);
    }

    @GetMapping("/stock")
    public List<AvisoStockDto> stock(@RequestHeader(value = EMPRESA, required = false) Long empresaId) {
        return deLaEmpresa(avisoStockService.recientes(), AvisoStockDto::empresaId, empresaId);
    }

    private static <T> List<T> deLaEmpresa(List<T> avisos, Function<T, Long> empresaDe, Long empresaId) {
        if (empresaId == null) {
            return avisos;
        }
        return avisos.stream().filter(a -> empresaId.equals(empresaDe.apply(a))).toList();
    }
}
