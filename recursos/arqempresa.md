src/main/java/com/empresa/productos

config ├── AppConfig.java ├── OpenApiConfig.java ├── SecurityConfig.java └── JacksonConfig.java

domain ├── model │ └── Producto.java │ ├── repository │ └── ProductoRepository.java │ ├── service │ └── ProductoDomainService.java │ ├── exception │ └── ProductoNotFoundException.java │ └── event └── ProductoCreadoEvent.java

application ├── dto │ ├── request │ │ └── ProductoRequest.java │ │ │ └── response │ └── ProductoResponse.java │ ├── mapper │ └── ProductoMapper.java │ ├── usecase │ ├── CrearProductoUseCase.java │ ├── ActualizarProductoUseCase.java │ ├── ObtenerProductoUseCase.java │ ├── ListarProductosUseCase.java


│ └── EliminarProductoUseCase.java │

├── validator │ └── ProductoValidator.java │

└── service └── ProductoApplicationService.java

infrastructure ├── persistence │ ├── entity │ │ └── ProductoEntity.java │ │ │ ├── repository │ │ └── JpaProductoRepository.java │ │ │ └── adapter │ └── ProductoRepositoryImpl.java │

├── client │ └── SistemaFacturacionClient.java │

├── messaging │ └── KafkaProductoProducer.java │

└── config └── DatabaseConfig.java

interfaces ├── controller │ └── ProductoController.java │

├── handler │ └── GlobalExceptionHandler.java │

└── advice └── LoggingAdvice.java


- Clean Architecture

- DDD (Domain Driven Design)

- SOLID

- Separación de responsabilidades

- Escalabilidad para microservicios

- CORS

- Event Driven Architecture

- Saga Pattern

- Observabilidad

- Tracing distribuido

- Arquitectura hexagonal avanzada.

- Auditoría

- Versionado de APIs

- Seguridad JWT

- Validaciones

- Flyway para migraciones

- Logs centralizados

- Observabilidad (Prometheus/Grafana)
