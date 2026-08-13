# Sección 3: Seguridad con Keycloak

Guía de referencia para el equipo. Resume el contenido de las clases de la sección, con el código real del proyecto **atlas-bank**, las capturas de pantalla y los slides de cada clase. Sirve como material de consulta para quien no vio la clase en vivo, quiera repasar un concepto puntual, o esté retomando el curso y necesite saber por dónde va y qué le falta.

## Qué vas a encontrar

- Por qué la seguridad se diseña desde el principio de un proyecto, no se agrega al final
- Mapeo JPA explícito: `@Table`, `@Column` y referencia entre aggregates por ID (no por relación JPA)
- Docker: repaso de conceptos (imagen vs contenedor) y por qué Docker Compose es la opción para levantar varios servicios
- Keycloak como Identity Provider, levantado con Docker Compose: realm, client, roles y usuarios
- Spring Boot como Resource Server: validación de JWT contra el `issuer-uri` de Keycloak
- `SecurityFilterChain` con protección por rol, y un `JwtAuthenticationConverter` que traduce los roles de Keycloak a authorities de Spring Security
- Un bug real de configuración (roles de **client** vs roles de **realm** en el token) y cómo se detectó y corrigió
- Por qué la seguridad vive en infraestructura y el dominio no sabe que Keycloak existe

El proyecto vive en [`/proyecto`](../proyecto) y evoluciona clase a clase sobre la base que quedó al cierre de la [Sección 2](../seccion_2). Cada carpeta (`clase_01` … `clase_11`) contiene el slide (`.html`) y/o las capturas de pantalla de ese momento del curso. Las clases 7, 8, 9 y 10 son puramente de código (no tienen carpeta propia porque no hay slide ni captura asociada) — su contenido se explica igual en este README con el diff real.

## Pasos para completar la sección

1. Asegurate de haber terminado la **Sección 2**: `atlas-bank` debe mapear con MapStruct, exponer solo DTOs, y manejar errores con `ProblemDetail` desde un `@RestControllerAdvice` centralizado.
2. Seguí las clases **en orden** (0 → 11): cada una depende del estado que dejó la anterior. Podés abrir el proyecto en `/proyecto` y hacer `checkout` del commit correspondiente a cada clase si querés ver el estado exacto (ver la tabla de abajo).
3. Levantá Keycloak con `docker compose up -d` desde [`proyecto/docker`](../proyecto/docker), entrá al admin console (`http://localhost:8181`, `admin`/`admin`) y recreá manualmente lo que se arma en las clases 5 y 6: realm `atlas-bank`, client `atlas-bank-api` (confidential, Standard flow + Direct access grants), roles de **client** `ROLE_ADMIN` y `ROLE_USER`, y al menos dos usuarios de prueba (uno con cada rol).
4. Levantá `atlas-bank` (`./mvnw spring-boot:run`, puerto `8082`) y probá con Postman: pedí un token contra `http://localhost:8181/realms/atlas-bank/protocol/openid-connect/token` (grant `password`, client id/secret de `atlas-bank-api`) y usalo como Bearer token.
5. Al final de la sección, `atlas-bank` debe:
   - Validar JWT emitidos por Keycloak como Resource Server (sin manejar contraseñas ni sesiones propias).
   - Proteger cada endpoint según el rol que corresponda (`POST /api/v1/accounts` solo `ADMIN`, operaciones de transacciones para `USER` y `ADMIN`, etc.).
   - Traducir correctamente los roles del token (`resource_access.atlas-bank-api.roles`) a authorities de Spring Security.
   - Devolver `403` para un usuario `USER` que intente crear una cuenta, y `201` para un `ADMIN`.
6. Como práctica, decodificá el token de cada usuario de prueba (jwt.io o simplemente el payload en base64) y confirmá qué claim trae los roles antes de asumir dónde deberían estar — es exactamente el error real que se resuelve en la Clase 10.

| Clase | Tema | Commit de referencia |
|---|---|---|
| 1 | ¿Por qué seguridad en un proyecto de arquitectura? | `7f15448` |
| 2 | Mapeo JPA: `@Table`, `@Column` y referencia por Id | `13b2d11` |
| 3 | Docker: repasando conceptos | `2565554` |
| 4 | Docker Compose: la opción | `2cb1e68` |
| 5 | Keycloak con Docker Compose — parte 1 | `a8d43f4` |
| 6 | Keycloak con Docker Compose — parte 2 | `9955f2b` |
| 7 | Spring Boot como Resource Server | `e57b0a0` |
| 8 | `SecurityFilterChain`: protección basada en rol 1 | `51d38a2` |
| 9 | `SecurityFilterChain`: protección basada en rol 2 | `5905f4e` |
| 10 | Probando la seguridad: USER y ADMIN | `b94d96a` |
| 11 | Seguridad como infraestructura, no como dominio | `ecfe279` |

---

## Clase 0 — Introducción

Bienvenido Equipo, la sección de la seguridad, es el punto central de esta sección. ¿Qué vamos a hacer? Vamos a empezar a diagramar la seguridad de nuestra API ¿Por qué vamos a comenzar? Porque no vamos a terminar, es decir, vamos a comenzar acá, y seguramente vamos a ir resolviendo a medida que avanza el curso diferentes etapas de seguridad, pero lo vamos a comenzar en este punto. No es una buena estrategia dejar para lo último la seguridad, porque cuanto más lejos dejemos la seguridad del inicio de nuestro proyecto más vamos a tener que refactorizar y sabemos que refactorizar es un dolor de cabeza, entonces esa es nuestra estrategia comenzar, vamos a resolver algunos problemas de seguridad. Si hacemos un poco de historia nosotros podemos decir que existen tres tipos de seguridad: autenticación básica con el Authorization, con el user y el password encriptado; la segunda estrategia, donde tenemos más control, donde tenemos nuestro propio filtro y trabajamos con JWT (JSON Web Token), es decir nosotros diagramamos ese token, pero sabemos que requiere de bastante código y bastante costo, aunque tenemos control total sobre esa problemática y también nos ahorramos trabajar con un servidor de autenticación. Y el tercero, el que vamos a ver acá, más profesional, es trabajar con un servidor de autenticación que en este caso es Keycloak.

¿Qué vamos a trabajar? Vamos a trabajar con roles, y van a ver que con una serie de pasos muy breves vamos a resolver el tema de la seguridad de forma super sencilla. Y eso sabemos que es un capital, porque resolver algo de forma sencilla, rápida y prolija y profesional nos va a ahorrar bastante tiempo para resolver otras problemáticas de nuestra aplicación, es decir, vamos a delegar la seguridad a Keycloak básicamente. Pero también en esta sección vamos a aprovechar a hacer un repaso muy breve de Docker: la diferencia que existe entre Docker y Docker Compose, y la forma que tenemos de levantar algunos servicios utilizando estas herramientas — lo vamos a hacer porque el servidor Keycloak lo vamos a levantar en un contenedor de Docker, entonces lo necesitamos conocer, y si hace rato que no lo usamos, esta sección también está orientada a esos estudiantes. Así que ese es el desafío: comenzar con la seguridad, implementar un servidor de autenticación muy profesional en pocos pasos, y van a ver lo interesante que es para aquellos que no lo conocen. Ese es el desafío para esta sección.

---

## Clase 1 — ¿Por qué seguridad en un proyecto de arquitectura?

Slide: [`clase_01/Por qué seguridad en un proyecto de arquitectura.html`](<clase_01/Por qué seguridad en un proyecto de arquitectura.html>)

> "La seguridad no se agrega al final, se diseña desde el principio."

**Qué vamos a implementar en esta sección:**
- Keycloak como Identity Provider, levantado con Docker Compose.
- Spring Boot como **Resource Server** (valida JWT, no emite ni gestiona contraseñas).
- `SecurityFilterChain` con protección por roles.
- Dos roles — `USER` y `ADMIN` — con endpoints protegidos según quién opera.

**Dos roles, responsabilidades claras:**

| Rol | Responsabilidad |
|---|---|
| `USER` | Opera sus propias cuentas: depositar, retirar, transferir, consultar saldo y extractos |
| `ADMIN` | Todo lo de `USER` + gestión: congelar y activar cuentas, ver dashboard global |

**Qué queda para más adelante:** el concepto de **ownership** — "un `USER` solo puede operar su propia cuenta" — no se resuelve en esta sección. Eso llega con DDD, cuando `Account` tenga un campo `belongsTo(customerId)` contra el cual comparar la identidad del token. Por ahora, la protección es únicamente por rol: cualquier `USER` autenticado puede transferir entre cualquier par de cuentas.

---

## Clase 2 — Mapeo JPA: `@Table`, `@Column` y referencia por Id

Material de referencia: [`clase_02/JPA_annotations.txt`](clase_02/JPA_annotations.txt) → [ObjectDB: JPA Annotations Reference](https://www.objectdb.com/api/java/jpa/annotations). Commit: `13b2d11`.

Hasta esta clase, las entities dependían del mapeo implícito de Hibernate (nombre de tabla y columna deducidos por convención). Se hace explícito ese mapeo con `@Table` y `@Column` en `Account` y `Transaction`:

```java
@Entity
@Table(name = "accounts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 20)
    private String type;   // SAVING, CHECKING

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, CLOSED, FROZEN

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // ...
}
```

Lo mismo en `Transaction`, con `@Table(name = "transactions")` y sus columnas (`type`, `sourceAccountId`, `targetAccountId`, `amount`, `fee`, `status`, `createdAt`).

**El punto de diseño importante:** `Transaction` no tiene una relación `@ManyToOne` hacia `Account` — tiene `sourceAccountId` y `targetAccountId` como `Long` planos, mapeados con `@Column(name = "source_account_id")` / `@Column(name = "target_account_id")`. Es una **referencia entre aggregates por ID**, no una asociación JPA. Cada aggregate (`Account`, `Transaction`) se persiste y se carga de forma independiente — no hay joins implícitos, ni riesgo de traer un grafo de objetos completo por accidente, ni acoplamiento de ciclo de vida entre ambos. Es el mismo principio que después formaliza DDD con los *aggregate roots*.

---

## Clase 3 — Docker: repasando conceptos

![captura de clase](<clase_03/Captura de pantalla 2026-08-08 a la(s) 7.36.11 a. m..png>)
![captura de clase](<clase_03/Captura de pantalla 2026-08-08 a la(s) 7.38.37 a. m..png>)
![captura de clase](<clase_03/Captura de pantalla 2026-08-08 a la(s) 7.46.48 a. m..png>)

Commit: `2565554` (solo capturas, sin cambios de código). Repaso rápido de Docker antes de meter Keycloak en el proyecto:

- **Imagen vs contenedor**: Docker Desktop → pestaña *Images*, mostrando las imágenes ya descargadas (`postgres:16-alpine`, `postgres:17-alpine`, etc.) — la imagen es la plantilla inmutable, el contenedor es la instancia corriendo.
- `docker --version` / `docker compose version` para confirmar que las herramientas están instaladas y qué versión corre.
- `docker run -d --name mi-postgres -p 5433:5432 -e POSTGRES_USER=atlas -e POSTGRES_PASSWORD=atlas123 -e POSTGRES_DB=atlas_bank postgres:17` — levantar un Postgres suelto, a mano, con todos los flags necesarios (nombre, puerto mapeado, variables de entorno, imagen). Funciona, pero ya se nota el problema: hay que recordar todos esos flags cada vez que se quiere volver a levantar el mismo contenedor.

---

## Clase 4 — Docker Compose: la opción

![captura de clase](<clase_04/Captura de pantalla 2026-08-08 a la(s) 7.55.09 a. m..png>)
![captura de clase](<clase_04/Captura de pantalla 2026-08-08 a la(s) 7.56.17 a. m..png>)

Commit: `2cb1e68`. El dolor de `docker run` a mano: `docker container ls` para encontrar el contenedor, `docker container rm -f mi-postgres` para borrarlo, `docker image ls` / `docker rmi <id>` para limpiar la imagen — todo manual, todo por comando suelto, todo para volver a escribir el mismo `docker run` gigante la próxima vez.

**La solución: `docker-compose.yml`.** Se declaran los servicios una sola vez:

```yaml
services:
  mi-postgres:
    image: postgres:17
    ports:
      - "5433:5432"
    environment:
      POSTGRES_USER: atlas
      POSTGRES_PASSWORD: atlas123
      POSTGRES_DB: atlas_bank
  mi-mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: otra_db
```

![captura de clase](<clase_04/Captura de pantalla 2026-08-08 a la(s) 8.06.36 a. m..png>)

`docker compose up -d` levanta **todos** los servicios declarados (acá, Postgres y MySQL a la vez) con un solo comando — nada de flags repetidos a mano.

![captura de clase](<clase_04/Captura de pantalla 2026-08-08 a la(s) 8.08.33 a. m..png>)
![captura de clase](<clase_04/Captura de pantalla 2026-08-08 a la(s) 8.10.57 a. m..png>)

`docker compose ps` y `docker compose logs -f` para inspeccionar el estado y los logs de cada servicio por nombre, y `docker compose down` / `docker compose up -d` para tirar abajo y volver a levantar el stack completo — repetible, versionable (el YAML se commitea) y sin tener que recordar un solo flag.

---

## Clase 5 — Keycloak con Docker Compose — parte 1

![captura de clase](<clase_05/Captura de pantalla 2026-08-08 a la(s) 8.19.37 a. m..png>)

Commit: `a8d43f4`. Se crea un `docker-compose.yml` **separado**, en [`proyecto/docker/`](../proyecto/docker), específico para la infraestructura de seguridad (no se mezcla con la infra de datos de la app):

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    command: start-dev
    ports:
      - "8181:8080"
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin
    depends_on:
      postgres-kc:
        condition: service_healthy

  postgres-kc:
    image: postgres:17
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: keycloak
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U keycloak"]
      interval: 5s
      timeout: 5s
      retries: 5
```

Keycloak tiene su **propia base de datos** (`postgres-kc`), separada de la de `atlas-bank` — otro servicio, otro contenedor, coordinado con `depends_on` + `healthcheck` para que Keycloak no arranque hasta que su Postgres esté listo (`pg_isready`).

![captura de clase](<clase_05/Captura de pantalla 2026-08-08 a la(s) 8.21.08 a. m..png>)
![captura de clase](<clase_05/Captura de pantalla 2026-08-08 a la(s) 8.23.06 a. m..png>)

`docker compose up -d` desde `proyecto/docker/` levanta ambos contenedores; `docker compose ps` confirma `docker-postgres-kc-1` en estado `Healthy` y `docker-keycloak-1` corriendo en el puerto `8181`. El log de arranque de Keycloak muestra el modo dev (`Profile dev activated`, `DO NOT use this configuration in production` — válido para el curso, no para producción real).

![captura de clase](<clase_05/Captura de pantalla 2026-08-08 a la(s) 8.23.47 a. m..png>)

Login del admin console en `http://localhost:8181` (usuario/clave del compose: `admin`/`admin`).

![captura de clase](<clase_05/Captura de pantalla 2026-08-08 a la(s) 8.26.48 a. m..png>)
![captura de clase](<clase_05/Captura de pantalla 2026-08-08 a la(s) 8.28.08 a. m..png>)

Desde el `master realm` (el realm administrativo de Keycloak, no se usa para la app) se crea un **realm nuevo**: `atlas-bank`. Cada realm es un espacio aislado de usuarios, roles y clients — la app va a vivir en el realm `atlas-bank`, no en `master`.

![captura de clase](<clase_05/Captura de pantalla 2026-08-08 a la(s) 8.28.31 a. m..png>)

Ya parado sobre el realm `atlas-bank`, se arranca la creación del **client**: `Client ID = atlas-bank-api`, tipo `OpenID Connect`. El client es la aplicación (`atlas-bank`) que le va a pedir tokens a Keycloak.

---

## Clase 6 — Keycloak con Docker Compose — parte 2

![captura de clase](<clase_06/Captura de pantalla 2026-08-08 a la(s) 8.36.55 a. m..png>)

Commit: `9955f2b`. Se termina de configurar el client `atlas-bank-api`, paso *Capability config*: `Client authentication = On` (client **confidencial**, con client secret — no público) y como *Authentication flow*, `Standard flow` + `Direct access grants` (este último es el que permite pedir un token directo con usuario/contraseña desde Postman, sin pasar por una pantalla de login redirigida — cómodo para probar la API durante el curso).

![captura de clase](<clase_06/Captura de pantalla 2026-08-08 a la(s) 8.42.01 a. m..png>)

Dentro del client `atlas-bank-api`, pestaña **Roles**, se crean los roles `ROLE_ADMIN` y `ROLE_USER`.

> **Punto clave que se paga en la Clase 10:** estos son **roles de client** (viven adentro de `atlas-bank-api → Roles`), no roles de realm (`Realm roles`, en el menú lateral). Esa diferencia determina en qué parte del JWT terminan apareciendo — y es exactamente lo que se rompe más adelante.

![captura de clase](<clase_06/Captura de pantalla 2026-08-08 a la(s) 8.46.32 a. m..png>)

Se crea el usuario `cliente2` (va a representar al rol `USER`) y se le asigna contraseña desde la pestaña *Credentials* (con `Temporary = On` para forzar el cambio en el primer login, comportamiento estándar de Keycloak).

![captura de clase](<clase_06/Captura de pantalla 2026-08-08 a la(s) 8.49.44 a. m..png>)

En el usuario `cliente1` (va a representar al rol `ADMIN`), pestaña *Role mapping*, se le asigna el rol de client `atlas-bank-api → ROLE_ADMIN` (además del `default-roles-atlas-bank` que Keycloak asigna automáticamente a todo usuario nuevo del realm).

Al cierre de esta clase: realm `atlas-bank`, client confidencial `atlas-bank-api`, dos roles de client (`ROLE_ADMIN`, `ROLE_USER`) y dos usuarios de prueba, cada uno con el rol que le corresponde.

---

## Clase 7 — Spring Boot como Resource Server

![captura de clase](<clase_06/Captura de pantalla 2026-08-08 a la(s) 9.13.02 a. m..png>)

Commit: `e57b0a0`. Se agrega la dependencia de Resource Server al `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

Y se apunta al realm de Keycloak en `application.yaml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8181/realms/atlas-bank
```

Con esto, Spring Security sabe **dónde** buscar las claves públicas de Keycloak (`.well-known/openid-configuration` del realm) para validar la firma, la expiración y el emisor de cualquier JWT que llegue en el header `Authorization: Bearer ...` — sin que `atlas-bank` tenga que manejar un solo secreto de firma a mano.

Prueba desde Postman: pestaña *Authorization*, `Auth Type = OAuth 2.0`, `Grant type = Password Credentials`, `Access Token URL = http://localhost:8181/realms/atlas-bank/protocol/openid-connect/token`, con el `Client ID`/`Client Secret` de `atlas-bank-api` (visibles en Keycloak, pestaña *Credentials* del client) y el usuario/contraseña del usuario de prueba (`cliente1`). Postman pide el token contra Keycloak y lo agrega como Bearer en el request a `POST localhost:8082/api/v1/accounts`.

![captura de clase](<clase_06/Captura de pantalla 2026-08-08 a la(s) 9.13.53 a. m..png>)

`atlas-bank-api → Credentials`: acá se copian el `Client ID` y el `Client Secret` que se usan en Postman para pedir el token.

---

## Clase 8 — `SecurityFilterChain`: protección basada en rol 1

Commit: `51d38a2`. Se crea `SecurityConfig`, con el primer `SecurityFilterChain`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        //Accounts
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts/{id}").hasAnyRole("USER","ADMIN")

                        //Transactions
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions/transfer").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions/{id}/transactions").hasAnyRole("USER", "ADMIN")

                        //H2
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
```

Reglas por endpoint: crear y listar cuentas (`POST` / `GET /api/v1/accounts`) solo para `ADMIN`; consultar una cuenta puntual (`GET /api/v1/accounts/{id}`) y las operaciones de transacciones, para `USER` o `ADMIN`. `/h2-console/**` queda abierta (herramienta de desarrollo). El resto de cualquier request (`anyRequest()`) exige, como mínimo, estar autenticado.

Dos detalles de configuración que acompañan siempre a un Resource Server sin sesión:
- `headers.frameOptions(...).disable()` — necesario para que la consola de H2 (que se sirve dentro de un `<iframe>`) no sea bloqueada por el header `X-Frame-Options` que Spring Security agrega por defecto.
- `csrf().disable()` — CSRF protege contra ataques que abusan de **cookies de sesión**; acá no hay sesión ni cookie, cada request se autentica con un Bearer token propio, así que la protección no aplica.

En esta clase el `SecurityFilterChain` ya compila y las rutas ya están declaradas, pero todavía usa `jwt(Customizer.withDefaults())` — sin un converter propio, Spring Security no tiene forma de traducir los roles custom de Keycloak a authorities. Eso es exactamente lo que resuelve la clase siguiente.

---

## Clase 9 — `SecurityFilterChain`: protección basada en rol 2

Commit: `5905f4e`. Se agrega el `JwtAuthenticationConverter` y se lo conecta al `SecurityFilterChain`:

```java
.oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
        )
)
```

```java
private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || realmAccess.get("roles") == null) {
            return List.of();
        }
        var roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
    });
    return converter;
}
```

Por defecto, Spring Security solo sabe interpretar el claim `scope`/`scp` de un JWT (los "scopes" estándar de OAuth2) — no tiene ni idea de qué es `realm_access` ni de los roles custom que emite Keycloak. Este converter reemplaza esa lógica por completo: lee el claim `realm_access.roles` del token y arma un `SimpleGrantedAuthority` por cada rol, para que `hasRole("ADMIN")` / `hasAnyRole("USER", "ADMIN")` (declarados en la Clase 8) tengan algo contra qué comparar.

Con esto el `SecurityFilterChain` queda, en apariencia, completo: rutas protegidas por rol + converter que extrae roles del token. **Pero hay un bug** — y no es evidente hasta que se prueba con un token real. Eso es la Clase 10.

---

## Clase 10 — Probando la seguridad: USER y ADMIN

Commit: `b94d96a` ("fix: bug para la asignación de roles"). Al probar el `SecurityFilterChain` de la Clase 9 con un token real de Keycloak, la protección por rol **no funciona para nadie** — ni `ADMIN` ni `USER` pasan `hasRole("ADMIN")` en `POST /api/v1/accounts`. Decodificando el JWT de `cliente1` (el usuario `ADMIN`) para entender por qué:

```json
{
  "realm_access": {
    "roles": ["default-roles-atlas-bank", "offline_access", "uma_authorization"]
  },
  "resource_access": {
    "account": { "roles": ["manage-account", "manage-account-links", "view-profile"] },
    "atlas-bank-api": { "roles": ["ROLE_ADMIN"] }
  }
}
```

Ahí está el problema: `ROLE_ADMIN` **no vive en `realm_access.roles`** — vive en `resource_access.atlas-bank-api.roles`, porque en la Clase 6 los roles se crearon como **roles de client** (`atlas-bank-api → Roles`), no como roles de realm. El converter de la Clase 9 solo lee `realm_access`, que para cualquier usuario del realm únicamente trae los roles por defecto de Keycloak (`default-roles-atlas-bank`, `offline_access`, `uma_authorization`) — nunca `ROLE_ADMIN` ni `ROLE_USER`. Resultado: ningún `hasRole(...)` coincide nunca, para ningún usuario.

**El fix:** leer el claim correcto — `resource_access.<client-id>.roles` — en vez de `realm_access.roles`:

```java
private static final String CLIENT_ID = "atlas-bank-api";

private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        var resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || resourceAccess.get(CLIENT_ID) == null) {
            return List.of();
        }
        var clientAccess = (Map<String, Object>) resourceAccess.get(CLIENT_ID);
        if (clientAccess.get("roles") == null) {
            return List.of();
        }
        var roles = (List<String>) clientAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());
    });
    return converter;
}
```

Como los roles en Keycloak ya se llaman literalmente `ROLE_ADMIN` / `ROLE_USER` (con el prefijo incluido en el nombre del rol), no hace falta transformarlos en el converter — `hasRole("ADMIN")` compara contra `ROLE_ADMIN` y coincide directo.

**Resultado tras el fix:** `POST /api/v1/accounts` con el token de `cliente1` (`ROLE_ADMIN`) → `201 Created`. El mismo endpoint con el token de `cliente2` (`ROLE_USER`) → `403 Forbidden`.

**Moraleja de la clase:** cuando la protección por rol "no anda" en un Resource Server, el primer paso no es tocar el `SecurityFilterChain` — es decodificar el JWT real y confirmar en qué claim exacto vienen los roles. `realm_access.roles` (roles de realm) y `resource_access.<client>.roles` (roles de client) son dos ubicaciones distintas en Keycloak, y un converter escrito para una no sirve para la otra.

---

## Clase 11 — Seguridad como infraestructura, no como dominio

Slide: [`clase_11/12- Seguridad como infraestructura, no como dominio.html`](<clase_11/12- Seguridad como infraestructura, no como dominio.html>)

> "El dominio no sabe que Keycloak existe."

**¿Dónde vive cada cosa?**

| Responsabilidad | Ubicación | ¿Toca el dominio? |
|---|---|---|
| Validar tokens JWT | `SecurityConfig` | ❌ No |
| Proteger endpoints por rol | `SecurityConfig` | ❌ No |
| Extraer roles del token | `JwtAuthenticationConverter` | ❌ No |
| Validar datos del request | Bean Validation (DTOs) | ❌ No |
| Validar reglas de negocio | Services / Entities | ✅ Sí |

**¿Por qué importa?**
- Si mañana se cambia Keycloak por otro Identity Provider, el dominio no cambia una sola línea.
- Si los endpoints cambian de ruta, los services ni se enteran.
- Los tests de dominio no necesitan levantar Spring Security ni un Keycloak de prueba.
- Cuando el curso llegue a **Arquitectura Hexagonal**, `SecurityConfig` va a encajar directamente como un *adapter* de infraestructura — no va a hacer falta reescribirlo.

**Lo que se construyó en esta sección:** mapeo JPA explícito con `@Table`/`@Column`, Docker y Docker Compose desde cero, Keycloak con realm/client/roles/usuarios, Spring Boot como Resource Server, `SecurityFilterChain` con protección por rol, y el converter de roles de Keycloak — con seguridad viviendo, de punta a punta, en infraestructura.

**Lo que queda pendiente, a propósito:**
- **Ownership** ("un `USER` solo opera su propia cuenta") → llega con DDD.
- `SecurityConfig` formalizado como *adapter* → Sección 8.
- Tests automatizados de seguridad → Sección 10.

Con esto se cierra la Sección 3: `atlas-bank` delega autenticación y autorización a Keycloak, valida JWT como Resource Server, protege cada endpoint por rol, y el dominio permanece completamente ajeno a cómo se implementa esa seguridad.
