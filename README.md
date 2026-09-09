# digitalfix-ms-notificaciones

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
- [`digitalfix-bff`](https://github.com/khrojasdev/digitalfix-bff) - BFF / API interna
- [`digitalfix-ms-catalogo`](https://github.com/khrojasdev/digitalfix-ms-catalogo) - Microservicio
- [`digitalfix-ms-ordenes`](https://github.com/khrojasdev/digitalfix-ms-ordenes) - Microservicio
- [`digitalfix-ms-notificaciones`](https://github.com/khrojasdev/digitalfix-ms-notificaciones) - Microservicio **(este)**
- [`digitalfix-ms-reportes`](https://github.com/khrojasdev/digitalfix-ms-reportes) - Microservicio
- [`digitalfix-ms-auditoria`](https://github.com/khrojasdev/digitalfix-ms-auditoria) - Microservicio
- [`digitalfix-infra`](https://github.com/khrojasdev/digitalfix-infra) - Infraestructura

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
