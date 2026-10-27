# ms-digitalfix-notify

**Proyecto:** DigitalFix - Gestion de ordenes de trabajo para una red de 20 empresas de mantenimiento electrico
**Componente:** Microservicio de notificaciones
**Asignatura:** DSY1107 Desarrollo Cloud Native I - Duoc UC

## Descripcion

Consume los eventos de orden que publica `ms-digitalfix-workorders` y genera
**avisos de cambio de estado**. Es el tercer consumidor de la topologia de
mensajeria, junto a `audit` y `report`.

## Mensajeria (RabbitMQ)

notify consume de dos exchanges distintos: los eventos de orden (topic) y las
alertas de stock de catalog (direct).

| Origen | Exchange | Tipo | Cola | DLQ | Routing key |
|---|---|---|---|---|---|
| Eventos de orden | `ordenes.negocio.exchange` | topic | `ordenes.avisos.queue` | `ordenes.avisos.dlq` | `ordenes.eventos.*` |
| Alertas de stock | `catalogo.stock.exchange` | direct | `notify.stock.queue` | `notify.stock.dlq` | `stock.bajo-minimo` |

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
| GET | `/api/avisos` | Avisos de cambio de estado recientes |
| GET | `/api/avisos/stock` | Avisos de stock bajo minimo recientes |
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

## Mensajeria (RabbitMQ)

Nombres en `application.yml` (`amqp.*`); beans en `config/RabbitMQConfig` (ordenes) y
`config/StockRabbitConfig` (stock); consumidores agrupados por dominio en `mensajeria`.

| Dominio | Exchange | Routing key | Cola | DLQ | Consumidor |
|---|---|---|---|---|---|
| Ordenes | `ordenes.negocio.exchange` (topic) | `ordenes.eventos.*` | `ordenes.avisos.queue` | `ordenes.avisos.dlq` | `mensajeria.ordenes.AvisoEventoConsumer` |
| Stock | `catalogo.stock.exchange` (direct) | `stock.bajo-minimo` | `notify.stock.queue` | `notify.stock.dlq` | `mensajeria.stock.StockAlertaConsumer` |

Cada evento de orden genera el aviso al cliente (con el comentario del cambio, si
lo hubo) y, si es una asignacion, el ticket de despacho al tecnico.

| Situacion | Decision (`mensajeria.ProcesadorDeMensajes`) |
|---|---|
| Procesado | `ACK` |
| Mensaje invalido (JSON ilegible o sin orden, tipo o empresa) | `NACK` sin reencolar: a la DLQ de inmediato |
| Falla transitoria (red, servicio externo) | Hasta `amqp.reintentos.maximo` intentos (1 s, 2 s, 4 s...), luego a la DLQ |
| Otro error | `NACK` a la DLQ sin reintentar |

## API

| Ruta | Que devuelve |
|---|---|
| `GET /api/avisos` | Avisos de orden de la empresa (`X-Company-Id`) |
| `GET /api/avisos/stock` | Alertas de stock de la empresa |
| `GET /api/avisos/despachos` | Tickets de despacho emitidos a los tecnicos |

## Integrantes

| Integrante | GitHub |
|---|---|
| Kevin Rojas | @khrojasdev |
| Christopher Perez | @ChrisPerezV |
| Diego Lopez | @DiegoLopez-f |
