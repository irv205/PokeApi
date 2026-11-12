# E2E test automation (black-box)

Automatización E2E lista para ejecutar con **Maestro** + fixtures de mocks en JSON (formato portable `List<MockService>`), usando los overrides de debug por intent.

## Requisitos
- Android SDK + `adb` (o `ADB=/ruta/a/adb`)
- Dispositivo/emulador conectado (`adb devices`)
- [Maestro](https://maestro.mobile.dev/) instalado (`maestro` en PATH o `MAESTRO=/ruta/a/maestro`)
- Build debug instalable (`./gradlew :app:installDebug`)

## Ejecutar

```bash
# Todos los flujos (instala debug, lanza con fixtures y corre Maestro)
./e2e/run.sh

# Un flujo específico
./e2e/run.sh happy      # lista OK con fixture feliz
./e2e/run.sh error400   # respuesta 400 custom -> estado de error
./e2e/run.sh config     # smoke: abre pantalla "Mocks de red"

# Saltar instalación si ya está el APK correcto
SKIP_INSTALL=1 ./e2e/run.sh happy
```

## Qué valida

- `flows/pokedex_happy_mock.yaml` + `fixtures/pokemon_page_happy.json`: la Pokédex carga en modo `MOCK` y muestra los Pokémon del fixture.
- `flows/pokedex_custom_400.yaml` + `fixtures/pokemon_page_custom_400.json`: un `400` personalizado del mock se refleja como estado de error (`Reintentar`).
- `flows/open_mock_config.yaml`: la pantalla debug de mocks es accesible y muestra import/export/portabilidad.

## Cómo funciona

`e2e/run.sh` inicia la app con extras de debug (solo en builds `debug`):

```bash
adb shell am start -W -n com.irv205.pokeapi/.MainActivity \
  --es api_environment mock \
  --es mock_config_json_base64 "<BASE64_DEL_JSON>"
```

- `api_environment`: `mock` | `prod`
- `mock_config_json_base64`: configuración `List<MockService>` en base64 (recomendado para automatización; evita problemas de escaping)
- `mock_config_json`: JSON en texto plano (útil para casos simples)
- `mock_fallback_to_network`: `true|false` (opcional)

Las fixtures son el mismo formato exportable desde la UI/`MockConfigViewModel.exportConfigJson()`, por lo que puedes versionarlas por módulo/feature/API y reutilizarlas en CI o E2E.

## Notas
- El mock es estricto por defecto: si falta un match, la request falla (determinismo). Las suites deben incluir los servicios que el flujo necesita.
- Coil (imágenes) no pasa por `MockInterceptor`; los sprites pueden requerir red o manejo aparte según el entorno.
- La configuración es en memoria por proceso: cada flujo arranca con su fixture/estado conocido.
