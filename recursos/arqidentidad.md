## 1. Estructura general del microservicio gubernamental de identidad

La raíz del proyecto es: com.segip.dntic.msvc

src/main/java/com/gobierno/identidad

bootstrap └── IdentidadApplication.java

config ├── DatabaseConfig.java ├── SecurityConfig.java ├── SwaggerConfig.java ├── KafkaConfig.java ├── CacheConfig.java ├── AuditConfig.java └── ObservabilityConfig.java

domain ├── model │ ├── Persona.java │ ├── DocumentoIdentidad.java │ └── Biometria.java │ ├── valueobject │ ├── NumeroDocumento.java │ ├── NombreCompleto.java │ └── HuellaDigital.java │ ├── repository │ ├── PersonaRepository.java │ └── DocumentoRepository.java │ ├── service │ └── IdentidadDomainService.java │ ├── event │ ├── PersonaRegistradaEvent.java │ ├── DocumentoEmitidoEvent.java │ └── BiometriaActualizadaEvent.java

│


└── exception

├── PersonaNoEncontradaException.java └── DocumentoDuplicadoException.java

application

├── dto

│ ├── request

│ │ ├── RegistrarPersonaRequest.java

│ │ ├── ActualizarPersonaRequest.java

│ │ └── RegistrarBiometriaRequest.java

│

│

│

└── response

│ ├── PersonaResponse.java

│

└── DocumentoResponse.java

│

├── usecase

│ ├── command

│ │ ├── RegistrarPersonaUseCase.java

│ │ ├── EmitirDocumentoUseCase.java

│ │ └── ActualizarBiometriaUseCase.java

│ │

│ └── query

│ ├── BuscarPersonaUseCase.java

│

└── ListarPersonasUseCase.java

│

├── mapper

│ └── PersonaMapper.java

│

├── validator

│ └── PersonaValidator.java

│

└── service

└── IdentidadApplicationService.java

infrastructure ├── persistence │ ├── entity │ │ ├── PersonaEntity.java │ │ ├── DocumentoEntity.java │ │ └── BiometriaEntity.java


│ │ │ ├── repository │ │ ├── JpaPersonaRepository.java │ │ └── JpaDocumentoRepository.java │ │ │ └── adapter │ ├── PersonaRepositoryImpl.java │ └── DocumentoRepositoryImpl.java │

├── messaging │ ├── producer │ │ └── IdentidadKafkaProducer.java │ │ │ └── consumer │ └── IdentidadKafkaConsumer.java │

├── cache │ └── PersonaRedisCache.java │

├── client │ ├── PadronElectoralClient.java │ ├── MigracionClient.java │ └── PoliciaClient.java │

└── scheduler

└── SincronizacionPadronScheduler.java

interfaces ├── controller │ ├── PersonaController.java │ └── DocumentoController.java │ ├── filter │ └── CorrelationIdFilter.java │ ├── advice │ ├── LoggingAdvice.java │ └── MetricsAdvice.java │ └── handler


└── GlobalExceptionHandler.java

shared

├── audit

│ └── AuditoriaService.java

│

├── security

│ └── JwtUtil.java

│

├── util

│ └── FechaUtil.java

│

└── constants

└── ApiPaths.java

## 2. Explicación de cada capa

## BOOTSTRAP

Punto de arranque del microservicio.

@SpringBootApplication public class IdentidadApplication {

public static void main(String[] args){ SpringApplication.run(IdentidadApplication.class,args);

}

}

## CONFIG

Configuración técnica del sistema.

## Archivo Función

DatabaseConfig

SecurityConfig

conexión PostgreSQL

autenticación JWT


## Archivo Función

KafkaConfig

CacheConfig

AuditConfig

ObservabilityConfig métricas

mensajería

Redis

auditoría

## DOMAIN (núcleo del negocio)

Contiene las reglas del negocio del sistema de identidad.

No depende de frameworks.

## model

Representa entidades del dominio.

Ejemplo:

public class Persona {

private Long id; private NombreCompleto nombre; private NumeroDocumento documento;

}

## valueobject

Objetos de valor.

Ejemplo:

public class NumeroDocumento {

private String valor;

}

Se usa para validar reglas del dominio.


## repository

Contratos de persistencia.

public interface PersonaRepository {

Persona guardar(Persona persona);

}

## service

Servicios de dominio.

```
Ejemplo:
public class IdentidadDomainService {
public void validarDocumentoUnico(String documento){
// regla de negocio
}
}
```

## event

Eventos del dominio.

Ejemplo:

PersonaRegistradaEvent

DocumentoEmitidoEvent

Estos eventos se envían a Kafka para otros sistemas.

## APPLICATION

Orquesta los casos de uso del sistema.

## DTO

Objetos que viajan en la API.

Ejemplo:


RegistrarPersonaRequest PersonaResponse

Separan API del dominio.

## USE CASES

Cada operación es un caso de uso.

Se divide en:

## command

Operaciones que modifican datos.

RegistrarPersona EmitirDocumento ActualizarBiometria

## query

Operaciones de lectura.

BuscarPersona ListarPersonas

Esto es CQRS.

## MAPPER

Convierte:

DTO → DOMAIN DOMAIN → ENTITY

ENTITY → RESPONSE

## VALIDATOR

Validaciones de negocio.

Ejemplo:

edad mínima documento único formato del nombre


## INFRASTRUCTURE

Implementa tecnologías externas.

## PERSISTENCE

Acceso a base de datos.

Contiene:

Entity Repository JPA

Adapter

## MESSAGING

Arquitectura event driven.

Ejemplo:

PersonaRegistradaEvent

se publica en Kafka.

## CACHE

Redis para consultas rápidas.

Ejemplo:

consulta por número de documento

## CLIENT

Clientes para otros sistemas.

Ejemplo:

Padron electoral Migración

Policía


## SCHEDULER

Procesos programados.

Ejemplo:

sincronización de padrón limpieza de datos actualización biométrica

## INTERFACES

Entrada al sistema.

## CONTROLLER

API REST.

Ejemplo:

POST /personas GET /personas/{documento}

## FILTER

Filtros HTTP.

Ejemplo:

CorrelationId

Permite rastrear solicitudes entre microservicios.

## ADVICE

Interceptores.

Ejemplo:

logs métricas

auditoría

## HANDLER


Manejo global de errores.

## SHARED

Código reutilizable.

Contiene:

auditoría utilidades seguridad

constantes

## 3. Flujo de una solicitud real

HTTP Request ↓ Controller ↓ Application Service ↓ UseCase ↓ Domain ↓ Repository Interface ↓ RepositoryImpl ↓ JPA Repository ↓ PostgreSQL

## 4. Comunicación entre microservicios

UseCase ↓ Domain Event ↓ Kafka Producer


Otro microservicio

## 5. Tecnologías usadas en plataformas de identidad

## Tecnología Uso

Spring Boot microservicios

PostgreSQL base de datos

Kafka

Redis

Keycloak

Prometheus métricas

Grafana monitoreo

ELK

Docker

Kubernetes orquestación

eventos

cache

identidad

logs

contenedores

## 6. Escala típica de estas plataformas

Un sistema nacional de identidad puede tener:

## Elemento Cantidad

Microservicios 50–200

Tablas

APIs

Eventos Kafka 300+

500+

2000+

## 7. Características clave


- alta disponibilidad

- auditoría completa

- trazabilidad de cada transacción

- seguridad fuerte

- integración con múltiples sistemas

- soporte para millones de ciudadanos
