package cl.duoc.digitalfix.notify.service;

import cl.duoc.digitalfix.notify.service.dto.OrdenEventoDto;
import cl.duoc.digitalfix.notify.web.dto.TicketDespachoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Genera el ticket de despacho a terreno cuando una orden queda ASIGNADA a un
 * tecnico (regla del caso: notify envia el ticket al tecnico e informa al
 * cliente; el aviso al cliente lo arma AvisoService).
 *
 * El "envio" queda en el log con destinatario, asunto y cuerpo, y el ticket se
 * guarda en memoria para consultarlo por GET /api/avisos/despachos. El correo
 * real (SMTP) y el PDF se pueden enchufar en enviar() sin tocar el resto.
 *
 * Idempotente por eventKey: si RabbitMQ entrega el evento dos veces, se emite
 * un solo ticket.
 */
@Service
public class TicketDespachoService {

    private static final Logger log = LoggerFactory.getLogger(TicketDespachoService.class);
    private static final int MAX_TICKETS = 100;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final Deque<TicketDespachoDto> recientes = new ArrayDeque<>();
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    /**
     * @return el ticket emitido, o vacio si el evento no es una asignacion, no
     *         trae al tecnico o ya se habia procesado
     */
    public synchronized Optional<TicketDespachoDto> alAsignar(OrdenEventoDto evento) {
        if (!"ASIGNADA".equalsIgnoreCase(evento.tipoEvento())) {
            return Optional.empty();
        }
        if (evento.tecnicoEmail() == null || evento.tecnicoEmail().isBlank()) {
            log.warn("La orden #{} fue asignada pero el evento no trae el correo del tecnico; no se emite ticket.",
                    evento.ordenId());
            return Optional.empty();
        }
        if (evento.eventKey() != null && !procesados.add(evento.eventKey())) {
            log.info("Evento {} ya procesado, se omite ticket duplicado.", evento.eventKey());
            return Optional.empty();
        }

        TicketDespachoDto ticket = armar(evento);
        enviar(ticket);
        recientes.addFirst(ticket);
        while (recientes.size() > MAX_TICKETS) {
            recientes.removeLast();
        }
        return Optional.of(ticket);
    }

    private TicketDespachoDto armar(OrdenEventoDto e) {
        LocalDateTime asignacion = e.fecha() == null ? LocalDateTime.now() : e.fecha();
        String numero = "DSP-" + e.ordenId() + "-" + asignacion.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        boolean reasignacion = e.esReasignacion();
        String asunto = (reasignacion ? "Reasignacion" : "Despacho") + " a terreno - orden #" + e.ordenId();

        StringBuilder texto = new StringBuilder()
                .append("Hola ").append(e.tecnicoNombre() == null ? "" : e.tecnicoNombre()).append(",\n\n")
                .append(reasignacion ? "Se te reasigno" : "Se te asigno")
                .append(" la orden de trabajo #").append(e.ordenId()).append(".\n\n")
                .append("Ticket: ").append(numero).append('\n')
                .append("Servicio: ").append(e.servicioId() == null ? "-" : "#" + e.servicioId()).append('\n')
                .append("Asignada el: ").append(asignacion.format(FORMATO)).append('\n');
        if (e.comentarios() != null && !e.comentarios().isBlank()) {
            texto.append("Detalle del cliente: ").append(e.comentarios().trim()).append('\n');
        }
        texto.append("\nAl salir a terreno, el supervisor marcara la orden EN_DESPLAZAMIENTO.\n")
                .append("\nDigitalFix");

        return new TicketDespachoDto(numero, e.ordenId(), e.empresaId(), e.servicioId(), e.tecnicoId(),
                e.tecnicoNombre(), e.tecnicoEmail(), e.tecnicoTelefono(), e.creadorId(), e.comentarios(),
                reasignacion, asignacion, LocalDateTime.now(), asunto, texto.toString());
    }

    /** Punto de envio. Hoy deja constancia en el log; aqui va el SMTP cuando exista. */
    private void enviar(TicketDespachoDto ticket) {
        log.info("Ticket de despacho {} enviado a {} <{}> [empresa {}]: {}",
                ticket.numero(), ticket.tecnicoNombre(), ticket.tecnicoEmail(), ticket.empresaId(), ticket.asunto());
    }

    /** Tickets recientes de la empresa (o todos si no viene empresa), del mas nuevo al mas antiguo. */
    public synchronized List<TicketDespachoDto> recientes(Long empresaId) {
        List<TicketDespachoDto> lista = new ArrayList<>();
        for (TicketDespachoDto t : recientes) {
            if (empresaId == null || empresaId.equals(t.empresaId())) {
                lista.add(t);
            }
        }
        return lista;
    }
}
