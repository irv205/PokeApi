# Scripts y comandos — PokeCompose

Guía rápida para ejecutar el proyecto, las pruebas y los flujos E2E desde la **terminal de Android Studio**.

> Abre la terminal con **View → Tool Windows → Terminal** y asegúrate de estar en la raíz del proyecto (`PokeApi/`). Todos los comandos asumen esa ubicación.

---

## 0. Preparación de la terminal (una vez por sesión)

```bash
# Ir a la raíz del repo
cd "$(git rev-parse --show-toplevel)"

# Herramientas locales de esta máquina
export ADB="$HOME/Library/Android/sdk/platform-tools/adb"
export MAESTRO="$HOME/.maestro/bin/maestro"

# Verificar que todo responde
"$ADB" devices
"$MAESTRO" --version
./gradlew --version

# Dar permisos de ejecución si hace falta
chmod +x ./gradlew ./e2e/run.sh
```

Opcional: deja `adb` y `maestro` en el `PATH` de tu shell.

```bash
echo 'export PATH="$HOME/Library/Android/sdk/platform-tools:$HOME/.maestro/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

---

## 1. Runner interactivo (recomendado)

Un solo comando para elegir qué tests correr y generar un reporte HTML consolidado:

```bash
./run-tests.sh
```

Menú:

```text
1) Todos los tests (unit + instrumentados + E2E)
2) Solo unit tests
3) Test automation (E2E Maestro)
0) Salir
```

Al terminar genera:

```text
build/reports/test-runner/index.html
```

Ese reporte incluye el estado de cada suite, el resumen de unit tests por clase y los enlaces a los reportes HTML de Gradle y de Maestro.

```bash
# Abrir el reporte automáticamente al terminar (macOS)
OPEN_REPORT=1 ./run-tests.sh
```

> Si eliges una opción que requiere dispositivo y no hay ninguno conectado, el runner omite esas suites y lo deja indicado en el reporte.

---

## 2. Emulador o dispositivo

Con Android Studio también puedes usar **Device Manager**, pero desde terminal:

```bash
# Ver dispositivos conectados
"$ADB" devices

# Ver emuladores (AVD) disponibles
"$HOME/Library/Android/sdk/emulator/emulator" -list-avds

# Arrancar el AVD Pixel_9
"$HOME/Library/Android/sdk/emulator/emulator" -avd Pixel_9 &

# Esperar a que termine de arrancar
"$ADB" wait-for-device
"$ADB" shell getprop sys.boot_completed   # debe devolver 1
```

Si `adb devices` muestra `offline` o `unauthorized`:

```bash
"$ADB" kill-server
"$ADB" start-server
"$ADB" devices
```

---

## 3. Compilar e instalar

```bash
# Compilar APK debug
./gradlew :app:assembleDebug

# Instalar en el dispositivo/emulador conectado
./gradlew :app:installDebug

# Instalar una build debug que use mocks locales por defecto
./gradlew :app:installDebug -PapiEnvironment=MOCK

# Abrir la app
"$ADB" shell am start -n com.irv205.pokeapi/.MainActivity
```

APK generado: `app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Abrir la app con mocks (solo debug)

```bash
# Modo MOCK con los fixtures locales
"$ADB" shell am start -n com.irv205.pokeapi/.MainActivity \
  --es api_environment mock

# Modo PROD (API real)
"$ADB" shell am start -n com.irv205.pokeapi/.MainActivity \
  --es api_environment prod

# MOCK permitiendo fallback a la red (QA manual)
"$ADB" shell am start -n com.irv205.pokeapi/.MainActivity \
  --es api_environment mock \
  --ez mock_fallback_to_network true

# MOCK con un fixture E2E en base64
FIXTURE=e2e/fixtures/pokemon_page_happy.json
B64="$(python3 -c 'import base64,sys; print(base64.b64encode(open(sys.argv[1],"rb").read()).decode())' "$FIXTURE")"

"$ADB" shell am force-stop com.irv205.pokeapi
"$ADB" shell am start -W -n com.irv205.pokeapi/.MainActivity \
  --es api_environment mock \
  --es mock_config_json_base64 "$B64"
```

Fixtures disponibles:

- `e2e/fixtures/pokemon_page_happy.json`
- `e2e/fixtures/pokemon_page_custom_400.json`

---

## 5. Tests unitarios (JVM)

```bash
# Todos los tests unitarios debug
./gradlew :app:testDebugUnitTest

# Una clase concreta
./gradlew :app:testDebugUnitTest \
  --tests "com.irv205.pokeapi.presentation.pokedex.PokedexViewModelTest"

# Un método concreto
./gradlew :app:testDebugUnitTest \
  --tests "com.irv205.pokeapi.data.mock.MockInterceptorTest.*"

# Forzar la re-ejecución sin caché
./gradlew :app:testDebugUnitTest --rerun-tasks
```

Reporte HTML: `app/build/reports/tests/testDebugUnitTest/index.html`

---

## 6. Tests instrumentados y de UI

Requieren emulador o dispositivo conectado.

```bash
# Solo compilar los tests instrumentados
./gradlew :app:assembleDebugAndroidTest

# Ejecutar todos los tests de UI en el dispositivo
./gradlew :app:connectedDebugAndroidTest

# Ejecutar una clase concreta
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.irv205.pokeapi.presentation.pokedex.PokedexScreenTest
```

Reporte HTML: `app/build/reports/androidTests/connected/debug/index.html`

Clases incluidas:

- `PokedexScreenTest`
- `MockConfigScreenTest`
- `MockServiceDetailScreenTest`

---

## 7. E2E con Maestro

```bash
# Todos los flujos (instala el APK debug y ejecuta Maestro)
./e2e/run.sh

# Flujo específico
./e2e/run.sh happy       # Pokédex carga con fixture OK
./e2e/run.sh error400    # 400 custom → estado de error + Reintentar
./e2e/run.sh config      # smoke de la pantalla "Mocks de red"

# Saltar la instalación si el APK ya está actualizado
SKIP_INSTALL=1 ./e2e/run.sh happy

# Pasar rutas explícitas de adb/maestro
ADB="$ADB" MAESTRO="$MAESTRO" ./e2e/run.sh
```

>`run.sh` detecta automáticamente `adb` en `$HOME/Library/Android/sdk/platform-tools/adb` y `maestro` en `$HOME/.maestro/bin/maestro`.

---

## 8. Logs y limpieza

```bash
# Limpiar logcat y seguir los logs de la app
"$ADB" logcat -c
"$ADB" logcat | grep -iE "pokeapi|okhttp|AndroidRuntime"

# Solo crashes
"$ADB" logcat -s AndroidRuntime:E

# Detener la app
"$ADB" shell am force-stop com.irv205.pokeapi

# Borrar datos locales (favoritos y estado en memoria)
"$ADB" shell pm clear com.irv205.pokeapi

# Desinstalar
"$ADB" uninstall com.irv205.pokeapi
```

---

## 9. Calidad y diagnóstico de build

```bash
# Lint de debug
./gradlew :app:lintDebug

# Limpiar artefactos
./gradlew clean

# Compilar con traza completa si falla
./gradlew :app:assembleDebug --stacktrace

# Ver dependencias
./gradlew :app:dependencies

# Listar tareas disponibles
./gradlew :app:tasks
```

---

## 10. Flujo recomendado antes de un commit / PR

```bash
# 1. Pruebas unitarias + compilación de instrumentados + lint
./gradlew :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug

# 2. Con emulador/dispositivo conectado
./gradlew :app:connectedDebugAndroidTest

# 3. E2E completo
./e2e/run.sh
```

---

## 11. Problemas comunes

| Síntoma | Solución |
| --- | --- |
| `adb: command not found` | Usa `"$ADB"` o añade `platform-tools` al `PATH`. |
| `maestro: command not found` | Usa `"$MAESTRO"` o añade `~/.maestro/bin` al `PATH`. |
| `No Android device/emulator connected` | Arranca el AVD o conecta un dispositivo con depuración USB. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | `"$ADB" uninstall com.irv205.pokeapi` y vuelve a instalar. |
| El mock no se aplica | Verifica que sea build **debug** y que `api_environment=mock`. Revisa `logcat`. |
| Gradle usa un JDK incorrecto | `./gradlew --version`; el proyecto aprovisiona JDK 21 con `gradle/gradle-daemon-jvm.properties`. |
| Un test E2E no encuentra un elemento | Ejecuta primero `./e2e/run.sh <flujo>` sin `SKIP_INSTALL` y revisa los logs. |
