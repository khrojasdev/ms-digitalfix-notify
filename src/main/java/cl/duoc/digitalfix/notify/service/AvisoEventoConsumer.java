package cl.duoc.digitalfix.notify.service;

import cl.duoc.digitalfix.notify.service.dto.OrdenEventoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Consumidor de eventos de orden para el dominio de notificaciones.
 *
 * Maneja la confirmacion de mensajes de forma explicita (exigencia de la pauta):
 *  - ACK manual cuando el aviso se genera con exito.
 *  - NACK sin reencolar cuando algo falla, de modo que el mensaje caiga en la
 *    DLQ configurada y quede registrado en el log, en vez de reprocesarse en
 *    bucle.
 */
@Service
public class AvisoEventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(AvisoEventoConsumer.class);

    private final AvisoService avisoService;
    private final TicketDespachoService ticketDespachoService;
    private final ObjectMapper objectMapper;

    public AvisoEventoConsumer(AvisoService avisoService, TicketDespachoService ticketDespachoService,
                               ObjectMapper objectMapper) {
        this.avisoService = avisoService;
        this.ticketDespachoService = ticketDespachoService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${amqp.queue.avisos}")
    public void consumirEvento(String mensajeJson,
                               Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            OrdenEventoDto evento = objectMapper.readValue(mensajeJson, OrdenEventoDto.class);
            // Aviso al cliente y, si es una asignacion, ticket de despacho al tecnico.
            avisoService.generarAviso(evento);
            ticketDespachoService.alAsignar(evento);

            // Confirmacion manual de exito.
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("No se pudo procesar el evento de aviso, se envia a la DLQ. Motivo: {}",
                    e.getMessage(), e);
            // NACK sin reencolar: el mensaje se va a la DLQ para inspeccion posterior.
            channel.basicNack(tag, false, false);
        }
    }
}
