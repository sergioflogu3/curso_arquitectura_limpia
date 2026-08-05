## 1. Estructura completa del microservicio

src/main/java/com/gobierno/productos

bootstrap └── ProductosApplication.java

config ├── AppConfig.java ├── DatabaseConfig.java ├── SecurityConfig.java ├── SwaggerConfig.java ├── KafkaConfig.java ├── CacheConfig.java └── ObservabilityConfig.java

domain ├── model │ └── Producto.java │ ├── repository │ └── ProductoRepository.java │ ├── service │ └── ProductoDomainService.java │ ├── exception │ └── ProductoNotFoundException.java │ ├── event │ ├── ProductoCreadoEvent.java │ └── ProductoActualizadoEvent.java │ └── valueobject └── Precio.java

application ├── dto │ ├── request │ │ ├── CrearProductoRequest.java


│ │ └── ActualizarProductoRequest.java │ │ │ └── response │ ├── ProductoResponse.java │ └── ProductoDetalleResponse.java │ ├── mapper │ └── ProductoMapper.java │ ├── validator │ └── ProductoValidator.java │ ├── usecase │ ├── command │ │ ├── CrearProductoUseCase.java │ │ ├── ActualizarProductoUseCase.java │ │ └── EliminarProductoUseCase.java │ │ │ └── query │ ├── ObtenerProductoUseCase.java │ └── ListarProductosUseCase.java │ └── service

└── ProductoApplicationService.java

infrastructure ├── persistence │ ├── entity │ │ └── ProductoEntity.java │ │ │ ├── repository │ │ └── JpaProductoRepository.java │ │ │ └── adapter │ └── ProductoRepositoryImpl.java │ ├── messaging │ ├── producer │ │ └── ProductoKafkaProducer.java │ │


│ └── consumer │ └── ProductoKafkaConsumer.java │

├── cache │ └── RedisProductoCache.java │

├── client │ ├── FacturacionClient.java │ └── IdentidadClient.java │

└── scheduler

└── ProductoScheduler.java

interfaces ├── controller │ └── ProductoController.java │ ├── handler │ └── GlobalExceptionHandler.java │ ├── advice │ ├── LoggingAdvice.java │ └── MetricsAdvice.java │ └── filter

└── CorrelationIdFilter.java

shared ├── util │ └── DateUtil.java │ ├── constants │ └── ApiPaths.java │ └── audit

└── AuditoriaService.java

- 2. Explicación de cada capa


## BOOTSTRAP

Contiene el punto de inicio del microservicio.

```
@SpringBootApplication public class ProductosApplication {
public static void main(String[] args){
SpringApplication.run(ProductosApplication.class,args);
}
}
```

## CONFIG

Configuración técnica del sistema.

Ejemplos:

Archivo

AppConfig

DatabaseConfig Configuración de PostgreSQL

SecurityConfig

SwaggerConfig

KafkaConfig

CacheConfig

ObservabilityConfig Prometheus y métricas

Función

Beans globales

JWT / OAuth2

Documentación API

Configuración Kafka

Redis

## DOMAIN (núcleo del negocio)

Esta capa no depende de Spring ni de infraestructura.

## Contiene:

- entidades del negocio

- reglas

- contratos


## model

```
Representa el modelo de negocio.
public class Producto {
private Long id; private String nombre; private Precio precio;
}
```

## valueobject

Objetos de valor.

public class Precio {

private Double valor;

}

Esto permite validar reglas del negocio.

## repository

Contrato de acceso a datos.

public interface ProductoRepository {

Producto guardar(Producto producto);

}

event

Eventos del dominio.

Ejemplo:

ProductoCreadoEvent ProductoActualizadoEvent


Se publican en Kafka o RabbitMQ.

## APPLICATION

Contiene casos de uso del sistema.

## DTO

Separación entre API y dominio.

request response

Ejemplo:

CrearProductoRequest ProductoResponse

## USE CASES

Cada acción es un caso de uso independiente.

Se separa en:

command → escritura query → lectura

Esto es CQRS.

Ejemplo:

CrearProductoUseCase ActualizarProductoUseCase ListarProductosUseCase

## VALIDATOR

Validaciones de negocio.

Ejemplo:

nombre obligatorio

precio positivo


## MAPPER

Convierte:

DTO → Domain Domain → Entity

Entity → Response

## INFRASTRUCTURE

Implementa tecnologías externas.

## PERSISTENCE

Base de datos.

Contiene:

entity repository adapter

Ejemplo:

ProductoEntity JpaProductoRepository ProductoRepositoryImpl

## MESSAGING

Arquitectura event driven.

Ejemplo:

KafkaProducer KafkaConsumer

Se usa para comunicación entre microservicios.

## CACHE

Redis.

Ejemplo:


RedisProductoCache

Reduce consultas a base de datos.

## CLIENT

Clientes REST para otros sistemas.

Ejemplo:

FacturacionClient IdentidadClient

Se usa WebClient o Feign.

## SCHEDULER

Tareas programadas.

Ejemplo:

sincronización nocturna limpieza de datos reprocesos

## INTERFACES

Entrada al sistema.

## CONTROLLER

API REST.

Ejemplo:

POST /productos GET /productos

## HANDLER

Manejo global de errores.


## ADVICE

Interceptores.

Ejemplos:

logging metrics

auditoría

## FILTER

Filtros HTTP.

Ejemplo:

CorrelationIdFilter

Permite rastrear requests entre microservicios.

## SHARED

Código reutilizable.

Contiene:

utils constants audit

Ejemplo:

DateUtil ApiPaths AuditoriaService

## 3. Flujo real en producción

HTTP Request ↓ Controller ↓ ApplicationService ↓

UseCase


↓ Domain ↓ Repository Interface ↓ RepositoryImpl ↓ JPA Repository ↓ PostgreSQL

Eventos:

UseCase ↓ Domain Event ↓ Kafka Producer ↓

Otro microservicio

- 4. Tecnologías usadas en gobierno

## Tecnología Uso

Spring Boot microservicios

PostgreSQL base de datos

Kafka

Redis

Docker contenedores

Kubernetes orquestación

Prometheus métricas

Grafana monitoreo

ELK

Keycloak identidad

mensajería

cache

logs


## 5. Tamaño típico de microservicios

Un microservicio gubernamental puede tener:

## Tipo Cantidad

Controllers 5–10

UseCases 20–40

DTOs

Entities

Repositories 10

Mappers 10

Services 10

Total aproximado:

40–80

10–20

## 150 – 300 archivos por microservicio

## 6. Características clave de esta arquitectura

Escalable Alta disponibilidad Auditable Fácil mantenimiento Compatible con microservicios

Independencia de infraestructura
