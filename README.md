# PokeCompose

Aplicación Android del reto **PokeCompose App**, construida con Kotlin y Jetpack Compose sobre la [PokeAPI](https://pokeapi.co/). Implementa Clean Architecture, MVVM con UDF (`StateFlow`), Hilt, Retrofit, Room, Coil, navegación type-safe y Material 3 con temas claro/oscuro.

## Características

| Pantalla | Descripción |
| --- | --- |
| **Pokédex** | Listado en tarjetas con sprite oficial, nombre e ID. Paginación infinita (`limit=20`), búsqueda en tiempo real por nombre o ID y estados Loading / Success / Error con reintento. |
| **Detalle** | Cabecera con degradado según el tipo principal, chips de tipos, botón de favorito y pestañas **About**, **Stats** y **Evolución**. Las barras de estadísticas se animan al aparecer. |
| **Cadena de evolución** | Vista dedicada con las etapas de evolución agrupadas y navegación al detalle al tocar cualquier Pokémon, tanto desde la pestaña como desde la pantalla completa. |
| **Mi equipo (Favoritos)** | Persistencia local con Room, eliminación por *swipe to dismiss* y por botón, además de un *empty state* amigable. |
| **Quiz** | Reto de 5 rondas con siluetas (imagen teñida), 4 opciones por ronda, temporizador de 15 segundos, conteo de aciertos y pantalla final con opción de volver a jugar. |

Extras de UX: navegación inferior entre Pokédex / Mi equipo / Quiz, transiciones animadas entre destinos, `AnimatedContent` para pestañas y fases del quiz, y `AnimatedVisibility` para revelar respuestas.

## Arquitectura

```
app/src/main/java/com/irv205/pokeapi/
├── core/
│   ├── common/        Resource<T>, URLs de sprites, helpers de nombres
│   ├── di/            Módulos de Hilt (red, base de datos, repos, dispatchers)
│   └── network/       safeApiCall: manejo centralizado de errores
├── data/
│   ├── local/         Entidad, DAO y base de datos Room
│   ├── mapper/        DTO → modelos de dominio
│   ├── mock/          Motor de mocks, fixtures de PokeAPI y overrides de debug
│   ├── remote/        PokeApiService (Retrofit) y DTOs
│   └── repository/    Implementaciones de los repositorios
├── domain/
│   ├── model/         Modelos puros de negocio
│   ├── repository/    Contratos
│   └── usecase/       Casos de uso
├── presentation/
│   ├── components/    UI compartida (tarjetas, chips, barras, evolución)
│   ├── navigation/    Rutas @Serializable y NavHost
│   ├── pokedex/       Pantalla + ViewModel
│   ├── detail/        Pantalla + ViewModel
│   ├── evolution/     Pantalla + ViewModel
│   ├── favorites/     Pantalla + ViewModel
│   ├── mock/          Pantalla debug para activar mocks y elegir escenarios
│   └── quiz/          Pantalla + ViewModel
└── ui/theme/          Paleta, tipografía y colores por tipo
```

- **Separación estricta:** la capa `presentation` solo conoce modelos y casos de uso de `domain`; `data` implementa los contratos y mapea DTOs.
- **Estado:** cada ViewModel expone un único `StateFlow<UiState>` inmutable; la UI solo emite eventos hacia el ViewModel.
- **Errores:** todas las llamadas de red pasan por `safeApiCall`, que devuelve `Resource.Success`, `Resource.Error` o `Resource.Loading`, distingue errores HTTP, red y serialización, y re-lanza `CancellationException`.
- **Detalle eficiente:** `/pokemon/{id}` y `/pokemon-species/{id}` se solicitan en paralelo. Los sprites de la lista se construyen con el patrón de PokeAPI sprites para evitar una llamada de detalle por Pokémon.
- **Quiz eficiente:** el pool de candidatos se obtiene con una sola petición (`/pokemon?limit=1025`).

## Stack

Versiones fijadas en `gradle/libs.versions.toml`:

- Kotlin 2.2.10 · AGP 9.1.1 · KSP 2.3.6
- Compose BOM 2026.02.01 · Material 3 · Navigation Compose 2.9.8 (type-safe)
- Hilt 2.60.1 · Retrofit 3.0.0 · OkHttp 5.5.0 · kotlinx.serialization
- Room 2.8.5 · Coil 3.3.0 · MockWebServer 5.5.0
- JUnit · MockK 1.14.11 · Turbine 1.2.1 · Compose UI Test

> `compileSdk` es 37.1 y `targetSdk` 36 porque las dependencias actuales lo requieren. Hilt 2.60.1 es necesario con AGP 9 (2.57.2 falla con `Android BaseExtension not found`).

## Requisitos

- Android Studio reciente (AGP 9) o Gradle 9.3.1.
- JDK 21; el proyecto lo aprovisiona automáticamente vía `gradle/gradle-daemon-jvm.properties`.
- Android SDK con la plataforma 37.1 instalada.
- Emulador o dispositivo con Android 7.0 (API 24) o superior.

## Ejecutar

```bash
# Compilar e instalar en el dispositivo/emulador conectado
./gradlew :app:installDebug

# Abrir la app
adb shell am start -n com.irv205.pokeapi/.MainActivity
```

También puedes abrir el proyecto en Android Studio y ejecutar la configuración `app`.

## Mocks de red

El proyecto incluye el motor portable de `MockInterceptorLibrary.md`, adaptado a la PokeAPI. En entorno `MOCK` el interceptor resuelve la respuesta localmente (método + path + query/body) sin llamar al servidor; en `PROD` es un no-op que delega en OkHttp.

Archivos principales:

- `data/mock/MockInterceptor.kt`: cortocircuita la request y añade `x-mocked`, `x-mock-service` y `x-mock-scenario`.
- `data/mock/MockRequestMatcher.kt`: matching por método, path exacto, plantilla `{id}`, regex, query y body.
- `data/mock/MockService.kt` / `MockScenario.kt`: modelos serializables para importar/exportar configuración.
- `data/mock/PokeMockFixtures.kt`: fixtures reales de la API (24 Pokémon, dos páginas, pool del quiz, detalle, especie y evolución).
- `data/mock/DebugMockOverrides.kt`: selección de entorno e importación de JSON por `Intent` solo en debug.
- `data/networklog/`: registro en memoria de las llamadas OkHttp y el interceptor que las captura.
- `core/di/MockModule.kt` / `NetworkModule.kt`: providers de Hilt conectados al `OkHttpClient`.

### Pantalla de control de mocks (solo debug)

En builds **debug**, la cabecera de la Pokédex muestra un icono de ajustes a la derecha. Al pulsarlo se abre la pantalla **Mocks de red**, organizada en dos pestañas:

**Configuración**

- Activar o desactivar `MOCK` con el interruptor **Usar mocks locales**.
- Activar el **fallback a la red** para QA manual.
- Habilitar o deshabilitar cada servicio.
- Elegir la respuesta activa de cada servicio (happy path, error 500, 404, etc.).
- Abrir el detalle de un servicio tocando **N respuestas**.
- **Portabilidad**: `Importar JSON` pega una configuración `List<MockService>` y reemplaza la actual; `Exportar JSON` genera el JSON completo (pretty, con defaults) para copiarlo y reutilizarlo.
- Restablecer el catálogo original de fixtures.
- Pulsar **Aplicar y recargar Pokédex** para volver y volver a cargar el listado con la nueva configuración.

**Detalle del servicio**

Cada servicio se puede editar por completo (nombre, método, path pattern y habilitado) y admite múltiples respuestas. La lista muestra todas las respuestas con su status, delay, content-type y filtros de request; el radio button marca la respuesta activa que se usa como fallback.

- **Agregar respuesta** abre un editor a pantalla completa con nombre, status HTTP, delay, content-type, filtros de request y body de respuesta (JSON/texto pegable).
- **Editar** modifica una respuesta existente; **Eliminar** pide confirmación y, si era la activa, selecciona automáticamente la primera restante.
- Los filtros de request son opcionales. El checkbox **Filtrar por query params** habilita/deshabilita ese filtro; un escenario con `requestQuery` y/o `requestBody` solo se usa cuando todos sus pares coinciden. Los escenarios con filtros tienen prioridad sobre la respuesta activa sin filtros.
- Se pueden definir varias respuestas con el mismo status pero distinto mensaje (por ejemplo, dos `400` con cuerpos de error diferentes).
- **Eliminar servicio** borra el servicio y todas sus respuestas tras confirmar.

> La configuración de mocks vive en memoria: los cambios se mantienen mientras el proceso esté vivo, pero se pierden al reiniciar la app. Los fixtures originales se pueden recuperar con **Restablecer fixtures**.

**Llamadas**

- Lista las llamadas capturadas por `NetworkLoggingInterceptor`, de la más reciente a la más antigua (máximo 100, solo en memoria).
- Cada tarjeta resume método, endpoint, estado HTTP, origen (`MOCK`/`RED`), duración, hora y servicio/escenario mock.
- Al tocar una tarjeta se expande y muestra URL completa, query, request headers/body, status, response headers/body (JSON formateado) y el error si la llamada falló.
- El botón **Limpiar** vacía el registro.

El registro no se escribe a disco y solo se instala en debug, por lo que en release no existe ni la pantalla ni la captura de llamadas.

### Ejecutar contra mocks

```bash
# Compilar el debug usando los fixtures locales por defecto
./gradlew :app:installDebug -PapiEnvironment=MOCK

# O arrancar una build normal en modo MOCK
adb shell am start -n com.irv205.pokeapi/.MainActivity \
  --es api_environment mock

# Volver a la API real
adb shell am start -n com.irv205.pokeapi/.MainActivity \
  --es api_environment prod
```

El valor por defecto de debug es `PROD`; release siempre fuerza `PROD` y los extras de debug se ignoran.

### Importar/exportar para multi-módulo

La configuración usa un formato portable: `List<MockService>`, el mismo que consumen `DebugMockOverrides.EXTRA_MOCK_CONFIG_JSON` y la UI de debug. Esto permite versionar fixtures como archivos JSON, compartirlos entre módulos/builds, o cargarlos por `adb` en pruebas E2E.

- **Exportar** (UI o `MockConfigViewModel.exportConfigJson()`): incluye todos los campos por defecto para que el JSON sea auto-contenido.
- **Importar** (UI o `MockConfigViewModel.importConfigJson(raw)` / extra `mock_config_json`): valida el JSON y reemplaza los servicios actuales; ignora claves desconocidas para tolerar evolución del esquema.

### Configuración estricta y JSON propio

En `MOCK` el modo es estricto (`fallbackToNetwork = false`): si ninguna request casa, se lanza `IOException` para que el fallo sea visible en tests. Para QA manual se puede permitir el fallback:

```bash
adb shell am start -n com.irv205.pokeapi/.MainActivity \
  --es api_environment mock \
  --ez mock_fallback_to_network true
```

También se puede reemplazar la configuración completa con un JSON con la forma `List<MockService>`:

```bash
adb shell am start -n com.irv205.pokeapi/.MainActivity \
  --es api_environment mock \
  --es mock_config_json "$(cat fixtures/mocks.json)"
```

Cada escenario permite `statusCode`, `contentType`, `requestQuery`, `requestBody`, `responseHeaders`, `responseBody` y `delayMs`. Para elegir un error concreto basta con cambiar `selectedScenarioId` del servicio (por ejemplo `server_error` en `pokemon_page`).

> Coil mantiene su propio cliente para las imágenes, por lo que los sprites siguen descargándose de la red y no pasan por el interceptor. El mock cubre exclusivamente las llamadas de PokeAPI usadas por la capa `data`.


## Pruebas

```bash
# Pruebas unitarias del ViewModel (MockK + Turbine)
./gradlew :app:testDebugUnitTest

# Compilar las pruebas instrumentadas
./gradlew :app:assembleDebugAndroidTest

# Pruebas de UI en un emulador/dispositivo conectado
./gradlew :app:connectedDebugAndroidTest
```

Cobertura incluida:

- `PokedexViewModelTest`: éxito de primera página, búsqueda por nombre/ID y error con reintento, usando `MainDispatcherRule` (Turbine + MockK).
- `PokedexScreenTest`: render de tarjeta y navegación al pulsar, y flujo de búsqueda con limpieza, usando `createComposeRule`.
- `MockConfigScreenTest`: la pantalla de mocks alterna `PROD/MOCK`, el botón de aplicar dispara la recarga y la pestaña de llamadas muestra la tarjeta resumida y su detalle expandido.
- `MockServiceDetailScreenTest`: detalle del servicio, edición del servicio, alta de una respuesta `400` personalizada, selección de respuesta activa, borrado de respuesta con confirmación y borrado del servicio.
- `InMemoryMockConfigProviderTest`: actualización de servicios, alta/edición/borrado de respuestas, selección automática de la respuesta activa y borrado de servicios.
- `MockServiceDetailViewModelTest`: estado del detalle, edición del servicio, normalización de respuestas, selección y borrado de respuestas, y borrado del servicio.
- `NetworkCallRecorderTest`: orden más reciente primero, límite de llamadas y limpieza del registro.
- `NetworkLoggingInterceptorTest`: captura método, endpoint, status, headers y body de llamadas reales, mockeadas y con error, usando `MockWebServer`.
- `MockRequestMatcherTest`: path exacto, plantilla, regex, método, query, body y fallback al escenario seleccionado.
- `MockInterceptorTest`: página 1/2, pool del quiz, detalle, especie, evolución, error 500, modo `PROD`, modo estricto y fallback a red con `MockWebServer`.
- `MockServiceSerializationTest`: importación/exportación del JSON de configuración y del catálogo completo de fixtures.
- `MockConfigViewModelTest`: cambio de entorno, fallback, activación/desactivación de servicios, selección de escenario, restauración de fixtures, exposición/limpieza de llamadas de red e importación/exportación JSON portable.

## Base de datos

Room guarda el esquema exportado en `app/schemas/com.irv205.pokeapi.data.local.PokeDatabase/1.json`. La tabla `favorite_pokemon` almacena ID, nombre, imagen y tipo principal (nullable, para permitir favoritos desde el listado antes de conocer el detalle).

## Licencia

Proyecto creado como reto técnico. PokeAPI y los sprites pertenecen a sus respectivos autores.
