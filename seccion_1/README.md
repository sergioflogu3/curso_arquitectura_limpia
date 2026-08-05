# Presentación 1 — "Los cimientos: Por qué la arquitectura importa"
**Curso:** Patrones de arquitectura con Spring Boot 4 y Java 21
**Proyecto guía:** atlas-bank (basado en la colección Postman provista)
**Formato:** módulo de 13 clases (~45 min promedio c/u, total ≈ 7.5-8 horas repartidas en varias sesiones)
**Audiencia:** compañeros de trabajo — nivel asumido: conocen Java, no necesariamente Spring ni arquitectura de software

---

## ⚠️ Notas de alcance (léelas antes de armar los slides)

1. **La colección Postman describe el estado final del proyecto, no el de esta presentación.** Incluye Auth con Keycloak, Transactions y un AI Agent. Esta Sección 1 **solo** cubre: CRUD de `Account`, SOLID, inyección de dependencias, y organización de paquetes. Dilo explícitamente en la Clase 1 para fijar expectativas.
2. **`GET /accounts/{id}/dashboard` queda fuera del checkpoint** salvo que lo implementes como stub con datos fijos — depende de `Transactions`, que no existe todavía en este alcance.
3. **Modelo de datos real** (extraído de los request bodies de Postman), es la base de todos los ejemplos de código de este plan:
   ```
   Account {
     accountNumber: String   // "10001"
     ownerName: String       // "Juan Pérez"
     email: String           // "juan@example.com"
     type: AccountType       // SAVINGS | CHECKING
     balance: BigDecimal     // 50000.00
     status: AccountStatus   // ACTIVE | CLOSED (inferido del endpoint /close)
   }
   ```
4. **Stack confirmado:** Spring Boot 4.1.x requiere Java 17 como mínimo, pero recomienda fuertemente Java 21+ para virtual threads — tu elección de Java 21 es la correcta y vale la pena mencionarlo como gancho en la Clase 1 (no es una elección arbitraria, es la recomendación oficial actual).

---

## Estructura general (13 slides de sección + subslides por clase)

| # | Clase | Duración | Bloque temático |
|---|-------|----------|------------------|
| 1 | Los objetivos: aprendizaje y proyecto | 15 min | Kickoff |
| 2 | El costo del código sin arquitectura | 30 min | Motivación |
| 3 | Setup del proyecto base (parte 1 y 2) | 60 min | Setup |
| 4 | Conectando las capas del proyecto (parte 1 y 2) | 60 min | CRUD base |
| 5 | Principios SOLID (overview) | 20 min | SOLID |
| 6 | Single Responsibility | 30 min | SOLID |
| 7 | Open/Closed | 30 min | SOLID |
| 8 | Liskov Substitution | 30 min | SOLID |
| 9 | Interface Segregation | 25 min | SOLID |
| 10 | Dependency Inversion | 35 min | SOLID |
| 11 | Inyección de dependencias en Spring | 35 min | DI |
| 12 | Package-by-layer vs package-by-feature | 40 min | Organización |
| 13 | Checkpoint del proyecto | 60 min | Cierre |

---

## Clase 1 — Los objetivos: aprendizaje y proyecto
**Duración:** 15 min
**Objetivo:** que el equipo sepa qué va a poder hacer al terminar la Sección 1 y qué NO se va a cubrir todavía.

**Contenido:**
- Qué es `atlas-bank` y por qué es el hilo conductor de todo el curso (mostrar el diagrama de arquitectura hexagonal final como "spoiler" del destino, sin implementarlo aún).
- Alcance explícito de esta presentación: CRUD de cuentas, SOLID, DI, organización de paquetes.
- Alcance explícito de lo que **no** se toca todavía: transferencias, seguridad con Keycloak, agente de IA (eso son presentaciones futuras).
- Cómo se evalúa el checkpoint de la Clase 13.

**Demo/código:** ninguno — es slide de contexto.

**Actividad:** pregunta abierta al equipo: "¿qué proyecto suyo actual no sobreviviría un cambio de requerimientos sin reescritura total?" (dispara la Clase 2).

**Slides sugeridos:**
1. Portada: Patrones de arquitectura con Spring Boot 4 + Java 21
2. Qué es atlas-bank (mapa mental del proyecto completo, con las 3 fases marcadas: Fundamentos → Hexagonal → Seguridad/IA)
3. Qué cubrimos en esta presentación (checklist)
4. Qué NO cubrimos todavía (checklist, para evitar falsas expectativas)
5. Cómo se evalúa el checkpoint

---

## Clase 2 — El costo del código sin arquitectura
**Duración:** 30 min
**Objetivo:** que el equipo sienta el dolor del código sin criterio arquitectónico antes de darles la solución.

**Contenido:**
- Síntomas típicos: clases de 800 líneas, lógica de negocio en el controller, imposibilidad de testear sin levantar toda la app, miedo a tocar código porque "algo se rompe en otro lado".
- Costo real: tiempo de onboarding, tiempo de estimación que se dispara, bugs en producción por acoplamiento oculto.
- Contraste: mostrar una versión "mala" de `AccountController` que hace validación, persistencia (con `EntityManager` directo), envío de email y logging, todo en el mismo método `crearCuenta()`.

**Demo/código:**
```java
// Versión ANTI-PATRÓN — todo en el controller
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    @PersistenceContext
    private EntityManager em;

    @PostMapping
    public ResponseEntity<?> crearCuenta(@RequestBody Map<String, Object> body) {
        if (body.get("email") == null || !body.get("email").toString().contains("@")) {
            return ResponseEntity.badRequest().body("email inválido");
        }
        // persistencia manual mezclada con validación y lógica de negocio
        Account a = new Account();
        a.setAccountNumber((String) body.get("accountNumber"));
        a.setOwnerName((String) body.get("ownerName"));
        a.setEmail((String) body.get("email"));
        a.setBalance(new BigDecimal(body.get("balance").toString()));
        em.persist(a);
        // envío de email hardcodeado aquí mismo...
        // logging manual aquí mismo...
        return ResponseEntity.ok(a);
    }
}
```
Este es el "villano" que la Sección 1 entera va a ir desmontando pieza por pieza.

**Actividad:** el equipo identifica en voz alta 3 responsabilidades mezcladas en ese método.

**Slides sugeridos:**
1. Los síntomas del código sin arquitectura
2. El costo real (tiempo, bugs, miedo a tocar código)
3. Code smell en vivo: AccountController "todo en uno"
4. ¿Cuántas responsabilidades encontraron?

---

## Clase 3 — Setup del proyecto base (parte 1 y 2)
**Duración:** 60 min (30 + 30)

### Parte 1 — Spring Initializr
**Objetivo:** dejar el esqueleto de `atlas-bank` corriendo.
**Contenido:**
- start.spring.io: Spring Boot 4.1.x, Java 21, Maven/Gradle (definir cuál usa el equipo).
- Dependencias mínimas: Spring Web, Spring Data JPA, Validation, H2 (dev) / PostgreSQL (driver).
- Estructura de carpetas inicial generada por Initializr.
- `application.yml` básico (puerto, datasource H2 en memoria).

**Demo/código:**
```yaml
spring:
  application:
    name: atlas-bank
  datasource:
    url: jdbc:h2:mem:atlas-bank
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
server:
  port: 8080
```

### Parte 2 — Entidad Account
**Objetivo:** modelar la entidad base a partir del contrato ya definido en Postman.
**Contenido:**
- Mapear `Account` como entidad JPA usando exactamente los campos que ya usan los compañeros en Postman (`accountNumber`, `ownerName`, `email`, `type`, `balance`).
- Enum `AccountType { SAVINGS, CHECKING }`.
- Por qué arrancar del contrato de API (lo que YA consumen) reduce fricción con el equipo que prueba en Postman.

**Demo/código:**
```java
package com.atlas.bank.atlas_bank.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accountNumber;
    private String ownerName;
    private String email;
    private String type;  // SAVING, CHECKING,
    private BigDecimal balance;
    private String status; // ACTIVE, CLOSED, FROZEN
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (status == null) this.status = "ACTIVE";
        if (balance == null) this.balance = BigDecimal.ZERO;
    }
}
```



**Actividad:** cada compañero levanta el proyecto localmente y confirma que `GET /actuator/health` (o un endpoint dummy) responde antes de seguir.

**Slides sugeridos:**
1. Spring Initializr en vivo (screenshot o demo real)
2. Dependencias elegidas y por qué
3. application.yml explicado línea por línea
4. La entidad Account, campo por campo, contra el JSON real de Postman
5. Checkpoint intermedio: "¿a todos les levantó el proyecto?"

---

## Clase 4 — Conectando las capas del proyecto (parte 1 y 2)
**Duración:** 60 min (30 + 30)

### Parte 1 — Repository y Service
**Objetivo:** implementar el flujo completo Controller → Service → Repository para `POST /accounts` y `GET /accounts`.
**Contenido:**
- `AccountRepository extends JpaRepository<Account, Long>`.
- `AccountService` con lógica mínima (todavía sin SOLID perfecto — a propósito, para refactorizarlo en las clases 6-10).
- Por qué el Controller nunca debe hablar directo con el Repository.

**Demo/código:**
```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
}

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public Account crear(Account account) {
        return repository.save(account);
    }

    public List<Account> listarTodas() {
        return repository.findAll();
    }
}
```

### Parte 2 — Controller y contrato REST
**Objetivo:** exponer los endpoints que YA existen en la colección Postman, para que el equipo pruebe en vivo contra sus propios requests guardados.
**Contenido:**
- `POST /api/v1/accounts`, `GET /api/v1/accounts`, `GET /api/v1/accounts/{id}` — implementados uno a uno contra la colección real.
- DTOs de entrada/salida vs exponer la entidad JPA directamente (mencionar el riesgo, profundizar en presentaciones futuras).

**Demo/código:**
```java
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Account> crear(@RequestBody Account account) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(account));
    }

    @GetMapping
    public List<Account> listar() {
        return service.listarTodas();
    }
}
```

**Actividad:** el equipo corre "Create Account" y "List All Accounts" desde su propia colección Postman contra el proyecto recién levantado — cierre del loop input→output.

**Slides sugeridos:**
1. El flujo Controller → Service → Repository (diagrama)
2. AccountRepository en vivo
3. AccountService en vivo (versión inicial, "buena pero mejorable")
4. AccountController en vivo
5. Demo cruzada: Postman real del equipo contra el código recién escrito

---

## Clase 5 — Principios SOLID (overview)
**Duración:** 20 min
**Objetivo:** dar el mapa completo antes de entrar principio por principio.

**Contenido:**
- Qué significa cada letra, en una frase, sin ejemplos todavía.
- Por qué SOLID no es una lista de reglas sino una forma de razonar sobre el cambio.
- Anticipar: cada uno de los 5 principios siguientes se va a mostrar violado y luego corregido sobre `AccountService`.

**Demo/código:** ninguno — es slide de mapa conceptual.

**Actividad:** ninguna, es puente hacia la Clase 6.

**Slides sugeridos:**
1. SOLID: las 5 letras, una frase cada una
2. El hilo conductor: todas las violaciones van a vivir en AccountService
3. Roadmap de las próximas 5 clases

---

## Clase 6 — Single Responsibility: el servicio que hace todo
**Duración:** 30 min
**Objetivo:** identificar y separar responsabilidades mezcladas en `AccountService`.

**Contenido:**
- Ampliar `AccountService` con validación + notificación + auditoría, todo mezclado (el "servicio que hace todo" del título de la clase).
- Refactor: extraer `AccountValidator`, `NotificationService`, `AuditLogger`.
- Regla práctica: "si para describir qué hace la clase necesitas la palabra 'y', probablemente viola SRP".

**Demo/código:**
```java
// ANTES — viola SRP
@Service
public class AccountService {
    public Account crear(Account account) {
        if (account.getEmail() == null) throw new IllegalArgumentException("email requerido");
        Account saved = repository.save(account);
        emailSender.enviar(saved.getEmail(), "Cuenta creada");
        auditLog.registrar("CREATE_ACCOUNT", saved.getId());
        return saved;
    }
}

// DESPUÉS — responsabilidades separadas
@Service
public class AccountService {
    private final AccountRepository repository;
    private final AccountValidator validator;
    private final NotificationService notifier;
    private final AuditLogger audit;

    public Account crear(Account account) {
        validator.validar(account);
        Account saved = repository.save(account);
        notifier.notificarCreacion(saved);
        audit.registrar("CREATE_ACCOUNT", saved.getId());
        return saved;
    }
}
```

**Actividad:** el equipo, en parejas, encuentra otra responsabilidad oculta en el código de la Clase 4 y la extrae.

**Slides sugeridos:**
1. SRP en una frase
2. AccountService "que hace todo" (código)
3. Las 3 responsabilidades escondidas
4. AccountService refactorizado
5. Regla práctica para detectar violaciones de SRP

---

## Clase 7 — Open/Closed: extender sin romper
**Duración:** 30 min
**Objetivo:** agregar comportamiento nuevo sin modificar código existente.

**Contenido:**
- Escenario: calcular una tasa de mantenimiento distinta para `SAVINGS` y `CHECKING`, resuelto primero con `if/else` sobre el enum (viola OCP: cada cuenta nueva obliga a tocar el método).
- Refactor con Strategy: interfaz `AccountFeePolicy`, implementaciones `SavingsFeePolicy` y `CheckingFeePolicy`, resueltas por Spring vía `Map<AccountType, AccountFeePolicy>` o un bean por tipo.

**Demo/código:**
```java
// ANTES — viola OCP
public BigDecimal calcularTasa(Account account) {
    if (account.getType() == AccountType.SAVINGS) return BigDecimal.valueOf(0.01);
    if (account.getType() == AccountType.CHECKING) return BigDecimal.valueOf(0.02);
    throw new IllegalStateException("tipo no soportado");
}

// DESPUÉS — abierto a extensión, cerrado a modificación
public interface AccountFeePolicy {
    AccountType tipoSoportado();
    BigDecimal calcularTasa(Account account);
}

@Component
public class SavingsFeePolicy implements AccountFeePolicy {
    public AccountType tipoSoportado() { return AccountType.SAVINGS; }
    public BigDecimal calcularTasa(Account account) { return BigDecimal.valueOf(0.01); }
}
// Agregar un tercer tipo de cuenta = una clase nueva, cero líneas tocadas en las existentes
```

**Actividad:** el equipo diseña (sin codear) cómo agregarían un tercer tipo de cuenta, `BUSINESS`, sin tocar las clases existentes.

**Slides sugeridos:**
1. OCP en una frase
2. El if/else que crece para siempre
3. Strategy: la interfaz AccountFeePolicy
4. Agregar un tipo nuevo = una clase nueva
5. Ejercicio: diseñar BUSINESS

---

## Clase 8 — Liskov: herencia que no miente
**Duración:** 30 min
**Objetivo:** detectar cuándo una jerarquía de herencia rompe el contrato del padre.

**Contenido:**
- Escenario tentador: `SavingsAccount extends Account` con un método `retirar()` que lanza excepción si el retiro deja el balance bajo un mínimo — y `CheckingAccount extends Account` que sobreescribe `retirar()` permitiendo saldo negativo hasta un límite. Mostrar cómo un código que trata a todos como `Account` se rompe según el subtipo real.
- Regla práctica: si necesitas un `instanceof` para saber cómo tratar a un objeto, probablemente ya violaste LSP.
- Solución en este dominio: preferir composición (`AccountFeePolicy`, ya visto en Clase 7) sobre herencia para variar comportamiento por tipo de cuenta.

**Demo/código:**
```java
// VIOLACIÓN — el subtipo cambia el contrato del padre
class Account {
    public void retirar(BigDecimal monto) {
        if (monto.compareTo(balance) > 0) throw new SaldoInsuficienteException();
        balance = balance.subtract(monto);
    }
}

class CheckingAccount extends Account {
    @Override
    public void retirar(BigDecimal monto) {
        // "silenciosamente" permite sobregiro — un cliente que espera el contrato del padre se rompe
        balance = balance.subtract(monto);
    }
}
```

**Actividad:** el equipo revisa el código de la Clase 4 y confirma que, al NO haber usado herencia para `AccountType`, ya evitaron esta trampa sin saberlo — conectar con la decisión de la Clase 7.

**Slides sugeridos:**
1. LSP en una frase
2. La jerarquía tentadora: SavingsAccount / CheckingAccount
3. Dónde se rompe el contrato
4. Por qué composición > herencia en este caso
5. Conexión con la Strategy de la clase anterior

---

## Clase 9 — Interface Segregation: interfaces que no estorban
**Duración:** 25 min
**Objetivo:** dividir una interfaz gorda en contratos específicos por consumidor.

**Contenido:**
- Escenario: una interfaz `AccountOperations` con `crear`, `listar`, `buscarPorId`, `cerrar`, `generarDashboard`, `exportarReporte` — el Controller solo necesita 4 de esos 6 métodos, pero se ve forzado a depender de todos.
- Refactor: `AccountReader`, `AccountWriter`, separadas por quién las consume.

**Demo/código:**
```java
// ANTES — interfaz gorda
public interface AccountOperations {
    Account crear(Account account);
    List<Account> listar();
    Account buscarPorId(Long id);
    Account cerrar(Long id);
    DashboardDTO generarDashboard(Long id);
    byte[] exportarReporte(Long id);
}

// DESPUÉS — segregada por responsabilidad de quien consume
public interface AccountReader {
    List<Account> listar();
    Account buscarPorId(Long id);
}

public interface AccountWriter {
    Account crear(Account account);
    Account cerrar(Long id);
}
```

**Actividad:** el equipo identifica qué interfaz consumiría el futuro `AI Agent` (de la colección Postman) — spoiler consciente de que probablemente solo necesite `AccountReader`.

**Slides sugeridos:**
1. ISP en una frase
2. La interfaz AccountOperations, gorda
3. Quién usa qué método realmente
4. AccountReader / AccountWriter
5. Pregunta: ¿qué interfaz necesitaría el AI Agent?

---

## Clase 10 — Dependency Inversion: depender de abstracciones
**Duración:** 35 min
**Objetivo:** que el servicio dependa de una interfaz, no de una implementación concreta — y sembrar el concepto de puertos, que se retoma en arquitectura hexagonal más adelante.

**Contenido:**
- Mostrar `AccountService` dependiendo directo de `JpaAccountRepositoryImpl` (clase concreta) vs dependiendo de la interfaz `AccountRepository`.
- [PROBABLE] Este es el punto exacto del curso donde conviene decir en voz alta: "esto que estamos haciendo — depender de una abstracción — es la base de lo que en la próxima presentación va a llamarse 'puerto' en arquitectura hexagonal". No lo desarrolles todavía, solo plantá la semilla.

**Demo/código:**
```java
// VIOLACIÓN — depende de la implementación concreta
@Service
public class AccountService {
    private final JpaAccountRepositoryImpl repository; // clase concreta de infraestructura
}

// CORRECTO — depende de la abstracción
@Service
public class AccountService {
    private final AccountRepository repository; // interfaz — Spring inyecta la implementación
}
```

**Actividad:** el equipo dibuja (en un miro/pizarra) la flecha de dependencia antes y después — de "Service → Implementación" a "Service → Interfaz ← Implementación".

**Slides sugeridos:**
1. DIP en una frase
2. Service dependiendo de la clase concreta (mal)
3. Service dependiendo de la interfaz (bien)
4. La flecha de dependencia se invierte (diagrama)
5. Semilla: esto es un "puerto" — se retoma en la próxima presentación

---

## Clase 11 — Inyección de dependencias: qué es y cómo funciona
**Duración:** 35 min
**Objetivo:** entender el mecanismo de Spring que hace posible todo lo visto en la Clase 10, y resolver ambigüedad con `@Qualifier`/`@Primary`.

**Contenido:**
- Constructor injection vs field injection (`@Autowired` en el campo) — por qué constructor injection es la práctica recomendada (inmutabilidad, testeable sin reflection, dependencias explícitas).
- Escenario de ambigüedad: dos implementaciones de `NotificationService` (`EmailNotificationService`, `SmsNotificationService`) — Spring no puede elegir sola.
- `@Primary` para fijar un default, `@Qualifier` para forzar una elección explícita.

**Demo/código:**
```java
// Field injection — evitar
@Service
public class AccountService {
    @Autowired
    private AccountRepository repository; // no es final, difícil de testear
}

// Constructor injection — recomendado
@Service
public class AccountService {
    private final AccountRepository repository;
    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }
}

// Ambigüedad resuelta
public interface NotificationService { void enviar(String destino, String mensaje); }

@Primary
@Component
public class EmailNotificationService implements NotificationService { /* ... */ }

@Component
public class SmsNotificationService implements NotificationService { /* ... */ }

@Service
public class AccountService {
    private final NotificationService notifier;
    public AccountService(@Qualifier("smsNotificationService") NotificationService notifier) {
        this.notifier = notifier;
    }
}
```

**Actividad:** el equipo agrega una segunda implementación de una interfaz propia del proyecto y resuelve la ambigüedad con `@Qualifier` en vivo.

**Slides sugeridos:**
1. Constructor injection vs field injection (comparación)
2. Por qué constructor injection gana (testeable, inmutable, explícito)
3. El problema: dos implementaciones, una interfaz
4. @Primary: el default
5. @Qualifier: la elección explícita

---

## Clase 12 — Package-by-layer vs package-by-feature
**Duración:** 40 min
**Objetivo:** refactorizar la estructura de paquetes del proyecto ya construido.

**Contenido:**
- Estructura actual (package-by-layer): `com.atlasbank.controller`, `com.atlasbank.service`, `com.atlasbank.repository`, `com.atlasbank.model` — mostrar cómo, al crecer el dominio (agregar `Transaction` en el futuro), cada paquete se llena de clases de features distintas mezcladas.
- Refactor a package-by-feature: `com.atlasbank.account.{controller,service,repository,model}`.
- [PROBABLE] Mencionar que esta reorganización por feature es, además, el primer paso естructural hacia los paquetes `domain/application/infrastructure` que va a pedir la arquitectura hexagonal en la próxima presentación — no es solo estética, es preparación.

**Demo/código:**
```
// ANTES — package-by-layer
com.atlasbank.controller.AccountController
com.atlasbank.service.AccountService
com.atlasbank.repository.AccountRepository
com.atlasbank.model.Account

// DESPUÉS — package-by-feature
com.atlasbank.account.AccountController
com.atlasbank.account.AccountService
com.atlasbank.account.AccountRepository
com.atlasbank.account.Account
```

**Actividad:** el equipo hace el refactor de paquetes en vivo sobre el proyecto de la Clase 4, usando el refactor tool del IDE (no manual) y corre los tests después para confirmar que nada se rompió.

**Slides sugeridos:**
1. Package-by-layer: la estructura actual
2. El problema cuando el dominio crece (spoiler: Transaction)
3. Package-by-feature: la estructura nueva
4. Comparación lado a lado
5. Esto es el primer paso hacia hexagonal (semilla para la próxima presentación)

---

## Clase 13 — Checkpoint del proyecto
**Duración:** 60 min
**Objetivo:** validar que el proyecto `atlas-bank` cumple, de forma verificable, todo lo enseñado en la Sección 1 — ni más ni menos.

**Criterios de aceptación (checklist literal a proyectar):**
- [ ] `POST /api/v1/accounts` crea una cuenta (contra la colección Postman real del equipo)
- [ ] `GET /api/v1/accounts` lista todas las cuentas
- [ ] `GET /api/v1/accounts/{id}` devuelve una cuenta por id
- [ ] `PATCH /api/v1/accounts/{id}/close` cambia el status a `CLOSED`
- [ ] `GET /api/v1/accounts/{id}/dashboard` — **excluido de este checkpoint** o implementado como stub explícito (depende de Transactions, fuera de alcance)
- [ ] `AccountService` inyectado por constructor, sin field injection
- [ ] Al menos una responsabilidad extraída fuera de `AccountService` (SRP, Clase 6)
- [ ] Al menos una variación de comportamiento resuelta con Strategy en vez de `if/else` (OCP, Clase 7)
- [ ] Ninguna clase depende de una implementación concreta de repositorio (DIP, Clase 10)
- [ ] Estructura de paquetes por feature (`com.atlasbank.account.*`), no por capa

**Actividad:** cada compañero corre su propia copia del proyecto contra la colección Postman completa y marca el checklist en vivo. Los que no cumplan algún ítem lo resuelven ahí mismo con ayuda del grupo (no se avanza a la Presentación 2 con deuda pendiente).

**Slides sugeridos:**
1. El checklist completo (proyectado)
2. Demo en vivo: correr la colección Postman contra el proyecto final
3. Qué queda explícitamente fuera (dashboard, transactions, auth, AI agent)
4. Qué viene en la Presentación 2 (arquitectura hexagonal — gancho de cierre)

---

## Anexo — Modelo extraído de la colección Postman (fuente de verdad para todos los ejemplos de código)

**Endpoints dentro de alcance de esta presentación (carpeta "Accounts"):**
| Método | Endpoint | Clase donde se implementa |
|--------|----------|---------------------------|
| POST | `/api/v1/accounts` | Clase 4 |
| GET | `/api/v1/accounts` | Clase 4 |
| GET | `/api/v1/accounts/{id}` | Clase 4 |
| PATCH | `/api/v1/accounts/{id}/close` | Clase 13 |
| GET | `/api/v1/accounts/{id}/dashboard` | Fuera de alcance — ver nota de la Clase 13 |

**Endpoints fuera de alcance (presentaciones futuras):**
- `Auth` (Keycloak): `POST /realms/atlas-bank/protocol/openid-connect/token`
- `Transactions`: `POST /api/v1/transactions/transfer`, `GET /api/v1/transactions/{id}/transactions`
- `AI Agent`: `POST /api/v1/ai/chat`