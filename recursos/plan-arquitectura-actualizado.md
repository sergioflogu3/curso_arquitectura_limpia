# Plan de Generación — SEGIP Sistema Completo (Stack actualizado, Ago 2026)

## Corrección de versiones objetivo

| Pedido original | Usar en su lugar | Motivo |
|---|---|---|
| Spring Boot 4 | **Spring Boot 4.1.0** | Rama 3.5 sin soporte OSS desde el 30/06/2026. 4.0 pierde soporte en dic-2026 — arrancar directo en 4.1 evita una segunda migración. Requiere Spring Framework 7.0.8+, Java 17 mínimo. |
| Angular 20 | **Angular 22** | 20 está en LTS solo hasta el 28/11/2026. Para un proyecto que arranca ahora, es quedar obsoleto en <4 meses. |
| Flutter LTS | **Flutter 3.44.x (canal stable)** | Flutter no tiene rama LTS — solo stable/beta/main. Fijar el patch exacto en CI (FVM), no seguir "stable" flotante. |
| Docker Compose | **Compose Specification v5.0 + CLI plugin v2.4x** | Spec vendor-neutral; el archivo pasa a llamarse `compose.yaml` y la clave `version:` ya no es necesaria (se ignora si está presente). |

---

## Backend — 5 microservicios Spring Boot 4.1 + Clean Architecture

Misma composición de microservicios del plan original (seguridad · registro · encuestas · respuestas · reportes). Cambios de migración a tener en cuenta en todos ellos:

- Baseline Jakarta EE 11 (viene de Spring Framework 7)
- Jackson 3 (reemplaza Jackson 2 — revisar serializers/deserializers custom)
- Auto-configuración dividida en JARs separados
- Null-safety con JSpecify — aprovechable desde el día uno en `domain/model`
- Versionado de API de primera clase (ya no hace falta implementarlo a mano)
- gRPC auto-configuration, mitigación SSRF en clientes HTTP, conexiones JDBC lazy

`msvc-seguridad` (OAuth 2.1 / JWT): actualizar junto con **Spring Security 7.1.0**, que se libera en el mismo tren de release que Boot 4.1.

Los demás microservicios (registro, encuestas, respuestas, reportes) no requieren cambios estructurales, solo el bump de versión y revisión de starters de terceros.

## Infraestructura Spring Cloud

`eureka-server` (8761) · `gateway-server` (8090, WebFlux) · `auth-server` (9000, OAuth 2.1) · PostgreSQL x5 (Docker)

Fijar **Spring Cloud 2025.1.2** — es el release train validado contra Spring Boot 4.1.

## Archivos generados por microservicio

| Capa | Contenido | Cambia con la migración |
|---|---|---|
| Domain | Entities, Value Objects, Domain Events, Ports in/out, Domain Exceptions, tests JUnit5, Flyway migrations | No — esta capa no depende de frameworks por diseño |
| Application | Use Cases (Interactors), Commands + Queries, Response DTOs, tests Mockito, `application.yml`, `pom.xml` | No |
| Adapter + Infra | REST Controllers ABM, JPA Entities + Repos, `@WebMvcTest`, `@DataJpaTest` + Testcontainers, Resilience4j, Swagger/OpenAPI | Verificar que Resilience4j y springdoc-openapi ya publicaron versiones compatibles con Boot 4.1 antes de fijar el `pom.xml` — los starters de terceros suelen ir un paso atrás del release train de Spring |

## Frontend — Angular 22 + Playwright E2E

Módulos: seguridad · registro · encuestas · respuestas · reportes/shared, con flujo PKCE OAuth 2.1.

Cambios relevantes de la versión:
- **Zoneless change detection** es el default en proyectos nuevos (sin Zone.js) — decidir explícitamente si el proyecto va zoneless desde el inicio o mantiene Zone.js durante la transición
- **Signal Forms** y **Angular Aria** ya son estables (no experimentales)
- **Componentes selectorless** — se pueden importar directo en templates sin selector

Services + Guards (HTTP Interceptor JWT, Auth Guard, Role Guard) y los specs de Playwright no cambian con el bump de versión.

## Mobile — Flutter 3.44.x (iOS + Android)

Screens, BLoC + Repository, tests — sin cambios estructurales frente al plan original. Como Flutter no tiene LTS, fijar el patch exacto (no "latest stable") en `pubspec.yaml` / CI para no heredar breaking changes en cada bump trimestral.

## Docker Compose + guía paso a paso

`docker-compose.yml` → **`compose.yaml`** (Compose Specification v5.0) · `check-env.sh` · `INSTALL.md` — sin cambios estructurales más allá del rename y la actualización del CLI plugin.

---

## Nota de confidencialidad

Este documento sigue conteniendo el nombre real de la entidad y la composición exacta de sus microservicios. Antes de subirlo a un repositorio público, decide si lo publicas con el nombre real, lo genéricas (`msvc-modulo-a`, `msvc-modulo-b`...), o lo dejas en un repo privado.
