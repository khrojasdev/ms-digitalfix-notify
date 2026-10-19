package cl.duoc.digitalfix.notify.service;

import cl.duoc.digitalfix.notify.service.dto.AlertaStockDto;
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
 * Consumidor de las alertas de stock que publica catalog por el exchange direct.
 *
 * Mismo manejo explicito de confirmacion que el resto de los consumidores:
 * ACK al procesar con exito y NACK sin reencolar ante error, de modo que el
 * mensaje caiga en la DLQ de stock.
 */
@Service
public class StockAlertaConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockAlertaConsumer.class);

    private final AvisoStockService avisoStockService;
    private final ObjectMapper objectMapper;

    public StockAlertaConsumer(AvisoStockService avisoStockService, ObjectMapper objectMapper) {
        this.avisoStockService = avisoStockService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${amqp.queue.stock}")
    public void consumirAlerta(String mensajeJson,
                               Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            AlertaStockDto alerta = objectMapper.readValue(mensajeJson, AlertaStockDto.class);
            avisoStockService.registrar(alerta);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("No se pudo procesar la alerta de stock, se envia a la DLQ. Motivo: {}",
                    e.getMessage(), e);
            channel.basicNack(tag, false, false);
        }
    }
}
