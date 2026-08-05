# Arquitectura de Microservicio — Productos (Empresa)

> Arquitectura hexagonal / Clean Architecture con DDD, aplicada a un microservicio de gestión de productos.
> Documento base de referencia. Ver `productos-gobierno.md` e `identidad-gobierno.md` para variantes con capas adicionales (cache, scheduler, mensajería con producer/consumer separados).

## Estructura de paquetes

```
com.empresa.productos
├── config
│   ├── AppConfig.java
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java
│   └── JacksonConfig.java
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
│   └── event
│       └── ProductoCreadoEvent.java
│
├── application
│   ├── dto
│   │   ├── request
│   │   │   └── ProductoRequest.java
│   │   └── response
│   │       └── ProductoResponse.java
│   ├── mapper
│   │   └── ProductoMapper.java
│   ├── usecase
│   │   ├── CrearProductoUseCase.java
│   │   ├── ActualizarProductoUseCase.java
│   │   ├── ObtenerProductoUseCase.java
│   │   ├── ListarProductosUseCase.java
│   │   └── EliminarProductoUseCase.java
│   ├── validator
│   │   └── ProductoValidator.java
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
│   ├── client
│   │   └── SistemaFacturacionClient.java
│   ├── messaging
│   │   └── KafkaProductoProducer.java
│   └── config
│       └── DatabaseConfig.java
│
└── interfaces
    ├── controller
    │   └── ProductoController.java
    ├── handler
    │   └── GlobalExceptionHandler.java
    └── advice
        └── LoggingAdvice.java
```

## Principios de arquitectura

| Categoría | Prácticas aplicadas |
|---|---|
| Diseño | Clean Architecture, DDD, SOLID, separación de responsabilidades |
| Escalabilidad | Diseño orientado a microservicios, Event Driven Architecture, Saga Pattern |
| Seguridad | Seguridad JWT, CORS, validaciones de entrada |
| Datos | Migraciones versionadas con Flyway |
| Observabilidad | Tracing distribuido, logs centralizados, métricas (Prometheus/Grafana), auditoría |
| API | Versionado de APIs, arquitectura hexagonal avanzada |

## Flujo de una solicitud

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

## Comunicación entre microservicios

```
UseCase → Domain Event → Kafka Producer → Otro microservicio
```
