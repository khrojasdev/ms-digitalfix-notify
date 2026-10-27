package cl.duoc.digitalfix.notify.mensajeria;

import com.fasterxml.jackson.core.JsonParseException;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import java.net.ConnectException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** La politica de ACK, reintento y DLQ que usan todos los consumidores. */
class ProcesadorDeMensajesTest {

    private final ProcesadorDeMensajes procesador = new ProcesadorDeMensajes(3, 1);
    private final Channel canal = mock(Channel.class);

    @Test
    void exitoConfirmaConAck() throws Exception {
        assertThat(procesador.procesar("cola", canal, 1L, () -> { }))
                .isEqualTo(ProcesadorDeMensajes.Resultado.CONFIRMADO);
        verify(canal).basicAck(1L, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void mensajeInvalidoVaALaDlqSinReintentar() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        var resultado = procesador.procesar("cola", canal, 2L, () -> {
            intentos.incrementAndGet();
            throw new MensajeInvalido("falta la empresa");
        });
        assertThat(resultado).isEqualTo(ProcesadorDeMensajes.Resultado.DLQ_INVALIDO);
        assertThat(intentos).hasValue(1);
        verify(canal).basicNack(2L, false, false);
    }

    @Test
    void jsonIlegibleTambienEsInvalido() throws Exception {
        var resultado = procesador.procesar("cola", canal, 3L, () -> {
            throw new JsonParseException(null, "no es json");
        });
        assertThat(resultado).isEqualTo(ProcesadorDeMensajes.Resultado.DLQ_INVALIDO);
        verify(canal).basicNack(3L, false, false);
    }

    @Test
    void fallaTransitoriaSeReintentaYSiSeRecuperaConfirma() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        var resultado = procesador.procesar("cola", canal, 4L, () -> {
            if (intentos.incrementAndGet() < 3) {
                throw new ConnectException("el servicio de correo no responde");
            }
        });
        assertThat(resultado).isEqualTo(ProcesadorDeMensajes.Resultado.CONFIRMADO);
        assertThat(intentos).hasValue(3);
        verify(canal).basicAck(4L, false);
    }

    @Test
    void fallaTransitoriaQuePersisteVaALaDlqTrasLosReintentos() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        var resultado = procesador.procesar("cola", canal, 5L, () -> {
            intentos.incrementAndGet();
            throw new RuntimeException("envoltorio", new java.sql.SQLTransientConnectionException("timeout"));
        });
        assertThat(resultado).isEqualTo(ProcesadorDeMensajes.Resultado.DLQ_TRAS_REINTENTOS);
        assertThat(intentos).hasValue(3);
        verify(canal).basicNack(5L, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void otroErrorVaALaDlqSinReintentar() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        var resultado = procesador.procesar("cola", canal, 6L, () -> {
            intentos.incrementAndGet();
            throw new IllegalStateException("estado imposible");
        });
        assertThat(resultado).isEqualTo(ProcesadorDeMensajes.Resultado.DLQ_ERROR);
        assertThat(intentos).hasValue(1);
        verify(canal).basicNack(6L, false, false);
    }
}
