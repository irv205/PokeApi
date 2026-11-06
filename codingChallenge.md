# 📱 Coding Challenge: PokeCompose App (Android)

Este documento detalla las instrucciones completas para desarrollar el reto técnico **PokeCompose App**, una aplicación móvil en Android desarrollada con **Kotlin** y **Jetpack Compose** que consume la [PokeAPI](https://pokeapi.co/).

---

## 🎯 Objetivo del Reto
Construir una aplicación robusta, modular y escalable aplicando los estándares modernos de desarrollo Android (Clean Architecture, MVVM, Jetpack Compose, Hilt y pruebas automatizadas).

---

## 🗺️ Pantallas y Flujos Requeridos (Al menos 5)

### 1. Pantalla de Listado Principal (Pokédex)
* **Endpoint:** `/pokemon?limit=20&offset=0` (Paginación infinita).
* **Requisitos:**
    * Mostrar tarjetas con el nombre, sprite oficial e ID del Pokémon.
    * Barra de búsqueda superior para filtrado en tiempo real.
    * Estados de UI: `Loading`, `Success`, `Error` con opción de reintentar.

### 2. Pantalla de Detalle del Pokémon
* **Endpoints:** `/pokemon/{id}` y `/pokemon-species/{id}`.
* **Requisitos:**
    * Cabecera dinámica con colores según el tipo principal del Pokémon.
    * Estadísticas base (HP, Attack, Defense, etc.) con barras de progreso animadas.
    * Pestañas (*Tabs*) informativas: *About* (altura, peso, descripción), *Stats* y *Evolution*.

### 3. Pantalla de Cadena de Evolución
* **Endpoint:** Derivado de `pokemon-species` -> `evolution_chain`.
* **Requisitos:**
    * Visualización secuencial de las evoluciones (Ej: *Bulbasaur $\rightarrow$ Ivysaur $\rightarrow$ Venusaur*).
    * Interactividad: al hacer clic en cualquier sprite o nombre, debe navegar al detalle de ese Pokémon.

### 4. Pantalla de Favoritos / Mi Equipo (Room)
* **Requisitos:**
    * Persistencia local usando **Room**.
    * Listado de Pokémon guardados por el usuario.
    * Funcionalidad de eliminación mediante *Swipe to dismiss* o botón dedicado.
    * Estado vacío (*Empty State*) amigable.

### 5. Pantalla de Minijuego / "Guess that Pokémon" (Quiz)
* **Requisitos:**
    * Lógica en el ViewModel para seleccionar un Pokémon aleatorio y generar 3 opciones incorrectas.
    * Mostrar la silueta del Pokémon (imagen con filtro de color oscuro/sombra).
    * Conteo de aciertos, temporizador y pantalla final de resultados al completar 5 rondas.

---

## ⚙️ Requisitos Técnicos y Arquitectura

* **Arquitectura:** Clean Architecture con separación estricta en capas (`data`, `domain`, `presentation`).
* **Patrón de UI:** MVVM con Unidirectional Data Flow (UDF) mediante `StateFlow`.
* **Inyección de Dependencias:** **Hilt**.
* **Red:** **Retrofit** + **OkHttp** con manejo de errores centralizado.
* **Carga de Imágenes:** **Coil**.
* **Navegación:** **Navigation Compose** (Type-safe navigation).
* **Diseño:** **Material 3** con soporte para temas claro/oscuro.

---

## 🧪 Criterios de Evaluación y Buenas Prácticas
1. **Manejo de Errores:** Uso de clases selladas (`Result<T>` o `Resource<T>`) para la gestión de estados de red.
2. **Pruebas (Testing):**
    * Al menos una prueba unitaria para el `ViewModel` utilizando *MockK* y *Turbine*.
    * Una prueba de UI con *Compose JUnit Test*.
3. **Animaciones:** Transiciones fluidas entre pantallas o uso de `AnimatedVisibility`.