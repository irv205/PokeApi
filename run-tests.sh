#!/usr/bin/env bash
set -uo pipefail

# Test runner interactivo para PokeCompose.
# Uso: ./run-tests.sh
#
# Opciones:
#   1) Todos los tests: unit + instrumentados + E2E
#   2) Solo unit tests
#   3) Test automation E2E (Maestro)
#
# Al final genera un reporte HTML consolidado en:
#   build/reports/test-runner/index.html
#
# Variables opcionales:
#   ADB=/ruta/a/adb
#   MAESTRO=/ruta/a/maestro
#   OPEN_REPORT=1   # abre el HTML al terminar (macOS)

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

REPORT_DIR="$ROOT_DIR/build/reports/test-runner"
E2E_REPORT_DIR="$REPORT_DIR/e2e"
rm -rf "$E2E_REPORT_DIR"
mkdir -p "$E2E_REPORT_DIR"

if [[ -t 1 ]]; then
  BOLD="$(tput bold 2>/dev/null || true)"
  GREEN="$(tput setaf 2 2>/dev/null || true)"
  RED="$(tput setaf 1 2>/dev/null || true)"
  YELLOW="$(tput setaf 3 2>/dev/null || true)"
  RESET="$(tput sgr0 2>/dev/null || true)"
else
  BOLD="" GREEN="" RED="" YELLOW="" RESET=""
fi

# ---------------------------------------------------------------
# Herramientas
# ---------------------------------------------------------------
ADB="${ADB:-}"
if [[ -z "$ADB" ]]; then
  if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
  elif [[ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
  fi
fi

MAESTRO="${MAESTRO:-}"
if [[ -z "$MAESTRO" ]]; then
  if command -v maestro >/dev/null 2>&1; then
    MAESTRO="$(command -v maestro)"
  elif [[ -x "$HOME/.maestro/bin/maestro" ]]; then
    MAESTRO="$HOME/.maestro/bin/maestro"
  fi
fi

export ADB MAESTRO

device_ready() {
  [[ -n "$ADB" ]] || return 1
  "$ADB" devices 2>/dev/null | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit found ? 0 : 1 }'
}

format_duration() {
  local seconds="$1"
  if (( seconds >= 60 )); then
    printf '%dm %02ds' $((seconds / 60)) $((seconds % 60))
  else
    printf '%ds' "$seconds"
  fi
}

# ---------------------------------------------------------------
# Menú
# ---------------------------------------------------------------
echo
echo "${BOLD}=============================================="
echo "  PokeCompose · Test Runner"
echo "==============================================${RESET}"
echo
echo "¿Qué quieres ejecutar?"
echo "  1) Todos los tests (unit + instrumentados + E2E)"
echo "  2) Solo unit tests"
echo "  3) Test automation (E2E Maestro)"
echo "  0) Salir"
echo

MODE=""
RUN_UNIT=0
RUN_INSTRUMENTED=0
RUN_E2E=0

while [[ -z "$MODE" ]]; do
  read -r -p "Selecciona una opción [1-3]: " OPTION || exit 1
  case "$OPTION" in
    1)
      MODE="Todos los tests"
      RUN_UNIT=1
      RUN_INSTRUMENTED=1
      RUN_E2E=1
      ;;
    2)
      MODE="Solo unit tests"
      RUN_UNIT=1
      ;;
    3)
      MODE="Test automation (E2E)"
      RUN_E2E=1
      ;;
    0|q|Q)
      echo "Saliendo."
      exit 0
      ;;
    *)
      echo "${YELLOW}Opción inválida. Elige 1, 2, 3 o 0.${RESET}"
      ;;
  esac
done

EXIT_CODE=0

# ---------------------------------------------------------------
# Verificación de dispositivo
# ---------------------------------------------------------------
if [[ "$RUN_INSTRUMENTED" -eq 1 || "$RUN_E2E" -eq 1 ]]; then
  if ! device_ready; then
    echo
    echo "${YELLOW}⚠ No hay dispositivo/emulador conectado.${RESET}"
    echo "  Se omitirán los tests que lo requieren."
    echo "  Arranca uno con:"
    echo "    $HOME/Library/Android/sdk/emulator/emulator -avd Pixel_9 &"
    echo "  y verifica con:"
    echo "    ${ADB:-adb} devices"
    echo
    RUN_INSTRUMENTED=0
    RUN_E2E=0
    if [[ "$RUN_UNIT" -eq 0 ]]; then
      EXIT_CODE=1
    fi
  fi
fi

# ---------------------------------------------------------------
# Ejecución
# ---------------------------------------------------------------
STARTED_AT="$(date '+%Y-%m-%d %H:%M:%S')"
START_TS="$(date +%s)"
UNIT_STATUS="skipped"
INSTRUMENTED_STATUS="skipped"
E2E_STATUS="skipped"

if [[ "$RUN_UNIT" -eq 1 ]]; then
  echo
  echo "${BOLD}==> Unit tests${RESET}"
  if ./gradlew :app:testDebugUnitTest --console=plain; then
    UNIT_STATUS="passed"
    echo "${GREEN}✓ Unit tests OK${RESET}"
  else
    UNIT_STATUS="failed"
    EXIT_CODE=1
    echo "${RED}✗ Unit tests con fallos${RESET}"
  fi
fi

if [[ "$RUN_INSTRUMENTED" -eq 1 ]]; then
  echo
  echo "${BOLD}==> Tests instrumentados / UI${RESET}"
  if ./gradlew :app:connectedDebugAndroidTest --console=plain; then
    INSTRUMENTED_STATUS="passed"
    echo "${GREEN}✓ Tests instrumentados OK${RESET}"
  else
    INSTRUMENTED_STATUS="failed"
    EXIT_CODE=1
    echo "${RED}✗ Tests instrumentados con fallos${RESET}"
  fi
fi

if [[ "$RUN_E2E" -eq 1 ]]; then
  echo
  echo "${BOLD}==> Test automation E2E (Maestro)${RESET}"
  if E2E_HTML_DIR="$E2E_REPORT_DIR" ./e2e/run.sh; then
    E2E_STATUS="passed"
    echo "${GREEN}✓ E2E OK${RESET}"
  else
    E2E_STATUS="failed"
    EXIT_CODE=1
    echo "${RED}✗ E2E con fallos${RESET}"
  fi
fi

FINISHED_AT="$(date '+%Y-%m-%d %H:%M:%S')"
FINISH_TS="$(date +%s)"
DURATION="$(format_duration $((FINISH_TS - START_TS)))"

# El reporte se genera incluso si hubo fallos o suites omitidas,
# para dejar constancia del resultado de la ejecución.

# ---------------------------------------------------------------
# Reporte HTML consolidado
# ---------------------------------------------------------------
echo
echo "${BOLD}==> Generando reporte HTML${RESET}"

MODE="$MODE" \
STARTED_AT="$STARTED_AT" \
FINISHED_AT="$FINISHED_AT" \
DURATION="$DURATION" \
UNIT_STATUS="$UNIT_STATUS" \
INSTRUMENTED_STATUS="$INSTRUMENTED_STATUS" \
E2E_STATUS="$E2E_STATUS" \
python3 - "$REPORT_DIR" <<'PY'
import html
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

report_dir = Path(sys.argv[1]).resolve()
root = report_dir.parents[2]

mode = os.environ.get("MODE", "Tests")
started_at = os.environ.get("STARTED_AT", "")
finished_at = os.environ.get("FINISHED_AT", "")
duration = os.environ.get("DURATION", "")
unit_status = os.environ.get("UNIT_STATUS", "skipped")
instrumented_status = os.environ.get("INSTRUMENTED_STATUS", "skipped")
e2e_status = os.environ.get("E2E_STATUS", "skipped")

STATUS_LABELS = {
    "passed": ("Pasó", "passed"),
    "failed": ("Falló", "failed"),
    "skipped": ("Omitido", "skipped"),
}


def badge(status: str) -> str:
    label, css = STATUS_LABELS.get(status, (status, "skipped"))
    return f'<span class="badge {html.escape(css)}">{html.escape(label)}</span>'


def rel(target: Path) -> str:
    return html.escape(os.path.relpath(target, report_dir))


def report_link(target: Path, text: str) -> str:
    if not target.exists():
        return ""
    return f'<a class="btn" href="{rel(target)}">{html.escape(text)}</a>'


# --- Unit tests -------------------------------------------------
unit_rows = []
unit_totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
unit_xml_dir = root / "app/build/test-results/testDebugUnitTest"

if unit_status != "skipped" and unit_xml_dir.is_dir():
    for xml_file in sorted(unit_xml_dir.glob("TEST-*.xml")):
        try:
            suite = ET.parse(xml_file).getroot()
        except ET.ParseError:
            continue

        name = suite.get("name") or xml_file.stem
        tests = int(suite.get("tests") or 0)
        failures = int(suite.get("failures") or 0)
        errors = int(suite.get("errors") or 0)
        skipped = int(suite.get("skipped") or 0)

        unit_totals["tests"] += tests
        unit_totals["failures"] += failures
        unit_totals["errors"] += errors
        unit_totals["skipped"] += skipped

        class_report = (
            root
            / "app/build/reports/tests/testDebugUnitTest"
            / name
            / "index.html"
        )
        link = report_link(class_report, "Ver")
        row_class = "row-failed" if failures or errors else ""
        unit_rows.append(
            f'<tr class="{row_class}">'
            f"<td>{html.escape(name)}</td>"
            f"<td>{tests}</td>"
            f"<td>{failures}</td>"
            f"<td>{errors}</td>"
            f"<td>{skipped}</td>"
            f"<td>{link or '—'}</td>"
            f"</tr>"
        )

if unit_status == "skipped":
    unit_summary = "No ejecutado."
elif unit_totals["tests"] == 0:
    unit_summary = "Sin resultados de tests."
else:
    failed = unit_totals["failures"] + unit_totals["errors"]
    unit_summary = (
        f'{unit_totals["tests"]} tests · '
        f'{failed} fallos · '
        f'{unit_totals["skipped"]} omitidos'
    )

unit_report = root / "app/build/reports/tests/testDebugUnitTest/index.html"
unit_link = (
    report_link(unit_report, "Abrir reporte Gradle")
    if unit_status != "skipped"
    else ""
)

# --- Instrumented tests ----------------------------------------
instrumented_report = (
    root / "app/build/reports/androidTests/connected/debug/index.html"
)
if instrumented_status == "skipped":
    instrumented_summary = "No ejecutado."
else:
    instrumented_summary = "Tests de UI instrumentados en dispositivo/emulador."
instrumented_link = (
    report_link(instrumented_report, "Abrir reporte Gradle")
    if instrumented_status != "skipped"
    else ""
)

# --- E2E tests --------------------------------------------------
e2e_links = []
if e2e_status != "skipped":
    for report in sorted((report_dir / "e2e").glob("*.html")):
        label = report.stem.replace("_", " ")
        e2e_links.append(
            f'<li><a href="{rel(report)}">{html.escape(label)}</a></li>'
        )

if e2e_status == "skipped":
    e2e_summary = "No ejecutado."
elif e2e_links:
    e2e_summary = f"{len(e2e_links)} flujo(s) con reporte Maestro."
else:
    e2e_summary = "Sin reportes generados."

if e2e_links:
    e2e_detail = '<ul class="report-list">' + "".join(e2e_links) + "</ul>"
else:
    e2e_detail = ""

unit_detail = ""
if unit_rows:
    unit_detail = f"""
    <section class="panel">
      <h2>Detalle de unit tests</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Clase</th>
              <th>Tests</th>
              <th>Fallos</th>
              <th>Errores</th>
              <th>Omitidos</th>
              <th>Reporte</th>
            </tr>
          </thead>
          <tbody>
            {''.join(unit_rows)}
          </tbody>
        </table>
      </div>
    </section>
    """

e2e_panel = ""
if e2e_detail:
    e2e_panel = f"""
    <section class="panel">
      <h2>Reportes E2E (Maestro)</h2>
      {e2e_detail}
    </section>
    """

CSS = """
:root { color-scheme: light dark; }
* { box-sizing: border-box; }
body {
  margin: 0;
  padding: 32px 20px 48px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  background: #f3f6fb;
  color: #172033;
}
.wrap { max-width: 1080px; margin: 0 auto; }
header {
  padding: 28px 32px;
  border-radius: 18px;
  background: linear-gradient(135deg, #1d4ed8, #7c3aed);
  color: #fff;
  box-shadow: 0 14px 34px rgba(29, 78, 216, .22);
}
header h1 { margin: 0 0 6px; font-size: 28px; letter-spacing: -.02em; }
header .subtitle { margin: 0; opacity: .9; font-size: 15px; }
.meta { display: flex; flex-wrap: wrap; gap: 8px 20px; margin-top: 18px; font-size: 13px; opacity: .9; }
.cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; margin: 22px 0; }
.card, .panel {
  background: Canvas;
  color: CanvasText;
  border: 1px solid rgba(100, 116, 139, .2);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, .06);
}
.card { padding: 20px; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.card h2 { margin: 0; font-size: 17px; }
.card p { margin: 12px 0 16px; font-size: 14px; opacity: .78; min-height: 20px; }
.badge { display: inline-flex; align-items: center; padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.badge.passed { background: #dcfce7; color: #166534; }
.badge.failed { background: #fee2e2; color: #991b1b; }
.badge.skipped { background: #e2e8f0; color: #475569; }
.btn {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 9px;
  background: #1d4ed8;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}
.btn:hover { background: #1e40af; }
.panel { padding: 22px; margin-top: 18px; }
.panel h2 { margin: 0 0 14px; font-size: 18px; }
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: 14px; }
th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid rgba(100, 116, 139, .18); }
th { font-size: 12px; text-transform: uppercase; letter-spacing: .04em; opacity: .62; }
tr.row-failed td { background: rgba(239, 68, 68, .08); }
.report-list { margin: 0; padding-left: 20px; line-height: 1.9; }
.report-list a { color: #1d4ed8; font-weight: 600; text-decoration: none; }
.report-list a:hover { text-decoration: underline; }
footer { margin-top: 28px; text-align: center; font-size: 12px; opacity: .55; }
@media (prefers-color-scheme: dark) {
  body { background: #0b1220; color: #e2e8f0; }
  .card, .panel { background: #111a2e; border-color: rgba(148, 163, 184, .18); }
  .badge.passed { background: #14532d; color: #bbf7d0; }
  .badge.failed { background: #7f1d1d; color: #fecaca; }
  .badge.skipped { background: #1e293b; color: #cbd5e1; }
  .report-list a { color: #93c5fd; }
}
"""

doc = f"""<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Reporte de tests · PokeCompose</title>
  <style>{CSS}</style>
</head>
<body>
  <div class="wrap">
    <header>
      <h1>Reporte de tests</h1>
      <p class="subtitle">PokeCompose · {html.escape(mode)}</p>
      <div class="meta">
        <span>Inicio: {html.escape(started_at)}</span>
        <span>Fin: {html.escape(finished_at)}</span>
        <span>Duración: {html.escape(duration)}</span>
      </div>
    </header>

    <section class="cards">
      <article class="card">
        <div class="card-head">
          <h2>Unit tests</h2>
          {badge(unit_status)}
        </div>
        <p>{html.escape(unit_summary)}</p>
        {unit_link}
      </article>

      <article class="card">
        <div class="card-head">
          <h2>Instrumentados / UI</h2>
          {badge(instrumented_status)}
        </div>
        <p>{html.escape(instrumented_summary)}</p>
        {instrumented_link}
      </article>

      <article class="card">
        <div class="card-head">
          <h2>E2E Maestro</h2>
          {badge(e2e_status)}
        </div>
        <p>{html.escape(e2e_summary)}</p>
      </article>
    </section>

    {unit_detail}
    {e2e_panel}

    <footer>Generado por <code>./run-tests.sh</code></footer>
  </div>
</body>
</html>
"""

(report_dir / "index.html").write_text(doc, encoding="utf-8")
print(f"Reporte generado: {report_dir / 'index.html'}")
PY

REPORT_FILE="$REPORT_DIR/index.html"

echo
if [[ -f "$REPORT_FILE" ]]; then
  echo "${GREEN}✓ Reporte HTML:${RESET} $REPORT_FILE"
  if [[ "${OPEN_REPORT:-0}" == "1" ]] && command -v open >/dev/null 2>&1; then
    open "$REPORT_FILE"
  else
    echo "  Ábrelo con: open \"$REPORT_FILE\""
  fi
else
  echo "${RED}✗ No se pudo generar el reporte HTML.${RESET}"
  EXIT_CODE=1
fi

if [[ "$EXIT_CODE" -eq 0 ]]; then
  echo
  echo "${GREEN}${BOLD}✓ Ejecución completada${RESET}"
else
  echo
  echo "${RED}${BOLD}✗ Ejecución finalizada con errores u omisiones${RESET}"
fi

exit "$EXIT_CODE"
