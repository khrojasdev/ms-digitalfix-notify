package cl.duoc.digitalfix.notify.mensajeria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Politica de confirmacion comun a todos los consumidores de este servicio.
 * Cada consumidor solo dice que hacer con el mensaje; la decision de ACK,
 * reintento, NACK o DLQ se toma aqui, siempre igual:
 *
 * <ul>
 *   <li><b>Exito</b>: ACK.</li>
 *   <li><b>Mensaje invalido</b> (no se puede leer, o le faltan datos
 *       obligatorios): NACK sin reencolar, va a la DLQ de inmediato. Reintentar
 *       no sirve: va a fallar igual.</li>
 *   <li><b>Falla transitoria</b> (la base de datos o la red no responden un
 *       momento): se reintenta hasta {@code amqp.reintentos.maximo} veces, con
 *       espera creciente (1 s, 2 s, 4 s...). Si sigue fallando, NACK a la DLQ.</li>
 *   <li><b>Cualquier otro error</b>: NACK a la DLQ sin reintentar.</li>
 * </ul>
 *
 * Nunca se reencola (requeue=false): un mensaje que vuelve a la cola sin
 * control se procesa en bucle. Todo lo que va a la DLQ queda en el log con la
 * cola, el motivo y la cantidad de intentos.
 */
@Component
public class ProcesadorDeMensajes {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorDeMensajes.class);

    /**
     * Excepciones que indican una falla pasajera. Se comparan por nombre para
     * no depender de librerias que este servicio quiza no tenga.
     */
    private static final Set<String> TRANSITORIAS = Set.of(
            "org.springframework.dao.TransientDataAccessException",
            "org.springframework.dao.RecoverableDataAccessException",
            "org.springframework.dao.DataAccessResourceFailureException",
            "org.springframework.transaction.CannotCreateTransactionException",
            "java.sql.SQLTransientException",
            "java.sql.SQLRecoverableException",
            "java.net.ConnectException",
            "java.net.SocketTimeoutException",
            "java.util.concurrent.TimeoutException");

    /** Como termino el procesamiento de un mensaje. */
    public enum Resultado { CONFIRMADO, DLQ_INVALIDO, DLQ_TRAS_REINTENTOS, DLQ_ERROR }

    /** El trabajo que hace el consumidor con un mensaje. */
    @FunctionalInterface
    public interface Trabajo {
        void ejecutar() throws Exception;
    }

    private final int maximoIntentos;
    private final long esperaInicialMs;

    public ProcesadorDeMensajes(@Value("${amqp.reintentos.maximo:3}") int maximoIntentos,
                                @Value("${amqp.reintentos.espera-ms:1000}") long esperaInicialMs) {
        this.maximoIntentos = Math.max(1, maximoIntentos);
        this.esperaInicialMs = Math.max(0, esperaInicialMs);
    }

    public Resultado procesar(String cola, Channel channel, long tag, Trabajo trabajo) throws IOException {
        int intento = 1;
        while (true) {
            try {
                trabajo.ejecutar();
                channel.basicAck(tag, false);
                return Resultado.CONFIRMADO;
            } catch (Exception e) {
                if (esInvalido(e)) {
                    log.error("[{}] mensaje invalido, va a la DLQ sin reintentar. Motivo: {}", cola, e.getMessage());
                    channel.basicNack(tag, false, false);
                    return Resultado.DLQ_INVALIDO;
                }
                if (esTransitoria(e)) {
                    if (intento < maximoIntentos) {
                        long espera = esperaInicialMs * (1L << (intento - 1));
                        log.warn("[{}] falla transitoria (intento {} de {}), se reintenta en {} ms. Motivo: {}",
                                cola, intento, maximoIntentos, espera, e.getMessage());
                        if (!dormir(espera)) {
                            channel.basicNack(tag, false, false);
                            return Resultado.DLQ_TRAS_REINTENTOS;
                        }
                        intento++;
                        continue;
                    }
                    log.error("[{}] falla transitoria tras {} intentos, va a la DLQ. Motivo: {}",
                            cola, maximoIntentos, e.getMessage(), e);
                    channel.basicNack(tag, false, false);
                    return Resultado.DLQ_TRAS_REINTENTOS;
                }
                log.error("[{}] error al procesar, va a la DLQ sin reintentar. Motivo: {}", cola, e.getMessage(), e);
                channel.basicNack(tag, false, false);
                return Resultado.DLQ_ERROR;
            }
        }
    }

    static boolean esInvalido(Throwable e) {
        return e instanceof MensajeInvalido
                || e instanceof JsonProcessingException
                || e instanceof IllegalArgumentException;
    }

    static boolean esTransitoria(Throwable e) {
        for (Throwable actual = e; actual != null; actual = actual.getCause() == actual ? null : actual.getCause()) {
            for (Class<?> tipo = actual.getClass(); tipo != null; tipo = tipo.getSuperclass()) {
                if (TRANSITORIAS.contains(tipo.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean dormir(long ms) {
        try {
            Thread.sleep(ms);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
