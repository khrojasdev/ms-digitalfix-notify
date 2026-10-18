package cl.duoc.digitalfix.notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de la cola que consume las alertas de stock que publica
 * ms-digitalfix-catalog.
 *
 * A diferencia de los avisos de orden (que llegan por un exchange topic), las
 * alertas de stock viajan por un exchange DIRECT con una routing key exacta.
 * Se mantiene en una clase de configuracion aparte para no mezclar las dos
 * topologias. Los nombres se leen desde application.yml bajo amqp.*.
 */
@Configuration
public class StockRabbitConfig {

    @Value("${amqp.exchange.stock}")
    private String stockExchangeName;

    @Value("${amqp.queue.stock}")
    private String stockQueueName;

    @Value("${amqp.queue.stock-dlq}")
    private String stockDlqName;

    @Value("${amqp.routing-key.stock-bajo-minimo}")
    private String stockRoutingKey;

    @Bean
    public DirectExchange stockExchange() {
        return new DirectExchange(stockExchangeName);
    }

    @Bean
    public Queue stockDlq() {
        return QueueBuilder.durable(stockDlqName).build();
    }

    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable(stockQueueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", stockDlqName)
                .build();
    }

    @Bean
    public Binding stockBinding(Queue stockQueue, DirectExchange stockExchange) {
        return BindingBuilder.bind(stockQueue).to(stockExchange).with(stockRoutingKey);
    }
}
