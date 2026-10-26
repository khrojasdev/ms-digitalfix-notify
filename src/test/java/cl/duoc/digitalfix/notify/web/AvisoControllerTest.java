package cl.duoc.digitalfix.notify.web;

import cl.duoc.digitalfix.notify.service.AvisoService;
import cl.duoc.digitalfix.notify.service.AvisoStockService;
import cl.duoc.digitalfix.notify.service.dto.AlertaStockDto;
import cl.duoc.digitalfix.notify.service.dto.OrdenEventoDto;
import cl.duoc.digitalfix.notify.web.dto.AvisoDto;
import cl.duoc.digitalfix.notify.web.dto.AvisoStockDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cada empresa ve solo sus avisos, aunque se guarden todos juntos. */
class AvisoControllerTest {

    private AvisoService avisos;
    private AvisoStockService stock;
    private AvisoController controller;

    @BeforeEach
    void avisosDeDosEmpresas() {
        avisos = new AvisoService();
        stock = new AvisoStockService();
        controller = new AvisoController(avisos, stock);

        avisos.generarAviso(new OrdenEventoDto("a1", "CREADA", 10L, "oid-a", 7L, LocalDateTime.now(), null));
        avisos.generarAviso(new OrdenEventoDto("b1", "CREADA", 20L, "oid-b", 8L, LocalDateTime.now(), null));
        stock.registrar(new AlertaStockDto(1L, 7L, "SKU-A", "Breaker", 1, 5, Instant.now()));
        stock.registrar(new AlertaStockDto(2L, 8L, "SKU-B", "Cable", 0, 3, Instant.now()));
    }

    @Test
    void losAvisosDeOrdenSeFiltranPorEmpresa() {
        assertThat(controller.recientes(7L)).extracting(AvisoDto::ordenId).containsExactly(10L);
        assertThat(controller.recientes(8L)).extracting(AvisoDto::ordenId).containsExactly(20L);
        assertThat(controller.recientes(99L)).isEmpty();
    }

    @Test
    void lasAlertasDeStockSeFiltranPorEmpresa() {
        assertThat(controller.stock(7L)).extracting(AvisoStockDto::sku).containsExactly("SKU-A");
        assertThat(controller.stock(8L)).extracting(AvisoStockDto::sku).containsExactly("SKU-B");
    }

    @Test
    void sinCabeceraDevuelveTodo() {
        assertThat(controller.recientes(null)).hasSize(2);
        assertThat(controller.stock(null)).hasSize(2);
    }

    @Test
    void leeLaEmpresaDeLaCabeceraQuePoneElBff() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(get("/api/avisos").header("X-Company-Id", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ordenId").value(20));

        mvc.perform(get("/api/avisos/stock").header("X-Company-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sku").value("SKU-A"));
    }
}
