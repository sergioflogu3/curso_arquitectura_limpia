# Arquitectura de Microservicio Gubernamental — Identidad

> ⚠️ El documento original indicaba dos raíces distintas para el paquete (`com.segip.dntic.msvc` y `com.gobierno.identidad`). Se usa `com.gobierno.identidad` en todo este documento por ser la que aparece en el árbol completo. Verifica cuál es la correcta antes de publicar, especialmente si el proyecto referencia un sistema real.

## 1. Estructura general

```
com.gobierno.identidad
├── bootstrap
│   └── IdentidadApplication.java
│
├── config
│   ├── DatabaseConfig.java
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── KafkaConfig.java
│   ├── CacheConfig.java
│   ├── AuditConfig.java
│   └── ObservabilityConfig.java
│
├── domain
│   ├── model
│   │   ├── Persona.java
│   │   ├── DocumentoIdentidad.java
│   │   └── Biometria.java
│   ├── valueobject
│   │   ├── NumeroDocumento.java
│   │   ├── NombreCompleto.java
│   │   └── HuellaDigital.java
│   ├── repository
│   │   ├── PersonaRepository.java
│   │   └── DocumentoRepository.java
│   ├── service
│   │   └── IdentidadDomainService.java
│   ├── event
│   │   ├── PersonaRegistradaEvent.java
│   │   ├── DocumentoEmitidoEvent.java
│   │   └── BiometriaActualizadaEvent.java
│   └── exception
│       ├── PersonaNoEncontradaException.java
│       └── DocumentoDuplicadoException.java
│
├── application
│   ├── dto
│   │   ├── request
│   │   │   ├── RegistrarPersonaRequest.java
│   │   │   ├── ActualizarPersonaRequest.java
│   │   │   └── RegistrarBiometriaRequest.java
│   │   └── response
│   │       ├── PersonaResponse.java
│   │       └── DocumentoResponse.java
│   ├── usecase
│   │   ├── command
│   │   │   ├── RegistrarPersonaUseCase.java
│   │   │   ├── EmitirDocumentoUseCase.java
│   │   │   └── ActualizarBiometriaUseCase.java
│   │   └── query
│   │       ├── BuscarPersonaUseCase.java
│   │       └── ListarPersonasUseCase.java
│   ├── mapper
│   │   └── PersonaMapper.java
│   ├── validator
│   │   └── PersonaValidator.java
│   └── service
│       └── IdentidadApplicationService.java
│
├── infrastructure
│   ├── persistence
│   │   ├── entity
│   │   │   ├── PersonaEntity.java
│   │   │   ├── DocumentoEntity.java
│   │   │   └── BiometriaEntity.java
│   │   ├── repository
│   │   │   ├── JpaPersonaRepository.java
│   │   │   └── JpaDocumentoRepository.java
│   │   └── adapter
│   │       ├── PersonaRepositoryImpl.java
│   │       └── DocumentoRepositoryImpl.java
│   ├── messaging
│   │   ├── producer
│   │   │   └── IdentidadKafkaProducer.java
│   │   └── consumer
│   │       └── IdentidadKafkaConsumer.java
│   ├── cache
│   │   └── PersonaRedisCache.java
│   ├── client
│   │   ├── PadronElectoralClient.java
│   │   ├── MigracionClient.java
│   │   └── PoliciaClient.java
│   └── scheduler
│       └── SincronizacionPadronScheduler.java
│
├── interfaces
│   ├── controller
│   │   ├── PersonaController.java
│   │   └── DocumentoController.java
│   ├── filter
│   │   └── CorrelationIdFilter.java
│   ├── advice
│   │   ├── LoggingAdvice.java
│   │   └── MetricsAdvice.java
│   └── handler
│       └── GlobalExceptionHandler.java
│
└── shared
    ├── audit
    │   └── AuditoriaService.java
    ├── security
    │   └── JwtUtil.java
    ├── util
    │   └── FechaUtil.java
    └── constants
        └── ApiPaths.java
```

## 2. Explicación de cada capa

### bootstrap
Punto de arranque del microservicio.

```java
@SpringBootApplication
public class IdentidadApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentidadApplication.class, args);
    }
}
```

### config
Configuración técnica del sistema.

| Archivo | Función |
|---|---|
| DatabaseConfig | Conexión PostgreSQL |
| SecurityConfig | Autenticación JWT |
| SwaggerConfig | Documentación de API |
| KafkaConfig | Mensajería |
| CacheConfig | Redis |
| AuditConfig | Auditoría |
| ObservabilityConfig | Métricas |

### domain (núcleo del negocio)
No depende de frameworks. Contiene entidades, reglas y contratos.

**model** — entidades del dominio:
```java
public class Persona {
    private Long id;
    private NombreCompleto nombre;
    private NumeroDocumento documento;
}
```

**valueobject** — objetos de valor usados para validar reglas del dominio:
```java
public class NumeroDocumento {
    private String valor;
}
```

**repository** — contratos de persistencia:
```java
public interface PersonaRepository {
    Persona guardar(Persona persona);
}
```

**service** — servicios de dominio:
```java
public class IdentidadDomainService {
    public void validarDocumentoUnico(String documento) {
        // regla de negocio
    }
}
```

**event** — eventos de dominio publicados en Kafka para otros sistemas: `PersonaRegistradaEvent`, `DocumentoEmitidoEvent`, `BiometriaActualizadaEvent`.

### application
Orquesta los casos de uso.

- **dto**: `RegistrarPersonaRequest`, `PersonaResponse`, etc. — separan la API del dominio.
- **usecase**: dividido en `command` (escritura: registrar persona, emitir documento, actualizar biometría) y `query` (lectura: buscar, listar). Esto es CQRS.
- **mapper**: convierte DTO → Domain → Entity → Response.
- **validator**: validaciones de negocio (edad mínima, documento único, formato del nombre).

### infrastructure
Implementa tecnologías externas.

| Subcapa | Contenido |
|---|---|
| persistence | Entity, Repository JPA, Adapter |
| messaging | Producer/Consumer de Kafka, arquitectura event-driven |
| cache | Redis — ej. consulta por número de documento |
| client | Clientes a Padrón Electoral, Migración, Policía |
| scheduler | Sincronización de padrón, limpieza de datos, actualización biométrica |

### interfaces
Entrada al sistema.

- **controller**: API REST (`POST /personas`, `GET /personas/{documento}`).
- **filter**: `CorrelationIdFilter` — rastrea solicitudes entre microservicios.
- **advice**: interceptores de logs, métricas y auditoría.
- **handler**: manejo global de errores.

### shared
Código reutilizable: auditoría, seguridad (JWT), utilidades, constantes.

## 3. Flujo de una solicitud real

```
HTTP Request
   → Controller
   → Application Service
   → UseCase
   → Domain
   → Repository Interface
   → RepositoryImpl
   → JPA Repository
   → PostgreSQL
```

## 4. Comunicación entre microservicios

```
UseCase → Domain Event → Kafka Producer → Otro microservicio
```

## 5. Tecnologías usadas

| Tecnología | Uso |
|---|---|
| Spring Boot | Microservicios |
| PostgreSQL | Base de datos |
| Kafka | Eventos / mensajería |
| Redis | Cache |
| Keycloak | Identidad |
| Prometheus | Métricas |
| Grafana | Monitoreo |
| ELK | Logs |
| Docker | Contenedores |
| Kubernetes | Orquestación |

## 6. Escala típica de estas plataformas

> [PROBABLE] Estos valores se reconstruyeron por posición a partir de una tabla con columnas desalineadas en el original. Verifícalos contra la fuente si la tienes.

| Elemento | Cantidad |
|---|---|
| Microservicios | 50–200 |
| Tablas | 300+ |
| APIs | 500+ |
| Eventos Kafka | 2000+ |

## 7. Características clave

- Alta disponibilidad
- Auditoría completa
- Trazabilidad de cada transacción
- Seguridad fuerte
- Integración con múltiples sistemas
- Soporte para millones de ciudadanos
