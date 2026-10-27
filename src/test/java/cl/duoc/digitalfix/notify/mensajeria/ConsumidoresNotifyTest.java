package cl.duoc.digitalfix.notify.mensajeria;

import cl.duoc.digitalfix.notify.mensajeria.ordenes.AvisoEventoConsumer;
import cl.duoc.digitalfix.notify.mensajeria.stock.StockAlertaConsumer;
import cl.duoc.digitalfix.notify.service.AvisoService;
import cl.duoc.digitalfix.notify.service.AvisoStockService;
import cl.duoc.digitalfix.notify.service.TicketDespachoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Los consumidores de notify por dominio: ordenes y stock. */
class ConsumidoresNotifyTest {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final ProcesadorDeMensajes procesador = new ProcesadorDeMensajes(3, 1);
    private final Channel canal = mock(Channel.class);

    @Test
    void asignacionGeneraAvisoConComentarioYTicket() throws Exception {
        AvisoService avisos = new AvisoService();
        TicketDespachoService tickets = new TicketDespachoService();
        AvisoEventoConsumer consumidor = new AvisoEventoConsumer(avisos, tickets, json, procesador, "ordenes.avisos.queue");

        String mensaje = "{\"eventKey\":\"e1\",\"tipoEvento\":\"ASIGNADA\",\"ordenId\":9,\"empresaId\":7,"
                + "\"fecha\":\"2026-10-26T10:00:00\",\"estadoAnterior\":\"CREADA\",\"servicioId\":3,"
                + "\"tecnicoNombre\":\"Pedro Soto\",\"tecnicoEmail\":\"pedro@digitalfix.cl\",\"nota\":\"Llevar escalera\",\"campoNuevo\":1}";
        consumidor.consumirEvento(mensaje, canal, 11L);

        assertThat(avisos.recientes()).hasSize(1);
        assertThat(avisos.recientes().get(0).mensaje())
                .isEqualTo("La orden #9 fue asignada a Pedro Soto. Comentario: Llevar escalera");
        assertThat(tickets.recientes(7L)).hasSize(1);
        verify(canal).basicAck(11L, false);
    }

    @Test
    void eventoDeOrdenInvalidoVaALaDlq() throws Exception {
        AvisoEventoConsumer consumidor = new AvisoEventoConsumer(new AvisoService(), new TicketDespachoService(),
                json, procesador, "ordenes.avisos.queue");
        consumidor.consumirEvento("{roto", canal, 12L);
        consumidor.consumirEvento("{\"tipoEvento\":\"CREADA\"}", canal, 13L);
        verify(canal).basicNack(12L, false, false);
        verify(canal).basicNack(13L, false, false);
    }

    @Test
    void alertaDeStockSeRegistraOVaALaDlq() throws Exception {
        AvisoStockService stock = new AvisoStockService();
        StockAlertaConsumer consumidor = new StockAlertaConsumer(stock, json, procesador, "notify.stock.queue");

        consumidor.consumirAlerta("{\"repuestoId\":1,\"empresaId\":7,\"sku\":\"SKU-1\",\"nombre\":\"Breaker\","
                + "\"stock\":1,\"stockMinimo\":5,\"fecha\":\"2026-10-27T10:00:00Z\"}", canal, 21L);
        consumidor.consumirAlerta("{\"sku\":\"SKU-1\"}", canal, 22L);

        assertThat(stock.recientes()).hasSize(1);
        verify(canal).basicAck(21L, false);
        verify(canal).basicNack(22L, false, false);
    }
}
