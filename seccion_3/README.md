# Sección 3: Seguridad con Keycloak
	Porque un proyecto de Arquitectura necesita seguridad
	Mapeo JPA profesional: @Table, @Column y referencia entre aggregates por ID
	Docker lo justo: imagen vs contenedor, dolor de docker run, Docker Compose como solución
	Keycloak con Docker Compose: real, client, roles y usuarios
	Spring Boot como Resource Server: JWT y issuer-uri
	SecurityFilterChain con roles y converter de Keycloak (realm_access.roles)
	La seguridad vive en infraestructura, el dominio no sabe que Keycloak existe.
### Los temas son los siguientes:
	Clase_00: Introducción
	Clase_01: ¿Por qué seguridad en un proyecto de arquitectura?
	Clase_02: Mapeo JPA: @Table, @Column y referencia por Id
	Clase_03: Docker: repasando conceptos
	Clase_04: Docker compose: La opción
	Clase_05: Keycloak con Docker compose – parte 1
	Clase_06: Keycloak con Docker compose – parte 2
	Clase_07: Spring boot como Resource Server
	Clase_08: SecurityFilterChain: protección basada en rol 1
	Clase_09: SecurityFilterChain: protección basada en rol 2
	Clase_10: Probando la seguridad: USER y ADMIN
	Clase_11: Seguridad como infraestructura, no como dominio 

## Clase 0: Introducción
Bienvenido Equipo, la sección de la seguridad, es el punto central de esta sección. ¿Qué vamos a hacer? Vamos a empezar a diagramar la seguridad de nuestra API ¿Por qué vamos a comenzar? Porque no vamos a terminar, es decir, vamos a comenzar acá, y seguramente vamos a ir resolviendo a medida que avanza el curso diferentes etapas de seguridad, pero lo vamos a comenzar en este punto. No es una buena estrategia dejar para lo último la seguridad, porque cuanto más lejos dejemos la seguridad del inicio de nuestro proyecto más vamos a tener que refactorizar y sabemos que refactorizar es un dolor de cabeza, entonces esa es nuestra estrategia comenzar, vamos a resolver algunos problemas de seguridad. Si hacemos un poco de historia nosotros podemos decir que existe tres tipos de seguridad: autenticación básica con el Authorization, con el user y el password encriptado, la segunda estrategia, donde tenemos más control donde tenemos nuestro propio filtro y trabajamos con JWT (Jason Web Token) es decir nosotros diagramamos ese token, pero ese sabemos que requiere de bastante código y bastante costo, pero tenemos control total sobre esa problemática y también nos ahorramos de trabajar con un servidor de autenticación. Y el tercero, el que vamos a ver acá más profesional, es el trabajar con un servidor de autenticación que en este caso es Keycloak.
¿Qué vamos a trabajar? Vamos a trabajar con roles, y van a ver con una serie de pasos muy breves vamos a resolver el tema de la seguridad super sencillo. Y eso sabemos que es un capital porque resolver algo de forma sencilla de forma rápida y de forma muy prolijea y profesional nos ahorraremos bastante tiempo para resolver otras problemáticas de nuestra aplicación, es decir vamos a delegar la seguridad a Keycloak básicamente, pero también en esta sección vamos a aprovechar a hacer un repaso muy pequeño muy breve, vamos a hablar un poco de Docker, la diferencia que existe entre docker y docker compose y de la forma que tenemos de levantar algunos servicios utilizando estas herramientas, lo vamos a hacer porque, porque el servidor Keycloak lo vamos a levantar en un servidor de docker, entonces lo necesitamos, necesitamos conocer y si no recordamos, porque hace rato que no utilizamos docker, bueno esta sección está orientado a estos estudiantes. Así que ese es el desafió, comenzar con la seguridad, implementar un servidor de autenticación muy profesional, en pocos pasos y van a ver lo  interesante que es, aquellos que no lo conocen, y también nosotros vamos a resolver el tema de la seguridad o vamos a comenzar a desarrollarlo de una forma muy sencilla, así que ese es el desafió para esta sección

## Clase 01: ¿Por qué seguridad en un proyecto de arquitectura?