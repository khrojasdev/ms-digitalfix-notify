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
 * Los avisos son para el cliente que creo la orden. Se registran en log y se
 * guardan en memoria para consultarlos por la API. El ticket de despacho al
 * tecnico lo arma TicketDespachoService.
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

    /** Traduce el tipo de evento a un texto entendible para el cliente. */
    private String redactarMensaje(OrdenEventoDto evento) {
        String tipo = evento.tipoEvento() == null ? "" : evento.tipoEvento();
        String orden = "la orden #" + evento.ordenId();
        String tecnico = tieneTexto(evento.tecnicoNombre()) ? evento.tecnicoNombre() : null;
        return switch (tipo) {
            case "CREADA" -> "Se creo " + orden + ".";
            case "ASIGNADA" -> tecnico == null
                    ? capitalizar(orden) + " fue asignada a un tecnico."
                    : capitalizar(orden) + (evento.esReasignacion() ? " fue reasignada a " : " fue asignada a ")
                            + tecnico + ".";
            case "EN_DESPLAZAMIENTO" -> tecnico == null
                    ? "El tecnico va en camino para " + orden + "."
                    : "El tecnico " + tecnico + " va en camino para " + orden + ".";
            case "EN_EJECUCION", "INICIADA" -> "Comenzo la atencion de " + orden + ".";
            case "CERRADA" -> capitalizar(orden) + " fue cerrada.";
            case "CANCELADA" -> capitalizar(orden) + " fue cancelada.";
            default -> capitalizar(orden) + " cambio de estado (" + tipo + ").";
        };
    }

    private static boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static String capitalizar(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** Avisos recientes, del mas nuevo al mas antiguo. */
    public synchronized List<AvisoDto> recientes() {
        return new ArrayList<>(recientes);
    }
}
