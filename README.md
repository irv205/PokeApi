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
- Room 2.8.5 · Coil 3.3.0
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

## Base de datos

Room guarda el esquema exportado en `app/schemas/com.irv205.pokeapi.data.local.PokeDatabase/1.json`. La tabla `favorite_pokemon` almacena ID, nombre, imagen y tipo principal (nullable, para permitir favoritos desde el listado antes de conocer el detalle).

## Licencia

Proyecto creado como reto técnico. PokeAPI y los sprites pertenecen a sus respectivos autores.
