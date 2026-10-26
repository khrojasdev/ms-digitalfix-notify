package cl.duoc.digitalfix.notify;

import cl.duoc.digitalfix.notify.service.TicketDespachoService;
import cl.duoc.digitalfix.notify.service.dto.OrdenEventoDto;
import cl.duoc.digitalfix.notify.web.dto.TicketDespachoDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Ticket de despacho al tecnico cuando la orden queda ASIGNADA. */
class TicketDespachoServiceTest {

    private OrdenEventoDto evento(String eventKey, String tipo, String anterior, Long empresa, String email) {
        return new OrdenEventoDto(eventKey, tipo, 42L, "oid-sup", empresa, LocalDateTime.of(2026, 10, 26, 9, 30),
                "corr", anterior, tipo, 3L, "oid-cliente", "Router sin senal", 5L, "Pedro Soto",
                email, "+56911112222");
    }

    @Test
    void emiteElTicketConLosDatosDeLaOrden() {
        TicketDespachoService service = new TicketDespachoService();

        TicketDespachoDto ticket = service.alAsignar(
                evento("k1", "ASIGNADA", "CREADA", 7L, "pedro@digitalfix.cl")).orElseThrow();

        assertThat(ticket.numero()).isEqualTo("DSP-42-20261026093000");
        assertThat(ticket.tecnicoEmail()).isEqualTo("pedro@digitalfix.cl");
        assertThat(ticket.asunto()).isEqualTo("Despacho a terreno - orden #42");
        assertThat(ticket.texto()).contains("Hola Pedro Soto", "orden de trabajo #42", "Router sin senal");
        assertThat(ticket.reasignacion()).isFalse();
        assertThat(service.recientes(7L)).hasSize(1);
    }

    @Test
    void unaReasignacionTambienAvisaAlNuevoTecnico() {
        TicketDespachoService service = new TicketDespachoService();
        TicketDespachoDto ticket = service.alAsignar(
                evento("k2", "ASIGNADA", "ASIGNADA", 7L, "otro@digitalfix.cl")).orElseThrow();
        assertThat(ticket.reasignacion()).isTrue();
        assertThat(ticket.asunto()).startsWith("Reasignacion");
    }

    @Test
    void noRepiteElTicketSiElEventoLlegaDosVeces() {
        TicketDespachoService service = new TicketDespachoService();
        service.alAsignar(evento("k3", "ASIGNADA", "CREADA", 7L, "pedro@digitalfix.cl"));
        assertThat(service.alAsignar(evento("k3", "ASIGNADA", "CREADA", 7L, "pedro@digitalfix.cl"))).isEmpty();
        assertThat(service.recientes(null)).hasSize(1);
    }

    @Test
    void soloLasAsignacionesConCorreoGeneranTicket() {
        TicketDespachoService service = new TicketDespachoService();
        assertThat(service.alAsignar(evento("k4", "EN_DESPLAZAMIENTO", "ASIGNADA", 7L, "pedro@digitalfix.cl"))).isEmpty();
        assertThat(service.alAsignar(evento("k5", "ASIGNADA", "CREADA", 7L, null))).isEmpty();
        assertThat(service.recientes(null)).isEmpty();
    }

    @Test
    void cadaEmpresaVeSusTickets() {
        TicketDespachoService service = new TicketDespachoService();
        service.alAsignar(evento("k6", "ASIGNADA", "CREADA", 7L, "a@digitalfix.cl"));
        service.alAsignar(evento("k7", "ASIGNADA", "CREADA", 8L, "b@digitalfix.cl"));

        assertThat(service.recientes(7L)).extracting(TicketDespachoDto::tecnicoEmail).containsExactly("a@digitalfix.cl");
        assertThat(service.recientes(8L)).extracting(TicketDespachoDto::tecnicoEmail).containsExactly("b@digitalfix.cl");
    }
}
