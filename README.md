# ms-digitalfix-notify

**Proyecto:** DigitalFix - Gestion de ordenes de trabajo para una red de 20 empresas de mantenimiento electrico  
**Componente:** Microservicio  
**Asignatura:** DSY1107 Desarrollo Cloud Native I - Duoc UC  
**Primera entrega (EP1):** 14 de septiembre de 2026

## Descripcion

Notificaciones e informes: avisos de cambio de estado, ticket de despacho e informe tecnico.

## Tecnologias

Spring Boot 3, RabbitMQ (3 colas + 3 DLQ), Thymeleaf, OpenPDF

## Integrantes

| Integrante | GitHub |
|---|---|
| Kevin Rojas | @khrojasdev |
| Christopher Perez | @ChrisPerezV |
| Diego Lopez | @DiegoLopez-f |

## Repositorios del proyecto

- [`digitalfix-frontend`](https://github.com/khrojasdev/digitalfix-frontend) - Frontend
- [`ms-digitalfix-bff`](https://github.com/khrojasdev/ms-digitalfix-bff) - BFF / API interna
- [`ms-digitalfix-usuarios`](https://github.com/khrojasdev/ms-digitalfix-usuarios) - Microservicio
- [`ms-digitalfix-catalog`](https://github.com/khrojasdev/ms-digitalfix-catalog) - Microservicio
- [`ms-digitalfix-workorders`](https://github.com/khrojasdev/ms-digitalfix-workorders) - Microservicio
- [`ms-digitalfix-notify`](https://github.com/khrojasdev/ms-digitalfix-notify) - Microservicio **(este)**
- [`ms-digitalfix-report`](https://github.com/khrojasdev/ms-digitalfix-report) - Microservicio
- [`ms-digitalfix-audit`](https://github.com/khrojasdev/ms-digitalfix-audit) - Microservicio
- [`digitalfix-infra`](https://github.com/khrojasdev/digitalfix-infra) - Infraestructura

## Pendiente por definir

El documento del caso pide ademas *un microservicio administrador de RabbitMQ y otro de
Kafka, segun la pauta de cada evaluacion*. Ni la EP1 ni la EP2 los evaluan, asi que no
existen todavia como repositorios. Cuando la pauta de la evaluacion final los exija se
agregaran como `ms-digitalfix-rabbit` y `ms-digitalfix-kafka`. La decision del equipo fue esperar la pauta.

## Tablero

El backlog completo vive en un unico GitHub Project que enlaza los ocho repositorios.
Columnas: Backlog, To Do, In Progress, In Review, Done. Limite de trabajo en curso: 2 tarjetas por persona.

## Como se trabaja aqui

1. Cada tarea del tablero tiene su propia rama, indicada en el cuerpo del issue.
2. `git switch <rama>` - nunca se trabaja directamente sobre `main`.
3. Commits con Conventional Commits: `feat(catalogo): agrega endpoint de servicios`.
4. Pull request hacia `main` con `Closes #<numero del issue>` en la descripcion.
5. Revisa un companero distinto del autor. Recien ahi se hace merge.

Referencias entre repositorios: `khrojasdev/otro-repo#12`.

## Configuracion

Ninguna credencial vive en este repositorio. Todo llega por variables de entorno;
revisa `.env.example` para saber cuales.
