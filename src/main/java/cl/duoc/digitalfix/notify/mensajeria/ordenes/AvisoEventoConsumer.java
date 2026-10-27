package cl.duoc.digitalfix.notify.mensajeria.ordenes;

import cl.duoc.digitalfix.notify.mensajeria.MensajeInvalido;
import cl.duoc.digitalfix.notify.mensajeria.ProcesadorDeMensajes;
import cl.duoc.digitalfix.notify.service.AvisoService;
import cl.duoc.digitalfix.notify.service.TicketDespachoService;
import cl.duoc.digitalfix.notify.service.dto.OrdenEventoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio ordenes: cada evento de orden genera el aviso al cliente y, si es
 * una asignacion, el ticket de despacho al tecnico. ACK, reintento y DLQ los
 * decide {@link ProcesadorDeMensajes}.
 */
@Component
public class AvisoEventoConsumer {

    private final AvisoService avisoService;
    private final TicketDespachoService ticketDespachoService;
    private final ObjectMapper objectMapper;
    private final ProcesadorDeMensajes procesador;
    private final String cola;

    public AvisoEventoConsumer(AvisoService avisoService, TicketDespachoService ticketDespachoService,
                               ObjectMapper objectMapper, ProcesadorDeMensajes procesador,
                               @Value("${amqp.queue.avisos}") String cola) {
        this.avisoService = avisoService;
        this.ticketDespachoService = ticketDespachoService;
        this.objectMapper = objectMapper;
        this.procesador = procesador;
        this.cola = cola;
    }

    @RabbitListener(queues = "${amqp.queue.avisos}")
    public void consumirEvento(String mensajeJson, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        procesador.procesar(cola, channel, tag, () -> {
            OrdenEventoDto evento = objectMapper.readValue(mensajeJson, OrdenEventoDto.class);
            if (evento.ordenId() == null || evento.tipoEvento() == null || evento.empresaId() == null) {
                throw new MensajeInvalido("evento de orden incompleto: faltan orden, tipo o empresa");
            }
            avisoService.generarAviso(evento);
            ticketDespachoService.alAsignar(evento);
        });
    }
}
