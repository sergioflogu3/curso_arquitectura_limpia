# Sección 2 – arquitectura en capas con criterio
	Porque el entity no debe salir del service: DTOs de request y response
	Mapeo manual vs MapStruct
	Excepciones de dominio con ProblemDetail (RFC 7807)
	@RestControllerAdvice centralizado
	Bean Validation en DTOs: constraints estándar y mensajes custom
	Validaciones custom de negocio con @Constraint
## Las clases son las siguientes:
	Clase_0: Introducción 
	Clase_1: Anatomía de un monolito bien hecho
	Clase_2: DTOs – separación entre lo que se expone y lo que se persiste
	Clase_3: DTOs – request, response y @Data
	Clase_4: Mapeo de DTOs: MapStruct 1
	Clase_5: Mapeo de DTOs: MapStruct 2
	Clase_6: ProblemDetail: errores con estándar RFC 7807
	Clase_7: @RestControllerAdvice centralizado
	Clase_8: Bean validation en DTos
	Clase_9: Validaciones de custom de petición – parte 1
	Clase_10: Validaciones de custom de petición – parte 2

## Clase 0 – Introducción
Todas las secciones van a aportar soluciones y mejoras a nuestro proyecto. En la primera sección creamos un proyecto con deficiencias aplicamos SOLID y resolvimos algunos problemas, pero todavía queda mucho camino por recorrer. En esta sección le va a tocar a otro tipo de problemas: manejo ideal de excepciones, DTOs, y también mapeo con MapStruct. Bien ¿Qué vamos a hacer? Nosotros estamos exponiendo a la Entity, y sabemos que eso no es bueno, sabemos que estamos exponiendo la estructura de nuestras bases de datos y es ahí donde aparece el patrón DTO, pero cuando aparece el patrón DTO tengo que mapear, es decir, tengo que transformar de un DTO a una Entity y viceversa, entonces ahí es donde aparece el concepto de mapeo, el concepto de mapeo lo vamos a manejar con MapStruct y vamos a llegar a esa dependencia y la vamos a configurar y la vamos a utilizar y verán que interesante es MapStruct es probable que ya lo conozcan, porque para este curso es necesario algunos conocimientos básicos y talvez incluyan MapStruct. Entonces DTO, Mapeo, y también vamos a utilizar, un poquito mejor de lo que estábamos haciendo Lombok, es decir vamos a saber cuándo utilizar Data, setters y getters separado, vamos a aprender cosas separado en referencia a eso. Y también otro de los temas importantes que vamos a tener vamos a utilizar un decorador personalizado para manejar errores, es decir podemos tener tres tipos de errores, el request: que viene del dato que validamos con BeanValidation, pero hay algunos errores que no podemos hacerlo con BeanValidation lo tenemos que hacer con lógica del negocio o lo tenemos que hacer creando algún decorador personalizado, pero claro nosotros tenemos el DTO, mapeamos, hacemos un montón de cosas ocurren errores, capturamos esos errores, pero ¿Cómo los capturamos? Entonces ahí es donde aparece RestControllerAdvice el manejo global de excepciones, que va a hacer uno de los temas centrales de esta sección, entonces fíjense todos los temas importantes que van a hacer que al final de esta sección nosotros ya tengamos un proyecto con ciertas características importantes, ya aplicadas importantes, a todo lo que tiene que ver a nuestro atlas-banck y la mejora que prometimos que vamos a hacer a lo largo de todo el curso. Así que esta es la propuesta para la sección número 2: BeanValidation, Generar Decoradores, DTOs, manejo global de excepciones. Llegamos al final de esta introducción esta es la propuesta 

## Clase 1 - Anatomia de un monolito bien hecho
