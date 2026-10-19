package cl.duoc.digitalfix.notify.service;

import cl.duoc.digitalfix.notify.service.dto.AlertaStockDto;
import cl.duoc.digitalfix.notify.web.dto.AvisoStockDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Logica de los avisos de stock: convierte una alerta recibida de catalog en un
 * aviso legible y lo deja disponible para consulta. No conoce RabbitMQ.
 */
@Service
public class AvisoStockService {

    private static final Logger log = LoggerFactory.getLogger(AvisoStockService.class);
    private static final int MAX_AVISOS = 100;

    private final Deque<AvisoStockDto> recientes = new ArrayDeque<>();

    public synchronized void registrar(AlertaStockDto alerta) {
        String mensaje = "El repuesto " + alerta.sku() + " (" + alerta.nombre()
                + ") quedo bajo el minimo: " + alerta.stock() + " de " + alerta.stockMinimo() + ".";

        AvisoStockDto aviso = new AvisoStockDto(
                alerta.repuestoId(),
                alerta.sku(),
                mensaje,
                alerta.empresaId(),
                LocalDateTime.now()
        );

        recientes.addFirst(aviso);
        while (recientes.size() > MAX_AVISOS) {
            recientes.removeLast();
        }
        log.warn("Aviso de stock [empresa {}]: {}", alerta.empresaId(), mensaje);
    }

    public synchronized List<AvisoStockDto> recientes() {
        return new ArrayList<>(recientes);
    }
}
