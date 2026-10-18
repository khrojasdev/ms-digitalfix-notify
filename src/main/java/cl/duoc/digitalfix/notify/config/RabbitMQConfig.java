package cl.duoc.digitalfix.notify.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de mensajeria del microservicio de notificaciones.
 *
 * Toda la nomenclatura (exchange, cola, DLQ y routing key) se lee desde
 * application.yml bajo el prefijo amqp.*, de modo que ningun nombre quede
 * escrito a mano dentro del codigo de negocio.
 *
 * notify es un consumidor del exchange de negocio (topic) que ya publica
 * ms-digitalfix-workorders. Se enlaza a los eventos de orden para generar
 * avisos de cambio de estado.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${amqp.exchange.negocio}")
    private String exchangeName;

    @Value("${amqp.queue.avisos}")
    private String queueName;

    @Value("${amqp.queue.avisos-dlq}")
    private String dlqName;

    @Value("${amqp.routing-key.avisos}")
    private String routingKey;

    // 1. Exchange de negocio (compartido con audit y report). Tipo topic.
    @Bean
    public TopicExchange negocioExchange() {
        return new TopicExchange(exchangeName);
    }

    // 2. Dead Letter Queue: aqui aterrizan los avisos que no se pudieron procesar.
    @Bean
    public Queue avisosDlq() {
        return QueueBuilder.durable(dlqName).build();
    }

    // 3. Cola principal de avisos, con enrutamiento de muertos hacia la DLQ.
    @Bean
    public Queue avisosQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", dlqName)
                .build();
    }

    // 4. Binding: enlaza la cola de avisos al exchange por la routing key configurada.
    @Bean
    public Binding avisosBinding(Queue avisosQueue, TopicExchange negocioExchange) {
        return BindingBuilder.bind(avisosQueue).to(negocioExchange).with(routingKey);
    }

    // RabbitAdmin declara la topologia al arrancar y permite inspeccionar las colas.
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
