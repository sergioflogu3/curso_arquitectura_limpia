# Sección 4: Patrones de Diseño Esenciales en Spring

Guía de referencia para el equipo. Resume el contenido de las clases de la sección, con el código real del proyecto **atlas-bank**, las capturas de pantalla y los slides de cada clase. Sirve como material de consulta para quien no vio la clase en vivo, quiera repasar un concepto puntual, o esté retomando el curso y necesite saber por dónde va y qué le falta.

## Qué vas a encontrar

- Por qué un patrón sin un dolor concreto es *overengineering* — la regla que guía toda la sección
- **Strategy**: de `switch` de comisiones a una interfaz con múltiples implementaciones, inyectadas por Spring como `List<FeeCalculator>`
- **Template Method**: el flujo `validar → calcular comisión → ejecutar → guardar` formalizado en una clase base abstracta
- **Factory Method**: la construcción de `Transaction` centralizada en `TransactionFactory`, fuera del service
- **Observer**: `ApplicationEventPublisher` + `@EventListener` para desacoplar side effects (auditoría, notificaciones) del service que ejecuta la transferencia
- **Decorator**: un decorador manual (`AuditableAccountService`) y su equivalente automático con Spring AOP (`@Transactional`, `@Cacheable`, `@Async`)
- Un bug real que quedó sin corregir en el decorador manual — documentado en la Clase 12, para que el equipo lo detecte y lo resuelva

El proyecto vive en [`/proyecto`](../proyecto) y evoluciona clase a clase sobre la base que quedó al cierre de la [Sección 3](../seccion_3). Cada carpeta (`clase_01` … `clase_13`) contiene el slide (`.html`) y/o las capturas de pantalla de ese momento del curso. Las clases 5, 6, 8, 10 y 12 son puramente de código (no tienen carpeta propia porque no hay slide ni captura asociada) — su contenido se explica igual en este README con el diff real.

## Pasos para completar la sección

1. Asegurate de haber terminado la **Sección 3**: `atlas-bank` debe validar JWT de Keycloak como Resource Server y proteger cada endpoint por rol.
2. Seguí las clases **en orden** (0 → 13): cada una depende del código que dejó la anterior — Template Method depende de que Strategy ya esté resuelto, Factory Method depende de Template Method, Observer se engancha sobre el `execute()` que arma Template Method, y el Decorator envuelve un `AccountService` que no cambia en toda la sección.
3. Podés abrir el proyecto en `/proyecto` y hacer `checkout` del commit correspondiente a cada clase si querés ver el estado exacto (ver la tabla de abajo).
4. Prestá atención a los **paquetes**: en la Clase 7, `TransferService`, `TransferContext`, `TransactionProcessor` e `ITransferService` se mueven de `transaction.service` a `transaction.service.transfer`. Si venís de un `target/` compilado antes de ese commit, corré `mvn clean` — si no, quedan `.class` viejos del paquete anterior y Spring falla al arrancar con `ConflictingBeanDefinitionException` (dos beans con el mismo nombre, uno de cada paquete).
5. Al final de la sección, `atlas-bank` debe tener:
   - Cero `switch`/`if-else` por tipo de cuenta para calcular comisiones — todo resuelto por `List<FeeCalculator>` con auto-selección.
   - Un único flujo de transacción (`TransactionProcessor<C>`) reutilizable para transferencias y, a futuro, depósitos/retiros.
   - La construcción de `Transaction` centralizada en `TransactionFactory`, no repartida en cada service.
   - Los side effects de una transferencia (auditoría, notificación) desacoplados del `TransferService` vía eventos.
   - Un ejemplo de decorador manual (`AuditableAccountService`) y uno automático (`@Transactional` sobre `AccountService`) conviviendo en el mismo proyecto.
6. Como práctica: probá `POST /api/v1/transactions/transfer` con cuentas de distinto `type` (`SAVINGS`, `CHECKING`, `PREMIUM`, y uno inventado) y confirmá que cada una calcula la comisión que le corresponde — y solo cae en `DefaultFeeCalculator` (comisión 0) cuando el tipo no tiene una estrategia propia. Después probá `POST /api/v1/accounts` y fijate qué devuelve el body — es la Clase 12.

| Clase | Tema | Commit de referencia |
|---|---|---|
| 1 | ¿Por qué patrones de diseño? | `14c9016` |
| 2 | Strategy en Spring: Inyección de colecciones | `33bb2cb` |
| 3 | Strategy en el proyecto: Hands-on | `5828f7d`, `10dc563` |
| 4 | Template Method: parte 1 | `4f39728` |
| 5 | Template Method: parte 2 | `3c02409` |
| 6 | Template Method: parte 3 | `86f1f1e` |
| 7 | Factory Method: Creación desacoplada | `1e1af83` |
| 8 | Factory Method: ventaja y limitación | `bb72a61` |
| 9 | Observer: Eventos de aplicación | `fc3cb71` |
| 10 | Observer en Spring: `ApplicationEventPublisher` | `4ea3878` |
| 11 | Decorador: Enriquecer sin modificar 1 | `1f4089b` |
| 12 | Decorador: Enriquecer sin modificar 2 | `53c16ad` |
| 13 | Spring AOP: el decorador que ya estás usando | `fabdb86` |

---

## Clase 0 — Introducción

Bienvenidos a una nueva sección: patrones que organizan la lógica. Es 100% verdad, y no tanto — porque **Observer** no solo organiza lógica, también conecta piezas, pero tenía que ir en algún lado y decidimos incluirlo acá.

**Strategy** va a ser el punto de partida. Ya lo vimos en la Sección 2 cuando trabajamos el principio Open/Closed: ahí resolvimos una problemática y vimos, sin llamarlo así, cómo se implementa una strategy. Ahora lo vamos a ver desde un punto de vista más técnico, formal, y con una tarea propia.

**Template Method**: una clase abstracta con un método público que ejecuta métodos abstractos en un orden inalterable — las clases que extienden esa base implementan esos métodos abstractos como lo necesiten.

**Factory Method**: un patrón creacional que desacopla la creación de objetos de los services que los usan — separa la lógica de construcción ("hacer un `new` de algo") de la lógica de negocio del service, delegándola a una fábrica.

**Observer**: acá lo vamos a trabajar desde un punto de vista metodológico. Un evento se publica desde un service, y hay suscriptores interesados en saber cuándo se publica ese evento para disparar sus propios procesos — efectos secundarios que quedan separados del evento que los dispara, así el emisor no se carga de lógica difícil de mantener.

**Decorator**: agrega comportamiento a una clase sin tocarla. Ya lo usamos muchas veces vía anotaciones (`@Transactional`, por ejemplo), pero acá lo vamos a construir a mano, línea por línea, para entender la estrategia de "envolver" una clase antes de ver cómo Spring lo automatiza.

Un llamado de atención importante para toda la sección: aplicar un patrón es seguir una fórmula para resolver un problema — no es la única fórmula, y no todo problema necesita uno. Conocerlos sirve para poder discutir en equipo si, en una problemática puntual, aplicar tal patrón tiene sentido o no. Esa discusión tiene que estar presente al diseñar. Esto no significa que a partir de ahora todo tiene que ser un patrón — las aplicaciones evolucionaron, y muchas veces una librería o una dependencia ya resuelve lo que antes resolvíamos a mano con un patrón.

**Temas puntuales de la sección:**
- **Strategy**: reemplazar el `switch` de comisiones por una interfaz con múltiples implementaciones
- **Inyección de colecciones en Spring**: seleccionar la estrategia correcta en runtime
- **Template Method**: formalizar el flujo validar → calcular → ejecutar → guardar
- **Factory Method**: centralizar decisiones de construcción en `TransactionFactory`
- **Observer con `ApplicationEventPublisher`**: desacoplar side effects sin que el service los conozca
- **Decorator**: enriquecer comportamiento sin modificar el service original

---

## Clase 1 — ¿Por qué patrones de diseño?

Slide: [`clase_01/02- ¿Por qué patrones de diseño?.html`](<clase_01/02- ¿Por qué patrones de diseño?.html>)

> "Un patrón sin un dolor es *overengineering*."

Hasta ahora construimos un monolito con capas claras, DTOs, validaciones, manejo de errores y seguridad con Keycloak. Ahora toca algo distinto: mirar el código que ya tenemos y encontrar los dolores — y para cada dolor, un patrón que lo resuelve. Esta es la regla de toda la sección: **no se implementa un patrón porque es elegante o porque está en un libro. Se implementa porque hay un problema concreto que lo necesita.**

**¿Qué son los patrones de diseño?** Soluciones probadas a problemas recurrentes. No son código para copiar y pegar — son ideas que se adaptan al contexto de cada proyecto. Se formalizaron en 1994 (Gang of Four), y Spring Boot ya los usa internamente por todos lados: `@Autowired` es inyección de dependencias, `@Transactional` es un proxy (Decorator), `ApplicationEventPublisher` es un Observer. En esta sección hacemos explícito lo que Spring hace implícito.

**5 patrones, 5 dolores:**

| Patrón | Dolor en atlas-bank |
|---|---|
| Strategy | `switch` de comisiones que crece con cada tipo de cuenta |
| Template Method | Flujo de transacción repetido con variaciones |
| Factory Method | Creación de transacciones acoplada al tipo |
| Observer | Side effects (notificaciones, auditoría) acoplados al service |
| Decorator | Agregar comportamiento (auditoría) sin modificar el service |

**Lo que ya tenemos:** `FeeCalculator` con `supports()` y `calculate()`, y sus implementaciones (`SavingFeeCalculator`, `CheckingFeeCalculator`, `DefaultFeeCalculator`) — creadas en la Sección 2 al aplicar Open/Closed, inyectadas por Spring como `List<FeeCalculator>`. Es un **proto-Strategy** que ya existe en el código: en esta sección lo formalizamos, le ponemos nombre, y lo completamos.

---

## Clase 2 — Strategy en Spring: Inyección de colecciones

Slides: [`clase_02/03- Strategy en Spring- Inyección de colecciones 1.html`](<clase_02/03- Strategy en Spring- Inyección de colecciones 1.html>) (el concepto) y [`clase_02/03- Strategy en Spring- Inyección de colecciones 2.html`](<clase_02/03- Strategy en Spring- Inyección de colecciones 2.html>) (la inyección). Commit: `33bb2cb` (sin cambios de código — la implementación llega en la Clase 3).

> "Ya lo implementaron en Open/Closed. Ahora le ponemos nombre."

**Strategy**: definir una familia de algoritmos, encapsular cada uno, y hacerlos intercambiables. Tiene tres participantes:

| Rol | En atlas-bank |
|---|---|
| **Context** | `TransferService`, con `List<FeeCalculator> feeCalculators` |
| **Strategy** (interfaz) | `FeeCalculator` — `supports(accountType)`, `calculate(amount)` |
| **Concrete Strategies** | `SavingFeeCalculator` (1.0%), `CheckingFeeCalculator` (1.5%), `DefaultFeeCalculator` (0%, fallback) |

**¿Cuándo usar Strategy?** Cuando hay un comportamiento que varía según el contexto (tipo de cuenta, tipo de transacción, región...) y querés agregar variantes nuevas sin modificar código existente, eliminando `switch`/cadenas de `if-else`. Si agregar una variante nueva significa crear una clase nueva y nada más, es Strategy.

**Inyección de colecciones:** Spring escanea el contexto, encuentra todos los `@Component` que implementan `FeeCalculator`, y los inyecta automáticamente en `List<FeeCalculator> feeCalculators`. Si mañana se agrega una implementación nueva anotada con `@Component`, Spring la incluye sola — no se toca ni una línea del service que la consume.

```java
BigDecimal fee = feeCalculators.stream()
        .filter(fc -> fc.supports(from.getType()))  // ¿vos manejás este tipo?
        .findFirst()                                 // tomá la primera que diga sí
        .orElseThrow(...)                             // si ninguna → error
        .calculate(amount);                           // ejecutá el algoritmo
```

El service no tiene `switch`. No sabe cuántas estrategias existen. Cada estrategia se ofrece sola cuando le corresponde — a esto se lo llama **auto-selección**.

**Controlando la inyección:**

| Anotación | Para qué sirve | ¿Aplica acá? |
|---|---|---|
| `@Order` | Define la posición en la lista — menor número, mayor prioridad | Sí: garantizar que el fallback quede al final |
| `@Primary` | Marca un bean como opción por defecto cuando se inyecta **uno solo** | No — acá se inyecta una lista, no un bean único |
| `@Qualifier` | Elegís un bean específico por nombre, en tiempo de compilación | No — es lo opuesto a Strategy, que selecciona en runtime |

---

## Clase 3 — Strategy en el proyecto: Hands-on

![captura de clase](<clase_03/Captura de pantalla 2026-08-14 a la(s) 5.27.24 a. m..png>)

Commits: `5828f7d` (crea `PremiumFeeCalculator` vacío) y `10dc563` (lo implementa y agrega `@Order` a las cuatro estrategias).

**El problema real:** Spring no garantiza el orden en que el classpath scanning descubre los `@Component` — depende del orden de detección, y puede variar entre ejecuciones o entre entornos. `DefaultFeeCalculator.supports()` siempre devuelve `true` (es el fallback). Si por azar queda **primero** en la lista inyectada, el `.filter(fc -> fc.supports(...)).findFirst()` lo encuentra a él antes que a cualquier estrategia real, y **todas** las comisiones terminan calculándose en `0`, sin importar el tipo de cuenta — un bug intermitente, difícil de reproducir, que puede pasar tests hoy y fallar en producción mañana.

**La solución: `@Order`.**

```java
@Component
@Order(1)
public class SavingFeeCalculator implements FeeCalculator { /* ... */ }

@Component
@Order(1)
public class CheckingFeeCalculator implements FeeCalculator { /* ... */ }

@Component
@Order(1)
public class PremiumFeeCalculator implements FeeCalculator {
    @Override
    public boolean supports(String accountType) {
        return "PREMIUM".equals(accountType);
    }

    @Override
    public BigDecimal calculate(BigDecimal amount) {
        return BigDecimal.ZERO; // cuentas premium, sin comisión — por diseño
    }
}

@Component
@Order() // sin valor = Ordered.LOWEST_PRECEDENCE → siempre al final
public class DefaultFeeCalculator implements FeeCalculator {
    @Override
    public boolean supports(String accountType) {
        return true; // fallback
    }
    // ...
}
```

Las tres estrategias reales comparten `@Order(1)` — el orden relativo *entre ellas* no importa, porque sus `supports()` son mutuamente excluyentes (cada una responde a un `accountType` distinto). Lo que sí importa es que `DefaultFeeCalculator`, con `@Order()` (que por defecto vale `Ordered.LOWEST_PRECEDENCE`, la prioridad más baja posible), quede **siempre** último en la lista — así el fallback solo se ejecuta cuando ninguna estrategia real dijo que sí.

La captura muestra una transferencia real (`POST /api/v1/transactions/transfer`) ya con el fix aplicado, devolviendo la comisión correcta (`fee: 0`) para el tipo de cuenta involucrado — confirmando que la estrategia correcta responde, no el fallback por casualidad de orden.

---

## Clase 4 — Template Method: parte 1

Slide: [`clase_04/05- Template Method- parte 1.html`](<clase_04/05- Template Method- parte 1.html>). Commit: `4f39728` (solo el slide — la implementación arranca en la Clase 5).

**Template Method**: definir el esqueleto de un algoritmo en una clase base, dejando que las subclases completen los pasos que varían.

**El flujo en `TransferService`:** primero una preparación (buscar las cuentas — no varía entre tipos de transacción), y después cuatro pasos fijos que reciben un **contexto** con los datos de la operación (sin estado guardado en el service): `validate(ctx)` → `calculateFee(ctx)` → `execute(ctx, fee)` → `save(ctx, fee)`. Transferencia, depósito y retiro comparten el mismo flujo con reglas distintas en cada paso — si ese flujo se copia en cada service, el código queda duplicado. Template Method lo centraliza.

**Clase abstracta + subclases:**

```
Abstract TransactionProcessor<C>
  + process(C ctx)          ← template method
  # validate(C ctx)
  # calculateFee(C ctx)
  # execute(C ctx, fee)
  # save(C ctx, fee)
        △
        │ extends
Concrete TransferService              Futuro DepositProcessor
  C = TransferContext(from, to, amount)   C = DepositContext(to, amount)
  + execute() → busca cuentas,            + execute() → busca cuenta,
    llama process(ctx)                      llama process(ctx)
  # validate(ctx) → fondos, activas       # validate(ctx) → cuenta activa
  # calculateFee(ctx) → Strategy          # calculateFee(ctx) → sin comisión
  # execute(ctx, fee) → débito+crédito    # execute(ctx, fee) → solo crédito
  # save(ctx, fee) → tipo TRANSFER        # save(ctx, fee) → tipo DEPOSIT
```

`TransferService` es la primera implementación concreta; `DepositProcessor` queda planteado como el ejemplo de "próxima variante" que este diseño deja lista para agregar sin duplicar el flujo.

---

## Clase 5 — Template Method: parte 2

Commit: `3c02409`. Se crea la clase base abstracta y el contexto:

```java
@RequiredArgsConstructor
public abstract class TransactionProcessor<C> {
    protected final TransactionRepository transactionRepository;

    @Transactional
    public Transaction process(C context) {
        validate(context);
        BigDecimal fee = calculateFee(context);
        execute(context, fee);
        return save(context, fee);
    }

    protected abstract void validate(C context);
    protected abstract BigDecimal calculateFee(C context);
    protected abstract void execute(C context, BigDecimal fee);
    protected abstract Transaction save(C context, BigDecimal fee);
}
```

```java
public record TransferContext(Account fromAccount, Account toAccount, BigDecimal amount) {}
```

Y `TransferService` pasa a extender `TransactionProcessor<TransferContext>`, pero en este commit los cuatro métodos quedan como **stubs vacíos** (`validate` no hace nada, `calculateFee`/`save` devuelven `null`, `execute` no hace nada) — el esqueleto ya compila y define la forma, pero la lógica real (que antes vivía toda junta dentro de `execute(fromId, toId, amount)`) todavía no se movió a los pasos del template. Es un paso intermedio a propósito, para separar "definir el esqueleto" de "migrar la lógica" — se completa en la Clase 6.

---

## Clase 6 — Template Method: parte 3

Commit: `86f1f1e`. Se migra la lógica que antes vivía toda junta en `execute()` a cada paso abstracto:

```java
@Override
protected void validate(TransferContext context) {
    if (!"ACTIVE".equals(context.from().getStatus())) {
        throw new AccountNotActiveException(context.from().getId(), context.from().getStatus());
    }
    if (!"ACTIVE".equals(context.to().getStatus())) {
        throw new AccountNotActiveException(context.to().getId(), context.to().getStatus());
    }
    if (context.from().getBalance().compareTo(context.amount()) < 0) {
        throw new InsufficientFoundsException(context.from().getId(), context.from().getBalance(), context.amount());
    }
}

@Override
protected BigDecimal calculateFee(TransferContext context) {
    return feeCalculators.stream()
            .filter(fc -> fc.supports(context.from().getType()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No hay calculador para el tipo: " + context.from().getType()))
            .calculate(context.amount());
}

@Override
protected void execute(TransferContext context, BigDecimal fee) {
    context.from().setBalance(context.from().getBalance().subtract(context.amount()).subtract(fee));
    context.to().setBalance(context.to().getBalance().add(context.amount()));
    accountRepository.save(context.from());
    accountRepository.save(context.to());
}

@Override
protected Transaction save(TransferContext context, BigDecimal fee) {
    Transaction transaction = new Transaction();
    transaction.setType("TRANSFER");
    transaction.setSourceAccountId(context.from().getId());
    transaction.setTargetAccountId(context.to().getId());
    transaction.setAmount(context.amount());
    transaction.setFee(fee);
    transaction.setStatus("EXECUTED");
    return transactionRepository.save(transaction);
}
```

Y `execute(fromId, toId, amount)` (el método público del controller) queda reducido a buscar las cuentas y delegar todo el resto al template method heredado:

```java
public Transaction execute(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountRepository.findById(fromId).orElseThrow(() -> new AccountNotFoundException(fromId));
    Account to = accountRepository.findById(toId).orElseThrow(() -> new AccountNotFoundException(toId));
    return process(new TransferContext(from, to, amount));
}
```

De paso, `TransferContext` se renombra: `fromAccount`/`toAccount` pasan a `from`/`to`. Con esto, `TransferService` queda con exactamente la misma lógica de negocio que tenía antes de la Clase 4 — solo que ahora repartida en los cuatro pasos que `TransactionProcessor` va a poder reutilizar para otros tipos de transacción.

---

## Clase 7 — Factory Method: Creación desacoplada

Slide: [`clase_07/08- Factory Method- Creación desacoplada.html`](<clase_07/08- Factory Method- Creación desacoplada.html>). Commit: `1e1af83`.

**Factory Method**: centralizar las decisiones de construcción. No es "menos líneas de código" — es **menos acoplamiento**.

**Antes — decisiones dispersas** dentro de `save()`: strings mágicos (`"TRANSFER"`, `"EXECUTED"`), qué campos van según el tipo. Si mañana se agrega `DepositProcessor`, repetiría la misma mecánica con `"DEPOSIT"`, sin `source`, con `fee = ZERO` — la misma decisión, duplicada en cada service.

**Después — decisiones centralizadas:**

```java
public class TransactionFactory {
    public static Transaction createTransfer(TransferContext context, BigDecimal fee) {
        Transaction transaction = new Transaction();
        transaction.setType("TRANSFER");
        transaction.setSourceAccountId(context.from().getId());
        transaction.setTargetAccountId(context.to().getId());
        transaction.setAmount(context.amount());
        transaction.setFee(fee);
        transaction.setStatus("EXECUTED");
        return transaction;
    }
}
```

```java
@Override
protected Transaction save(TransferContext context, BigDecimal fee) {
    Transaction transaction = TransactionFactory.createTransfer(context, fee);
    return transactionRepository.save(transaction);
}
```

El service ya no sabe qué string va en `type`, ni cuál es el status por defecto, ni qué campos mapear — la factory decide todo eso. Si `Transaction` cambia internamente, los services no se enteran. Pensada a futuro, la fábrica queda lista para sumar `createDeposit(...)` y `createWithdrawal(...)` con sus propias reglas, sin que el service que las llama sepa nada de esos detalles.

**Reorganización de paquetes:** este commit también mueve `ITransferService`, `TransactionProcessor`, `TransferContext` y `TransferService` de `transaction.service` a `transaction.service.transfer`, y agrega `transaction.service.factory.TransactionFactory`. Si tenés un `target/` compilado de antes de este commit, corré `mvn clean` antes de levantar la app — si no, quedan `.class` huérfanos del paquete viejo y Spring falla al arrancar con `ConflictingBeanDefinitionException` (dos beans `transferService`, uno por cada paquete).

---

## Clase 8 — Factory Method: ventaja y limitación

Commit: `bb72a61`. Se agregan dos campos nuevos a `Transaction` — `createdBy` y `description` — y la factory empieza a completar `createdBy` sola:

```java
public static Transaction createTransfer(TransferContext context, BigDecimal fee) {
    Transaction transaction = new Transaction();
    transaction.setType("TRANSFER");
    transaction.setCreatedBy("SYSTEM"); // nuevo: la factory lo decide, el service no
    // ...
}
```

**La ventaja:** la factory absorbe cambios internos, defaults y reglas de mapeo que no dependen de quién la llama — agregar `createdBy = "SYSTEM"` no tocó ni una línea de `TransferService`.

**La limitación:** la factory **no** absorbe datos nuevos que vienen del usuario. Si mañana `description` tuviera que venir del request (por ejemplo, un motivo de transferencia que escribe el cliente), ese dato sí tiene que atravesar toda la cadena — DTO → Controller → Context → Factory — igual que antes. Es inevitable, la factory no lo evita. Saber esta diferencia es lo que permite decidir cuándo una factory se justifica y cuándo es *overengineering*: se justifica para centralizar decisiones internas repetidas; no resuelve la propagación de datos que legítimamente vienen de afuera.

---

## Clase 9 — Observer: Eventos de aplicación

Slide: [`clase_09/10- Observer- Eventos de aplicación.html`](<clase_09/10- Observer- Eventos de aplicación.html>). Commit: `fc3cb71` (solo el slide — la implementación llega en la Clase 10).

**Observer**: cuando algo sucede, los interesados se enteran y reaccionan, sin que el que produjo el evento los conozca.

**Side effects acoplados (el problema):** en un banco real, ejecutar una transferencia dispara mucho más que mover saldos — hay que enviar un comprobante, registrar un log de auditoría, correr una verificación de fraude. Si todo eso se resuelve inyectando `NotificationService`, `AuditService`, `FraudDetectionService` directo en `TransferService`, cada efecto secundario nuevo es una dependencia más — y `TransferService.execute()` termina conociendo medio sistema. Si mañana se agrega un `LoyaltyService` para sumar puntos, hay que volver a abrir y modificar `TransferService`. Y además quedan mezcladas dos categorías de error muy distintas: si falla el envío del email, ¿tiene que fallar la transferencia completa?

**Publicar y reaccionar (la solución):** `TransferService.execute()` hace una sola cosa extra — publica un evento (`TransactionExecutedEvent`, con `sourceId`, `targetId`, `amount`, `fee`). No sabe quién está escuchando. `NotificationListener`, `AuditListener`, `FraudDetectionListener` (o cualquier listener futuro) se suscriben al evento y reaccionan por su cuenta. Agregar un listener nuevo no requiere tocar `TransferService`.

**Los tres participantes:**

| Rol | En atlas-bank |
|---|---|
| **Event** | `TransactionExecuteEvent` — describe lo que pasó, lleva los datos que los listeners necesitan |
| **Publisher** | `TransferService`, usando `ApplicationEventPublisher` de Spring |
| **Listeners** | métodos anotados `@EventListener` — cada uno independiente, pueden vivir en paquetes distintos |

---

## Clase 10 — Observer en Spring: `ApplicationEventPublisher`

Commit: `4ea3878`. Se crea el evento:

```java
public record TransactionExecuteEvent(
        Long transactionId,
        String type,
        Long sourceAccountId,
        Long targetAccountId,
        BigDecimal amount,
        BigDecimal fee
) {}
```

Y dos listeners, cada uno reaccionando al mismo evento de forma completamente independiente:

```java
@Component
@Slf4j
public class AuditListener {
    @EventListener
    public void onTransactionExecuted(TransactionExecuteEvent event) {
        log.info("Registrando la auditoria - {} de cuenta #{} a cuenta #{} por ${}",
                event.type(), event.sourceAccountId(), event.targetAccountId(), event.amount());
    }
}

@Component
@Slf4j
public class NotificationListener {
    @EventListener
    public void onTransactionExecuted(TransactionExecuteEvent event) {
        log.info("Enviando comprobante de {} por ${} - Transaction #{}",
                event.type(), event.amount(), event.transactionId());
    }
}
```

`TransferService` pasa a inyectar `ApplicationEventPublisher` y, después de que `process(...)` (el template method de la Clase 6) devuelve la transacción ya guardada, publica el evento:

```java
public TransferService(TransactionRepository transactionRepository,
                       AccountRepository accountRepository,
                       List<FeeCalculator> feeCalculators,
                       ApplicationEventPublisher eventPublisher) {
    super(transactionRepository);
    this.accountRepository = accountRepository;
    this.feeCalculators = feeCalculators;
    this.eventPublisher = eventPublisher;
}

@Override
@Transactional
public Transaction execute(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountRepository.findById(fromId).orElseThrow(() -> new AccountNotFoundException(fromId));
    Account to = accountRepository.findById(toId).orElseThrow(() -> new AccountNotFoundException(toId));
    Transaction transaction = process(new TransferContext(from, to, amount));
    eventPublisher.publishEvent(new TransactionExecuteEvent(
            transaction.getId(), transaction.getType(),
            transaction.getSourceAccountId(), transaction.getTargetAccountId(),
            transaction.getAmount(), transaction.getFee()
    ));
    return transaction;
}
```

`TransferService` no importa `AuditListener` ni `NotificationListener` — ni siquiera sabe que existen. Agregar un tercer listener (por ejemplo, un `FraudDetectionListener`) es crear una clase nueva con `@Component` y `@EventListener`, sin tocar `TransferService`.

---

## Clase 11 — Decorador: Enriquecer sin modificar 1

Slide: [`clase_11/12- Decorador- Enriquecer sin modificar 1.html`](<clase_11/12- Decorador- Enriquecer sin modificar 1.html>). Commit: `1f4089b` (solo el slide — la implementación llega en la Clase 12).

**Decorator**: agregar comportamiento a un objeto sin modificar su código. Envolver, no heredar.

**Decorator en atlas-bank:**

```
Controller (AccountController)
    │ inyecta IAccountService
    ▼
Decorator (AuditableAccountService, @Primary)
    log antes → delega → log después
    │ delega
    ▼
Original (AccountService)
    lógica de negocio pura
```

El controller no sabe que existe el decorador — sigue inyectando `IAccountService`. `AccountService` no sabe que está siendo decorado — sigue sin un solo log de auditoría adentro. El acoplamiento queda resuelto con `@Primary` en el decorador: cuando Spring tiene que resolver `IAccountService`, entrega el decorador, no el original.

**Spring AOP: decoradores que ya usás**, sin haber escrito un decorador a mano:

| Anotación | Qué hace | Cómo lo hace |
|---|---|---|
| `@Transactional` | Abre transacción antes del método, hace commit si sale bien, rollback si hay excepción | Proxy que envuelve el bean original |
| `@Cacheable` | Verifica el caché antes de ejecutar; si hay resultado cacheado, lo devuelve, si no, ejecuta y cachea | Proxy que envuelve el bean original |
| `@Async` | Ejecuta el método en otro thread; el caller no espera el resultado | Proxy que envuelve el bean original |

---

## Clase 12 — Decorador: Enriquecer sin modificar 2

Commit: `53c16ad`. Se crea el decorador manual:

```java
@Slf4j
@Component
@Primary
public class AuditableAccountService implements IAccountService {

    private final IAccountService delegate;

    public AuditableAccountService(@Qualifier("accountService") IAccountService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Account create(Account account) {
        log.info("Creando una cuenta - numero: {}, titular {}",
                account.getAccountNumber(), account.getOwnerName());
        Account created = delegate.create(account);
        log.info("Cuenta creada exitosamente - ID: {}", created.getId());
        return null;
    }

    @Override
    public List<Account> findAll() {
        return delegate.findAll();
    }

    @Override
    public Account findById(Long id) {
        return delegate.findById(id);
    }
}
```

`@Qualifier("accountService")` en el constructor apunta explícitamente al bean original (`AccountService`), para no inyectarse a sí mismo por accidente (`AuditableAccountService` también implementa `IAccountService`, y es `@Primary`). `findAll()` y `findById()` delegan de forma transparente, sin agregar nada — el ejemplo pedagógico se concentra en `create()`.

> **Bug real, sin corregir en este commit:** `create()` llama a `delegate.create(account)`, loguea el `id` de la cuenta creada (`created.getId()`) — pero al final hace `return null;` en lugar de `return created;`. Como `AuditableAccountService` es `@Primary`, es el bean que `AccountController` termina usando. El resultado, hoy: `POST /api/v1/accounts` responde `201 Created`, pero con el **body vacío** (el mapper de MapStruct devuelve `null` de forma segura ante una entity `null`, así que no hay excepción — solo una respuesta sin datos). `AccountService` (el original) está perfecto; el bug vive exclusivamente en el decorador, y por eso es fácil no verlo si no se sabe que el decorador existe. Vale la pena corregirlo (`return created;`) antes de dar por cerrada la sección — es, además, un buen ejemplo de por qué un decorador que oculta el original puede esconder bugs que un test contra `AccountService` a secas no encontraría.

---

## Clase 13 — Spring AOP: el decorador que ya estás usando

Slide: [`clase_13/14- Spring AOP- el decorador que ya estás usando.html`](<clase_13/14- Spring AOP- el decorador que ya estás usando.html>). Commit: `fabdb86`.

**¿Qué es AOP?** *Aspect-Oriented Programming*: un mecanismo para interceptar la ejecución de métodos y agregar comportamiento antes, después o alrededor, sin tocar el código original. Cuando Spring encuentra ciertas anotaciones, genera un **proxy** en runtime que envuelve al bean real — ese proxy es un decorador automático.

```
Caller → Controller → Proxy (invisible, Spring AOP: abre tx → delega → commit/rollback) → Bean real (AccountService)
```

El caller cree que habla con el bean real. En realidad habla con el proxy.

**No toda `@` es AOP:**

| Envuelven la ejecución → **sí es AOP** | Configuran el bean → **no es AOP** |
|---|---|
| `@Transactional`, `@Cacheable`, `@Async`, `@Retryable` | `@Component`, `@Service`, `@Primary`, `@Qualifier`, `@Order` |
| Generan un proxy que intercepta el método | No interceptan nada, solo le dicen a Spring cómo registrar/inyectar el bean |

Ejemplo concreto en el código — `AccountService` gana `@Transactional` en sus tres métodos:

```java
@Override
@Transactional
public Account create(Account account) {
    return accountRepository.save(account);
}

@Override
@Transactional(readOnly = true)
public List<Account> findAll() {
    return accountRepository.findAll();
}

@Override
@Transactional(readOnly = true)
public Account findById(Long id) {
    return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
}
```

Nada de esto modifica el cuerpo de los métodos — Spring AOP genera el proxy transaccional solo, en tiempo de ejecución, a partir de la anotación.

**¿Cuándo usar cada uno?**

| | Decorator manual (`AuditableAccountService`) | Decorator AOP (`@Transactional`, `@Cacheable`) |
|---|---|---|
| Ventaja | Lógica específica de negocio, visible en el código fuente, fácil de debuguear | Genérico, aplica a muchos beans, cero *boilerplate*, una sola anotación |
| Desventaja | *Boilerplate* si la interfaz crece (hay que implementar todos los métodos, aunque solo uno cambie) | Invisible — más difícil de debuguear cuando algo falla |

No compiten entre sí — se complementan. `AccountService` termina, al cierre de la sección, envuelto por **dos** decoradores a la vez: el manual (`AuditableAccountService`, explícito en el código) y el automático (el proxy de `@Transactional`, generado por Spring AOP).

---

Con esto se cierra la Sección 4: `atlas-bank` reemplazó el `switch` de comisiones por Strategy, formalizó el flujo de transacción con Template Method, centralizó la construcción de `Transaction` con Factory Method, desacopló los side effects de una transferencia con Observer, y tiene tanto un decorador manual como el equivalente automático de Spring AOP conviviendo sobre el mismo service.
