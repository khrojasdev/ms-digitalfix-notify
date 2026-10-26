package cl.duoc.digitalfix.notify;

import cl.duoc.digitalfix.notify.service.AvisoService;
import cl.duoc.digitalfix.notify.service.dto.OrdenEventoDto;
import cl.duoc.digitalfix.notify.web.dto.AvisoDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la logica de avisos, sin levantar RabbitMQ: se valida el mapeo de
 * evento a mensaje, la idempotencia por eventKey y el registro en memoria.
 */
class AvisoServiceTest {

    private OrdenEventoDto evento(String eventKey, String tipo, Long ordenId) {
        return new OrdenEventoDto(eventKey, tipo, ordenId, "oid-1", 7L,
                LocalDateTime.of(2026, 10, 18, 12, 0), "corr-1");
    }

    @Test
    void generaUnAvisoLegiblePorCadaTipoDeEvento() {
        AvisoService service = new AvisoService();

        service.generarAviso(evento("k1", "CREADA", 100L));
        service.generarAviso(evento("k2", "CERRADA", 100L));

        List<AvisoDto> avisos = service.recientes();
        assertThat(avisos).hasSize(2);
        // El mas reciente queda primero.
        assertThat(avisos.get(0).mensaje()).contains("cerrada");
        assertThat(avisos.get(1).mensaje()).contains("creo la orden #100");
        assertThat(avisos.get(0).empresaId()).isEqualTo(7L);
    }

    @Test
    void esIdempotentePorEventKey() {
        AvisoService service = new AvisoService();

        service.generarAviso(evento("misma-clave", "ASIGNADA", 200L));
        service.generarAviso(evento("misma-clave", "ASIGNADA", 200L));

        assertThat(service.recientes()).hasSize(1);
    }

    @Test
    void tipoDesconocidoNoRompeYGeneraMensajeGenerico() {
        AvisoService service = new AvisoService();

        service.generarAviso(evento("k9", "ESTADO_NUEVO", 300L));

        assertThat(service.recientes()).hasSize(1);
        assertThat(service.recientes().get(0).mensaje()).contains("cambio de estado");
    }

    private OrdenEventoDto conTecnico(String eventKey, String tipo, String anterior) {
        return new OrdenEventoDto(eventKey, tipo, 400L, "oid-sup", 7L, LocalDateTime.of(2026, 10, 26, 9, 0),
                "corr-2", anterior, tipo, 3L, "oid-cliente", "No enciende", 5L, "Pedro Soto",
                "pedro@digitalfix.cl", "+56911112222");
    }

    @Test
    void nombraAlTecnicoEnLaAsignacionYElDesplazamiento() {
        AvisoService service = new AvisoService();

        service.generarAviso(conTecnico("a1", "ASIGNADA", "CREADA"));
        service.generarAviso(conTecnico("a2", "ASIGNADA", "ASIGNADA"));
        service.generarAviso(conTecnico("a3", "EN_DESPLAZAMIENTO", "ASIGNADA"));
        service.generarAviso(conTecnico("a4", "EN_EJECUCION", "EN_DESPLAZAMIENTO"));

        List<AvisoDto> avisos = service.recientes();
        assertThat(avisos.get(3).mensaje()).isEqualTo("La orden #400 fue asignada a Pedro Soto.");
        assertThat(avisos.get(2).mensaje()).isEqualTo("La orden #400 fue reasignada a Pedro Soto.");
        assertThat(avisos.get(1).mensaje()).isEqualTo("El tecnico Pedro Soto va en camino para la orden #400.");
        assertThat(avisos.get(0).mensaje()).isEqualTo("Comenzo la atencion de la orden #400.");
    }

    @Test
    void eventoAntiguoSinTecnicoSigueFuncionando() {
        AvisoService service = new AvisoService();
        service.generarAviso(evento("v1", "ASIGNADA", 500L));
        service.generarAviso(evento("v2", "INICIADA", 500L));

        assertThat(service.recientes().get(1).mensaje()).isEqualTo("La orden #500 fue asignada a un tecnico.");
        assertThat(service.recientes().get(0).mensaje()).isEqualTo("Comenzo la atencion de la orden #500.");
    }
}
