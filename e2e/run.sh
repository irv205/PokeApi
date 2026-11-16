#!/usr/bin/env bash
set -euo pipefail

# Black-box E2E automation for PokeApi debug build using Maestro + mock fixtures.
# Usage:
#   ./e2e/run.sh                 # run all flows
#   ./e2e/run.sh happy           # run happy-path flow
#   ./e2e/run.sh error400        # run custom 400 flow
#   ./e2e/run.sh config          # run mock-config smoke flow
#   SKIP_INSTALL=1 ./e2e/run.sh  # skip ./gradlew :app:installDebug

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

ADB="${ADB:-}"
if [[ -z "$ADB" ]]; then
  if command -v adb >/dev/null 2>&1; then
    ADB="adb"
  elif [[ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
  else
    echo "adb not found. Set ADB or install Android platform-tools." >&2
    exit 1
  fi
fi

MAESTRO="${MAESTRO:-}"
if [[ -z "$MAESTRO" ]]; then
  if command -v maestro >/dev/null 2>&1; then
    MAESTRO="maestro"
  elif [[ -x "$HOME/.maestro/bin/maestro" ]]; then
    MAESTRO="$HOME/.maestro/bin/maestro"
  else
    echo "maestro not found. Install Maestro or set MAESTRO." >&2
    exit 1
  fi
fi

APP_ID="com.irv205.pokeapi"
ACTIVITY="$APP_ID/.MainActivity"
TARGET="${1:-all}"

if ! "$ADB" get-state >/dev/null 2>&1; then
  echo "No Android device/emulator connected." >&2
  "$ADB" devices >&2 || true
  exit 1
fi

if [[ "${SKIP_INSTALL:-0}" != "1" ]]; then
  echo "==> Installing debug APK"
  ./gradlew :app:installDebug --console=plain
fi

launch_with_fixture() {
  local fixture="$1"
  local json_b64
  json_b64="$(python3 -c 'import base64,sys; print(base64.b64encode(open(sys.argv[1],"rb").read()).decode())' "$fixture")"

  echo "==> Launching app with fixture: $fixture"
  "$ADB" shell am force-stop "$APP_ID" || true
  "$ADB" logcat -c || true
  "$ADB" shell am start -W -n "$ACTIVITY" \
    --es api_environment mock \
    --es mock_config_json_base64 "$json_b64" >/dev/null
  sleep 3
}

run_flow() {
  local fixture="$1"
  local flow="$2"
  launch_with_fixture "$fixture"
  echo "==> Running flow: $flow"

  local maestro_args=()
  if [[ -n "${E2E_HTML_DIR:-}" ]]; then
    mkdir -p "$E2E_HTML_DIR"
    maestro_args+=(--format html --output "$E2E_HTML_DIR/$(basename "$flow" .yaml).html")
  fi

  "$MAESTRO" test "${maestro_args[@]}" "$flow"
}

case "$TARGET" in
  all)
    run_flow "e2e/fixtures/pokemon_page_happy.json" "e2e/flows/pokedex_happy_mock.yaml"
    run_flow "e2e/fixtures/pokemon_page_custom_400.json" "e2e/flows/pokedex_custom_400.yaml"
    run_flow "e2e/fixtures/pokemon_page_happy.json" "e2e/flows/open_mock_config.yaml"
    ;;
  happy)
    run_flow "e2e/fixtures/pokemon_page_happy.json" "e2e/flows/pokedex_happy_mock.yaml"
    ;;
  error400|400|custom400)
    run_flow "e2e/fixtures/pokemon_page_custom_400.json" "e2e/flows/pokedex_custom_400.yaml"
    ;;
  config)
    run_flow "e2e/fixtures/pokemon_page_happy.json" "e2e/flows/open_mock_config.yaml"
    ;;
  *)
    echo "Unknown target: $TARGET" >&2
    echo "Use: all | happy | error400 | config" >&2
    exit 1
    ;;
esac

echo "==> E2E flows completed"
