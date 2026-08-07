# Sección 2 — Arquitectura en capas con criterio

Guía de referencia para el equipo. Resume el contenido de las clases de la sección, con el código real del proyecto **atlas-bank**, las capturas de pantalla y los slides de cada clase. Sirve como material de consulta para quien no vio la clase en vivo, quiera repasar un concepto puntual, o esté retomando el curso y necesite saber por dónde va y qué le falta.

## Qué vas a encontrar

- Por qué la entity no debe salir del service: DTOs de request y response
- Mapeo manual vs MapStruct
- Excepciones de dominio con `ProblemDetail` (RFC 7807)
- `@RestControllerAdvice` centralizado
- Bean Validation en DTOs: constraints estándar y mensajes custom
- Validaciones custom de negocio con `@Constraint`

El proyecto vive en [`/proyecto`](../proyecto) y evoluciona clase a clase sobre la base que quedó al cierre de la [Sección 1](../seccion_1). Cada carpeta (`clase_01` … `clase_10`) contiene el slide (`.html`) y/o las capturas de pantalla de ese momento del curso. Las clases 3, 4 y 5 son puramente de código (no tienen carpeta propia porque no hay slide ni captura asociada) — su contenido se explica igual en este README con el diff real.

## Pasos para completar la sección

1. Asegurate de haber terminado la **Sección 1**: `atlas-bank` debe compilar con capas separadas (`controller` / `service` / `repository`), interfaces (DIP) y `FeeCalculator` (OCP).
2. Seguí las clases **en orden** (0 → 10): cada una depende del código que dejó la anterior. Podés abrir el proyecto en `/proyecto` y hacer `checkout` del commit correspondiente a cada clase si querés ver el estado exacto (ver la tabla de abajo).
3. En cada clase de esta guía vas a encontrar: qué problema resuelve, qué cambia en el código, y (cuando aplica) el slide o la captura de pantalla original.
4. Al final de la sección, `atlas-bank` debe:
   - No exponer la `Entity` en ningún endpoint (solo DTOs).
   - Mapear con MapStruct, no a mano.
   - Devolver errores en formato `ProblemDetail` (RFC 7807) para toda excepción, capturados en un único `@RestControllerAdvice`.
   - Validar los DTOs de entrada con Bean Validation (constraints estándar + mensajes custom).
   - Tener al menos una validación custom de negocio a nivel de clase (`@Constraint` de tipo `TYPE`, no de campo).
5. Como práctica, correlo localmente y probá con Postman/curl los casos de error de cada clase (los mismos que se muestran en las capturas): un `POST /api/v1/accounts` con body vacío, una transferencia a una cuenta inexistente, una transferencia de una cuenta a sí misma, etc. Si tu respuesta no se parece a la de la captura, revisá el diff de esa clase.

| Clase | Tema | Commit de referencia |
|---|---|---|
| 1 | Anatomía de un monolito bien hecho | — (repaso, sin cambios de código) |
| 2 | DTOs — separación entre lo que se expone y lo que se persiste | `18bcfd3` |
| 3 | DTOs — request, response y `@Data` | `18bcfd3` |
| 4 | Mapeo de DTOs: MapStruct 1 | `4cd52fe` |
| 5 | Mapeo de DTOs: MapStruct 2 | `5fd96dd` |
| 6 | ProblemDetail: errores con estándar RFC 7807 | `9eca3e7` |
| 7 | `@RestControllerAdvice` centralizado | `fe1195e` |
| 8 | Bean Validation en DTOs | `70a3e81` |
| 9 | Validaciones custom de petición — parte 1 | `59644f7` |
| 10 | Validaciones custom de petición — parte 2 | `324b863` |

---

## Clase 0 — Introducción

Todas las secciones van a aportar soluciones y mejoras a nuestro proyecto. En la primera sección creamos un proyecto con deficiencias, aplicamos SOLID y resolvimos algunos problemas, pero todavía queda mucho camino por recorrer. En esta sección le va a tocar a otro tipo de problemas: manejo ideal de excepciones, DTOs, y mapeo con MapStruct.

Nosotros estamos exponiendo la `Entity` directamente, y sabemos que eso no es bueno: estamos exponiendo la estructura de nuestra base de datos. Ahí aparece el patrón **DTO**. Pero cuando aparece el DTO tengo que mapear — transformar de un DTO a una Entity y viceversa — y ahí aparece el concepto de **mapeo**, que vamos a resolver con **MapStruct**. También vamos a usar mejor **Lombok**: cuándo conviene `@Data` y cuándo conviene separar `@Getter`/`@Setter`.

El otro tema central: el manejo de errores. Hay tres tipos de errores posibles en una petición:
- los que vienen del formato del dato, que se validan con **Bean Validation**;
- los que no se pueden resolver con Bean Validation y necesitan un **validador custom** (`@Constraint`);
- los de **lógica de negocio**, que se resuelven con excepciones de dominio.

Con el DTO mapeado y validado, van a ocurrir errores igual — ahí aparece `@RestControllerAdvice`, el manejo global de excepciones, uno de los temas centrales de la sección. Propuesta de la Sección 2: Bean Validation, decoradores custom, DTOs y manejo global de excepciones.

---

## Clase 1 — Anatomía de un monolito bien hecho

Slide: [`clase_01/slides-clase-15 (1).html`](<clase_01/slides-clase-15 (1).html>)

> "Un monolito mal hecho es un desastre. Un monolito bien hecho es un gran punto de partida."

**Las tres capas y la regla de dependencia**

| Capa | Responsabilidad | Incluye | NO incluye |
|---|---|---|---|
| Controller | Recibir y responder HTTP | DTOs, validación de formato | Lógica de negocio |
| Service | Orquestar la lógica | Reglas de negocio, transacciones | Acceso a datos, HTTP |
| Repository | Persistencia | Queries, acceso a BD | Lógica de negocio, HTTP |

Las dependencias fluyen en una sola dirección, siempre hacia abajo: el Controller conoce al Service, el Service conoce al Repository — nunca al revés. Si una capa inferior necesita comunicarse con una superior, se resuelve con eventos o interfaces, no con una referencia directa.

**Qué tiene ya `atlas-bank` (de la Sección 1)** — capas separadas, services con SRP, interfaces (DIP), package-by-feature con capas internas, `FeeCalculator` (OCP).

**Qué le falta** — y es exactamente lo que resuelve esta sección:
- DTOs: la entity sale directo al cliente.
- Manejo de errores: `RuntimeException` para todo.
- Validación: no se valida nada del request.
- El service orquestador todavía hace demasiado.

---

## Clase 2 — DTOs: separación entre lo que se expone y lo que se persiste

Slide: [`clase_02/slides-clase-16 (2).html`](<clase_02/slides-clase-16 (2).html>)

**¿Por qué la entity no debería llegar al cliente?**
- El cliente ve la estructura interna de tu base de datos.
- Si agregás un campo interno, aparece en la respuesta sin querer.
- Si necesitás que la respuesta sea distinta a lo que guardás, no podés.
- Estás acoplando el contrato de la API con el modelo de persistencia — un cambio interno se filtra al exterior.

**¿Qué es un DTO?** "Un objeto que define exactamente qué datos entran y qué datos salen."

**¿Dónde vive el mapeo?** Hay dos opciones válidas, y la diferencia es de criterio, no de corrección:

| | Opción A — DTO en el Service | Opción B — Entity en el Service |
|---|---|---|
| Quién mapea | El Service recibe y devuelve DTOs | El Controller mapea a DTO |
| Entity sale del Service | No | Sí (pero no del Controller) |
| Prioriza | Encapsulación | Reutilización interna |
| Controller | Ultra liviano, un pasamanos puro | Traduce entre DTO y entity |
| Contrato de API | Lo define el Service | Lo define el Controller |

En microservicios hay poca reutilización interna entre services y el valor está en el contrato hacia afuera, así que suele tener sentido que el Service sea el dueño del contrato (Opción A). Pero la Opción B también funciona.

**Decisión para atlas-bank:** Opción B — el Service devuelve entities, el Controller mapea a DTOs. Es la que se implementa en las próximas clases.

---

## Clase 3 — DTOs: request, response y `@Data`

Sin carpeta propia (sin slide ni captura) — contenido 100% de código. Commit: `18bcfd3`.

Se crean los primeros DTOs con Lombok `@Data`, y el mapeo se hace **a mano** (todavía sin MapStruct) en el Controller, tal como se decidió en la Clase 2:

```java
// AccountController — mapeo manual, "rústico" a propósito
@PostMapping
public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
    Account account = new Account();
    account.setAccountNumber(request.getAccountNumber());
    account.setOwnerName(request.getOwnerName());
    account.setEmail(request.getEmail());
    account.setType(request.getType());
    account.setBalance(request.getBalance());
    Account saved = accountService.create(account);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
}

// muy rústico, pero después lo mejoraremos
private AccountResponse toResponse(Account account) { /* ...campo a campo... */ }
```

Se crean `CreateAccountRequest`, `AccountResponse`, `TransferRequest` y `TransactionResponse`, todos con `@Data` (Lombok genera getters, setters, `equals`, `hashCode` y `toString` — está bien para un DTO, que no tiene identidad ni se persiste).

**El otro cambio importante — y el motivo de repasar Lombok:** la `Entity` deja de usar `@Data` y pasa a `@Getter @Setter` + `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` con `@EqualsAndHashCode.Include` solo en el `id`:

```java
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    // ...
}
```

`@Data` en una entity JPA es una fuente clásica de bugs: genera `equals`/`hashCode` sobre todos los campos (rompe con proxies de Hibernate y colecciones antes de tener `id`) y un `toString` que puede disparar `LazyInitializationException` si hay relaciones lazy. Regla práctica: `@Data` para DTOs, `@Getter`/`@Setter` + `equals`/`hashCode` explícito por `id` para entities.

---

## Clase 4 — Mapeo de DTOs: MapStruct 1

Sin carpeta propia — contenido 100% de código. Commit: `4cd52fe`.

Se reemplaza el mapeo manual de la Clase 3 por **MapStruct**. Se agrega la dependencia al `pom.xml` (`mapstruct` + `mapstruct-processor` como annotation processor, junto con `lombok-mapstruct-binding` para que Lombok y MapStruct se lleven bien) y se crean las interfaces mapper:

```java
@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Account toEntity(CreateAccountRequest request);

    AccountResponse toResponse(Account account);
}
```

MapStruct genera la implementación (`AccountMapperImpl`) en tiempo de compilación — cero reflection, cero costo en runtime. Los campos que no vienen en el request (`id`, `status`, `createdAt`) se ignoran explícitamente para que el mapper no falle al no encontrar origen. El Controller cambia de tener un método `toResponse` privado a inyectar el mapper y usarlo:

```java
private final AccountMapper accountMapper;
// ...
Account account = accountMapper.toEntity(request);
return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.toResponse(saved));
```

---

## Clase 5 — Mapeo de DTOs: MapStruct 2

Sin carpeta propia — contenido 100% de código. Commit: `5fd96dd` ("bug: configuración del pom.xml").

Cierra lo que quedó pendiente de la Clase 4 y corrige un bug real de configuración — vale la pena entenderlo porque es un error común al combinar Lombok + MapStruct:

1. **El orden de los `annotationProcessorPaths` importa.** El plugin del compilador tenía el bloque mal anidado (fuera de `<configuration>`), lo que hacía que MapStruct no generara la implementación correctamente. Se corrige la estructura del `pom.xml` para que Lombok, MapStruct y `lombok-mapstruct-binding` se declaren juntos, en ese orden, tanto para `compile` como para `test-compile`.
2. **Typo en un `@Mapping`:** `createAt` → `createdAt` en `AccountMapper` (el nombre del target tiene que matchear exactamente el campo de la entity, si no MapStruct falla en compilación o, peor, lo ignora silenciosamente).
3. Se termina de conectar `TransactionMapper` en `TransactionController` (que en la Clase 4 se había creado pero no se había wireado), eliminando el último `toResponse` manual que quedaba:

```java
private final TransactionMapper transactionMapper;
// ...
return ResponseEntity.ok(transactionMapper.toResponse(transaction));
```

Al cierre de esta clase, **ningún controller mapea a mano** — todo el mapeo DTO ↔ Entity pasa por MapStruct.

---

## Clase 6 — ProblemDetail: errores con estándar RFC 7807

Slide: [`clase_06/slides-clase-19.html`](clase_06/slides-clase-19.html)

**El problema de hoy:** todo error es un `RuntimeException` genérico que termina en un 500 sin estructura:

```json
// ❌ HTTP 500 Internal Server Error
{ "error": "Internal Server Error", "message": "Fondos insuficientes" }
// ¿Error del usuario? ¿Error del servidor? No hay forma de saberlo.
```

**La solución — `ProblemDetail` (RFC 7807),** built-in en Spring desde la versión 6: un formato estándar de error HTTP con `type`, `title`, `status` y `detail`.

```json
// ✅ HTTP 422 Unprocessable Entity
{
  "type": "about:blank",
  "title": "Fondos insuficientes",
  "status": 422,
  "detail": "La cuenta 1 no tiene fondos suficientes para transferir 5000.00"
}
```

Se crean tres excepciones de dominio, cada una en el paquete que le corresponde por feature (no todas en un paquete `exception` global):

```java
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id) {
        super("No se encontro la cuenta con ID: " + id);
    }
}
public class AccountNotActiveException extends RuntimeException { /* ... */ }
public class InsufficientFoundsException extends RuntimeException { /* ... */ }
```

Y se lanzan desde donde corresponde — `AccountService.findById` y `TransferService.execute` — reemplazando los `RuntimeException` genéricos. **Importante:** en esta clase todavía no existe un `@RestControllerAdvice` que las capture, así que en la práctica el efecto visible sigue siendo un 500 (ver la captura de la Clase 7). El manejo global es, a propósito, el tema de la clase siguiente.

---

## Clase 7 — `@RestControllerAdvice` centralizado

![Transferencia con cuenta inexistente devuelve 500 sin estructura](<clase_07/01-transfer-500-sin-problemdetail.png>)

Esta captura es el punto de partida de la clase: una transferencia a una cuenta que no existe (`fromAccountId: 999`) lanza `AccountNotFoundException` (creada en la Clase 6), pero como nadie la captura todavía, Spring Boot devuelve su página de error por defecto — un 500 sin ningún dato útil para el cliente.

La solución es centralizar el manejo de excepciones en un único `@RestControllerAdvice`, en lugar de poner `try/catch` en cada controller:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Cuenta no encontrada");
        return problem;
    }

    @ExceptionHandler(InsufficientFoundsException.class)
    public ProblemDetail handleInsufficientFounds(InsufficientFoundsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), exception.getMessage());
        problem.setTitle("Fondos insuficientes para realizar esta operacion");
        return problem;
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ProblemDetail handleAccountNotActive(AccountNotActiveException exception) { /* ...422... */ }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception exception) {
        // red de seguridad: cualquier excepción no mapeada cae acá como 500 genérico
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }
}
```

Cada excepción de dominio tiene su propio `@ExceptionHandler` con el status HTTP que le corresponde semánticamente (404 para "no encontrado", 422 para "entendí la petición pero no la puedo procesar"), y un `handleException` genérico como red de seguridad al final. A partir de esta clase, **todos** los errores del API pasan por acá.

---

## Clase 8 — Bean Validation en DTOs

| Antes — sin validar | Después — con Bean Validation |
|---|---|
| ![POST /accounts con body vacío se crea igual](<clase_08/01-post-accounts-sin-validar.png>) | ![POST /accounts con body vacío devuelve 400 con el detalle de cada campo](<clase_08/02-post-accounts-bean-validation.png>) |

Antes de esta clase, un `POST /api/v1/accounts` con `{}` como body se creaba igual — con todos los campos en `null` y `balance: 0`. Nada impedía persistir basura.

La solución: **Bean Validation** (`jakarta.validation`, ya presente vía `spring-boot-starter-validation`) sobre los DTOs de request, más `@Valid` en el controller para activarla:

```java
@Data
public class CreateAccountRequest {
    @NotBlank(message = "El numero de cuenta es obligatorio")
    private String accountNumber;
    @NotBlank(message = "El nombre del titular es obligatorio")
    private String ownerName;
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El eamil no tiene el formato valido")
    private String email;
    @NotBlank(message = "El tipo de cuenta es obligatorio")
    private String type;
    @PositiveOrZero(message = "El balance no puede ser negativo")
    private BigDecimal balance;
}
```

```java
@PostMapping
public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) { /* ... */ }
```

Y se agrega el handler que traduce `MethodArgumentNotValidException` (la que lanza Spring cuando `@Valid` falla) a `ProblemDetail`, juntando el mensaje de cada campo en una lista:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("Error de validacion");
    List<String> errors = new ArrayList<>();
    exception.getBindingResult().getFieldErrors()
            .forEach(error -> errors.add(error.getField() + ": " + error.getDefaultMessage()));
    problem.setProperty("errors", errors);
    return problem;
}
```

Resultado: el mismo `POST` con body vacío ahora responde `400` con el detalle exacto de qué falta (ver la segunda captura). El mismo patrón (`@NotNull`, `@Positive`) se aplica a `TransferRequest`.

---

## Clase 9 — Validaciones custom de petición — parte 1

![Transferencia entre la misma cuenta de origen y destino](<clase_09/01-transfer-misma-cuenta-antes.png>) ![Misma transferencia, ya bloqueada](<clase_09/02-transfer-misma-cuenta-despues.png>)

Bean Validation cubre el formato de campos individuales, pero hay reglas que dependen de **más de un campo a la vez** — por ejemplo, que la cuenta de origen y la de destino de una transferencia no sean la misma. Eso no se resuelve con `@NotNull` en un campo: hace falta una **constraint a nivel de clase**.

Se crean los dos archivos que definen ese tipo de validación custom:

```java
@Target(ElementType.TYPE) // ← a nivel de clase, no de campo
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DifferentAccountsValidator.class)
public @interface DifferentAccounts {
    String message() default "La cuenta de origen y destino no pueden ser la misma";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
public class DifferentAccountsValidator implements ConstraintValidator<DifferentAccounts, TransferRequest> {
    @Override
    public boolean isValid(TransferRequest transferRequest, ConstraintValidatorContext context) {
        return true; // placeholder — la lógica real se completa en la Clase 10
    }
}
```

Notá la firma: `ConstraintValidator<DifferentAccounts, TransferRequest>` — el segundo tipo genérico es el **DTO completo**, no un `String` o un `Long` como en un validador de campo. Eso es lo que permite comparar `fromAccountId` contra `toAccountId` dentro del mismo `isValid`. En esta parte se define la arquitectura del validador; todavía no está conectado a `TransferRequest` ni tiene lógica real — eso es la Clase 10.

---

## Clase 10 — Validaciones custom de petición — parte 2

![Transferencia a la misma cuenta bloqueada por la constraint](<clase_10/01-transfer-misma-cuenta-bloqueada.png>)

Se completa lo que quedó pendiente de la Clase 9. Primero, se conecta la anotación al DTO:

```java
@Data
@DifferentAccounts
public class TransferRequest {
    @NotNull(message = "El id del origen es obligatorio")
    private Long fromAccountId;
    @NotNull(message = "El id del destino es obligatorio")
    private Long toAccountId;
    @NotNull(message = "La cantidad es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal amount;
}
```

Y se implementa la lógica real del validador — con el chequeo de nulos primero, porque si `fromAccountId` o `toAccountId` todavía no llegaron, esa responsabilidad es de `@NotNull`, no de este validador (evita un `NullPointerException` y evita duplicar el mensaje de error):

```java
@Override
public boolean isValid(TransferRequest transferRequest, ConstraintValidatorContext context) {
    if (transferRequest.getFromAccountId() == null || transferRequest.getToAccountId() == null) {
        return true;
    }
    return !transferRequest.getFromAccountId().equals(transferRequest.getToAccountId());
}
```

**El ajuste que faltaba en `GlobalExceptionHandler`:** una constraint a nivel de `TYPE` no produce un `FieldError` (no hay un campo específico al que culpar), sino un **error global** (`ObjectError`). El handler de la Clase 8 solo leía `getFieldErrors()` — con eso, el mensaje de `@DifferentAccounts` se perdía en silencio. Se agrega `getGlobalErrors()`:

```java
List<String> errors = new ArrayList<>();
exception.getBindingResult().getFieldErrors()
        .forEach(error -> errors.add(error.getField() + ": " + error.getDefaultMessage()));
exception.getBindingResult().getGlobalErrors()
        .forEach(error -> errors.add(error.getDefaultMessage()));
problem.setProperty("errors", errors);
```

![Los tres niveles de validación de una petición en atlas-bank](<clase_10/02-niveles-de-validacion.png>)

Con esto queda cerrado el cuadro completo de validación de la sección — **tres niveles**, cada uno con su herramienta:

| Nivel | Herramienta | Ejemplo en atlas-bank |
|---|---|---|
| Validaciones de formato | Bean Validation estándar (`@NotNull`, `@Email`, `@Positive`...) | `CreateAccountRequest`, `TransferRequest` |
| Validaciones de consistencia | `ConstraintValidator` custom a nivel de clase | `@DifferentAccounts` |
| Validaciones de negocio | Excepciones de dominio, lanzadas desde el Service | `AccountNotActiveException`, `InsufficientFoundsException` |

Con esto se cierra la Sección 2: `atlas-bank` ya no expone la `Entity`, mapea con MapStruct, devuelve errores RFC 7807 desde un único `@RestControllerAdvice`, y valida cada petición en sus tres niveles.
