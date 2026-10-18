# ms-digitalfix-notify

**Proyecto:** DigitalFix - Gestion de ordenes de trabajo para una red de 20 empresas de mantenimiento electrico
**Componente:** Microservicio de notificaciones
**Asignatura:** DSY1107 Desarrollo Cloud Native I - Duoc UC

## Descripcion

Consume los eventos de orden que publica `ms-digitalfix-workorders` y genera
**avisos de cambio de estado**. Es el tercer consumidor de la topologia de
mensajeria, junto a `audit` y `report`.

## Mensajeria (RabbitMQ)

| Elemento | Nombre | Tipo |
|---|---|---|
| Exchange | `ordenes.negocio.exchange` | topic (compartido) |
| Cola | `ordenes.avisos.queue` | durable |
| DLQ | `ordenes.avisos.dlq` | durable |
| Routing key | `ordenes.eventos.*` | |

- Toda la nomenclatura esta centralizada en `application.yml` bajo `amqp.*`;
  ningun nombre se escribe a mano en el codigo.
- La topologia (exchange, cola, DLQ y binding) se declara con beans en
  `config/RabbitMQConfig.java`.
- El consumidor (`AvisoEventoConsumer`) usa **ACK manual**: confirma con
  `basicAck` al generar el aviso y hace `basicNack` sin reencolar ante un error,
  de modo que el mensaje caiga en la DLQ y quede en el log.
- La logica de negocio (`AvisoService`) no conoce RabbitMQ: recibe el evento ya
  deserializado. Es idempotente por `eventKey`.

## API

| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/avisos` | Avisos recientes generados (demo del consumo) |
| GET | `/actuator/health` | Estado del servicio |

## Tecnologias

Java 21, Spring Boot 3.3.5, Spring AMQP (RabbitMQ), Actuator.

## Variables de entorno

| Variable | Defecto | Uso |
|---|---|---|
| `RABBITMQ_HOST` | `localhost` | Host de RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Puerto de RabbitMQ |
| `RABBITMQ_USER` | `guest` | Usuario |
| `RABBITMQ_PASSWORD` | `guest` | Clave |
| `PUERTO` | `8084` | Puerto HTTP del servicio |

## Pendiente (siguiente entrega)

- Envio real del aviso por correo (plantilla Thymeleaf).
- Generacion de **ticket de despacho** e **informe tecnico** en PDF (OpenPDF),
  con sus propias colas y DLQ, incorporando un exchange de tipo **direct**.

## Integrantes

| Integrante | GitHub |
|---|---|
| Kevin Rojas | @khrojasdev |
| Christopher Perez | @ChrisPerezV |
| Diego Lopez | @DiegoLopez-f |
