package cl.duoc.digitalfix.notify.service;

import cl.duoc.digitalfix.notify.service.dto.OrdenEventoDto;
import cl.duoc.digitalfix.notify.web.dto.AvisoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logica de negocio del microservicio: convierte un evento de orden en un aviso
 * legible de cambio de estado.
 *
 * Esta clase NO conoce RabbitMQ. Recibe un DTO ya deserializado y devuelve el
 * aviso generado, de modo que la configuracion de mensajeria quede totalmente
 * separada de la logica de dominio (exigencia de la pauta).
 *
 * En esta entrega los avisos se registran en log y se guardan en memoria para
 * poder consultarlos por la API y demostrar el consumo. El envio real por
 * correo y la generacion de PDF (ticket e informe) se incorporan en la
 * siguiente entrega.
 */
@Service
public class AvisoService {

    private static final Logger log = LoggerFactory.getLogger(AvisoService.class);

    /** Cuantos avisos recientes se conservan en memoria para la API de consulta. */
    private static final int MAX_AVISOS = 100;

    private final Deque<AvisoDto> recientes = new ArrayDeque<>();
    /** eventKey ya procesados, para no avisar dos veces el mismo evento. */
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    /**
     * Genera el aviso para un evento de orden. Es idempotente por eventKey:
     * si el mismo evento llega repetido, se ignora sin volver a avisar.
     */
    public synchronized void generarAviso(OrdenEventoDto evento) {
        if (evento.eventKey() != null && !procesados.add(evento.eventKey())) {
            log.info("Evento {} ya procesado, se omite aviso duplicado.", evento.eventKey());
            return;
        }

        String mensaje = redactarMensaje(evento);

        AvisoDto aviso = new AvisoDto(
                evento.ordenId(),
                evento.tipoEvento(),
                mensaje,
                evento.empresaId(),
                evento.fecha(),
                LocalDateTime.now()
        );

        registrar(aviso);
        log.info("Aviso generado [empresa {}]: {}", evento.empresaId(), mensaje);
    }

    private void registrar(AvisoDto aviso) {
        recientes.addFirst(aviso);
        while (recientes.size() > MAX_AVISOS) {
            recientes.removeLast();
        }
    }

    /** Traduce el tipo de evento a un texto entendible para la persona. */
    private String redactarMensaje(OrdenEventoDto evento) {
        String tipo = evento.tipoEvento() == null ? "" : evento.tipoEvento();
        return switch (tipo) {
            case "CREADA"    -> "Se creo la orden #" + evento.ordenId() + ".";
            case "ASIGNADA"  -> "La orden #" + evento.ordenId() + " fue asignada a un tecnico.";
            case "INICIADA"  -> "Comenzo la atencion de la orden #" + evento.ordenId() + ".";
            case "CERRADA"   -> "La orden #" + evento.ordenId() + " fue cerrada.";
            case "CANCELADA" -> "La orden #" + evento.ordenId() + " fue cancelada.";
            default          -> "La orden #" + evento.ordenId() + " cambio de estado (" + tipo + ").";
        };
    }

    /** Avisos recientes, del mas nuevo al mas antiguo. */
    public synchronized List<AvisoDto> recientes() {
        return new ArrayList<>(recientes);
    }
}
