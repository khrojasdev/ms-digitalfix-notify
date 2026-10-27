package cl.duoc.digitalfix.notify.mensajeria.stock;

import cl.duoc.digitalfix.notify.mensajeria.MensajeInvalido;
import cl.duoc.digitalfix.notify.mensajeria.ProcesadorDeMensajes;
import cl.duoc.digitalfix.notify.service.AvisoStockService;
import cl.duoc.digitalfix.notify.service.dto.AlertaStockDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Dominio stock: las alertas de stock bajo minimo que publica catalog por el
 * exchange direct. ACK, reintento y DLQ los decide {@link ProcesadorDeMensajes}.
 */
@Component
public class StockAlertaConsumer {

    private final AvisoStockService avisoStockService;
    private final ObjectMapper objectMapper;
    private final ProcesadorDeMensajes procesador;
    private final String cola;

    public StockAlertaConsumer(AvisoStockService avisoStockService, ObjectMapper objectMapper,
                               ProcesadorDeMensajes procesador, @Value("${amqp.queue.stock}") String cola) {
        this.avisoStockService = avisoStockService;
        this.objectMapper = objectMapper;
        this.procesador = procesador;
        this.cola = cola;
    }

    @RabbitListener(queues = "${amqp.queue.stock}")
    public void consumirAlerta(String mensajeJson, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        procesador.procesar(cola, channel, tag, () -> {
            AlertaStockDto alerta = objectMapper.readValue(mensajeJson, AlertaStockDto.class);
            if (alerta.repuestoId() == null || alerta.empresaId() == null) {
                throw new MensajeInvalido("alerta de stock incompleta: faltan repuesto o empresa");
            }
            avisoStockService.registrar(alerta);
        });
    }
}
