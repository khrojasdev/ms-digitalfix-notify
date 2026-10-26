package cl.duoc.digitalfix.notify;

import cl.duoc.digitalfix.notify.service.AvisoEventoConsumer;
import cl.duoc.digitalfix.notify.service.AvisoService;
import cl.duoc.digitalfix.notify.service.TicketDespachoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** El evento ASIGNADA genera el aviso al cliente y el ticket al tecnico, y se confirma. */
class AvisoEventoConsumerTest {

    @Test
    void asignacionGeneraAvisoYTicket() throws Exception {
        AvisoService avisos = new AvisoService();
        TicketDespachoService tickets = new TicketDespachoService();
        Channel canal = mock(Channel.class);
        AvisoEventoConsumer consumidor = new AvisoEventoConsumer(avisos, tickets, new ObjectMapper().findAndRegisterModules());

        String json = "{\"eventKey\":\"e1\",\"tipoEvento\":\"ASIGNADA\",\"ordenId\":9,\"empresaId\":7,"
                + "\"fecha\":\"2026-10-26T10:00:00\",\"estadoAnterior\":\"CREADA\",\"servicioId\":3,"
                + "\"tecnicoNombre\":\"Pedro Soto\",\"tecnicoEmail\":\"pedro@digitalfix.cl\",\"campoNuevo\":1}";
        consumidor.consumirEvento(json, canal, 11L);

        assertThat(avisos.recientes()).hasSize(1);
        assertThat(avisos.recientes().get(0).mensaje()).isEqualTo("La orden #9 fue asignada a Pedro Soto.");
        assertThat(tickets.recientes(7L)).hasSize(1);
        verify(canal).basicAck(11L, false);
    }

    @Test
    void mensajeInvalidoVaALaDlq() throws Exception {
        Channel canal = mock(Channel.class);
        AvisoEventoConsumer consumidor = new AvisoEventoConsumer(new AvisoService(), new TicketDespachoService(),
                new ObjectMapper());
        consumidor.consumirEvento("{roto", canal, 12L);
        verify(canal).basicNack(12L, false, false);
    }
}
