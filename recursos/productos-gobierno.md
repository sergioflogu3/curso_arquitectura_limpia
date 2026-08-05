# Arquitectura de Microservicio Gubernamental — Productos

## 1. Estructura completa

```
com.gobierno.productos
├── bootstrap
│   └── ProductosApplication.java
│
├── config
│   ├── AppConfig.java
│   ├── DatabaseConfig.java
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── KafkaConfig.java
│   ├── CacheConfig.java
│   └── ObservabilityConfig.java
│
├── domain
│   ├── model
│   │   └── Producto.java
│   ├── repository
│   │   └── ProductoRepository.java
│   ├── service
│   │   └── ProductoDomainService.java
│   ├── exception
│   │   └── ProductoNotFoundException.java
│   ├── event
│   │   ├── ProductoCreadoEvent.java
│   │   └── ProductoActualizadoEvent.java
│   └── valueobject
│       └── Precio.java
│
├── application
│   ├── dto
│   │   ├── request
│   │   │   ├── CrearProductoRequest.java
│   │   │   └── ActualizarProductoRequest.java
│   │   └── response
│   │       ├── ProductoResponse.java
│   │       └── ProductoDetalleResponse.java
│   ├── mapper
│   │   └── ProductoMapper.java
│   ├── validator
│   │   └── ProductoValidator.java
│   ├── usecase
│   │   ├── command
│   │   │   ├── CrearProductoUseCase.java
│   │   │   ├── ActualizarProductoUseCase.java
│   │   │   └── EliminarProductoUseCase.java
│   │   └── query
│   │       ├── ObtenerProductoUseCase.java
│   │       └── ListarProductosUseCase.java
│   └── service
│       └── ProductoApplicationService.java
│
├── infrastructure
│   ├── persistence
│   │   ├── entity
│   │   │   └── ProductoEntity.java
│   │   ├── repository
│   │   │   └── JpaProductoRepository.java
│   │   └── adapter
│   │       └── ProductoRepositoryImpl.java
│   ├── messaging
│   │   ├── producer
│   │   │   └── ProductoKafkaProducer.java
│   │   └── consumer
│   │       └── ProductoKafkaConsumer.java
│   ├── cache
│   │   └── RedisProductoCache.java
│   ├── client
│   │   ├── FacturacionClient.java
│   │   └── IdentidadClient.java
│   └── scheduler
│       └── ProductoScheduler.java
│
├── interfaces
│   ├── controller
│   │   └── ProductoController.java
│   ├── handler
│   │   └── GlobalExceptionHandler.java
│   ├── advice
│   │   ├── LoggingAdvice.java
│   │   └── MetricsAdvice.java
│   └── filter
│       └── CorrelationIdFilter.java
│
└── shared
    ├── util
    │   └── DateUtil.java
    ├── constants
    │   └── ApiPaths.java
    └── audit
        └── AuditoriaService.java
```

## 2. Explicación de cada capa

### bootstrap
```java
@SpringBootApplication
public class ProductosApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductosApplication.class, args);
    }
}
```

### config

| Archivo | Función |
|---|---|
| AppConfig | Beans globales |
| DatabaseConfig | Configuración de PostgreSQL |
| SecurityConfig | JWT / OAuth2 |
| SwaggerConfig | Documentación de API |
| KafkaConfig | Configuración de Kafka |
| CacheConfig | Redis |
| ObservabilityConfig | Prometheus y métricas |

### domain (núcleo del negocio)
No depende de Spring ni de infraestructura. Contiene entidades, reglas y contratos.

```java
public class Producto {
    private Long id;
    private String nombre;
    private Precio precio;
}
```

```java
public class Precio {
    private Double valor;
}
```

```java
public interface ProductoRepository {
    Producto guardar(Producto producto);
}
```

Eventos publicados en Kafka o RabbitMQ: `ProductoCreadoEvent`, `ProductoActualizadoEvent`.

### application
- **dto**: separación entre API y dominio (`CrearProductoRequest`, `ProductoResponse`).
- **usecase**: dividido en `command` (escritura) y `query` (lectura) — CQRS.
- **validator**: nombre obligatorio, precio positivo.
- **mapper**: DTO → Domain → Entity → Response.

### infrastructure

| Subcapa | Contenido |
|---|---|
| persistence | Entity, Repository JPA, Adapter |
| messaging | KafkaProducer / KafkaConsumer para comunicación entre microservicios |
| cache | Redis — reduce consultas a base de datos |
| client | `FacturacionClient`, `IdentidadClient` (WebClient o Feign) |
| scheduler | Sincronización nocturna, limpieza de datos, reprocesos |

### interfaces
- **controller**: `POST /productos`, `GET /productos`.
- **handler**: manejo global de errores.
- **advice**: logging, métricas, auditoría.
- **filter**: `CorrelationIdFilter` — rastrea requests entre microservicios.

### shared
Utilidades, constantes y auditoría reutilizables: `DateUtil`, `ApiPaths`, `AuditoriaService`.

## 3. Flujo real en producción

```
HTTP Request
   → Controller
   → ApplicationService
   → UseCase
   → Domain
   → Repository Interface
   → RepositoryImpl
   → JPA Repository
   → PostgreSQL
```

```
UseCase → Domain Event → Kafka Producer → Otro microservicio
```

## 4. Tecnologías usadas en gobierno

| Tecnología | Uso |
|---|---|
| Spring Boot | Microservicios |
| PostgreSQL | Base de datos |
| Kafka | Mensajería |
| Redis | Cache |
| Docker | Contenedores |
| Kubernetes | Orquestación |
| Prometheus | Métricas |
| Grafana | Monitoreo |
| ELK | Logs |
| Keycloak | Identidad |

## 5. Tamaño típico

> [PROBABLE] Reconstruido a partir de una tabla con columnas desalineadas en el original. Verifícalo contra la fuente si la tienes.

| Tipo | Cantidad |
|---|---|
| Controllers | 5–10 |
| UseCases | 20–40 |
| DTOs | 40–80 |
| Entities | 10–20 |
| Repositories | 10 |
| Mappers | 10 |
| Services | 10 |
| **Total aproximado** | **150–300 archivos por microservicio** |

## 6. Características clave

- Escalable
- Alta disponibilidad
- Auditable
- Fácil mantenimiento
- Compatible con microservicios
- Independencia de infraestructura
