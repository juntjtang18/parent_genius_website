#!/usr/bin/env bash
# Start the local Genius Parenting stack:
#   1. Cloud SQL Auth Proxy  (unix socket /cloudsql/...)
#   2. app-subscription      http://127.0.0.1:8091
#   3. pg_strapi4            http://127.0.0.1:8080
#   4. Spring website        http://127.0.0.1:8081
#
# Usage:
#   ./start-local.sh        start everything and stream logs
#   ./start-local.sh stop   stop processes this script started
#
# Override paths with CLOUD_SQL_PROXY_SCRIPT, GPA_ROOT, JAVA_HOME.

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
GPA_ROOT="${GPA_ROOT:-$(cd -- "${SCRIPT_DIR}/.." && pwd)}"
SPRING_DIR="${SCRIPT_DIR}"
STRAPI_DIR="${GPA_ROOT}/pg_strapi4"
SUBSCRIPTION_DIR="${GPA_ROOT}/app-subscription"
PROXY_SCRIPT="${CLOUD_SQL_PROXY_SCRIPT:-/Users/juntjtang/develop/cloud-sql-server/start-proxy.sh}"
CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE:-lucid-arch-451211-b0:us-west1:cloud-sql-server}"
SOCKET_FILE="/cloudsql/${CLOUD_SQL_INSTANCE}/.s.PGSQL.5432"
LOG_DIR="${SCRIPT_DIR}/logs"
STATE_DIR="${LOG_DIR}"

SUBSCRIPTION_PORT="${SUBSCRIPTION_PORT:-8091}"
STRAPI_PORT="${STRAPI_PORT:-8080}"
SPRING_PORT="${SPRING_PORT:-8081}"

STARTED_PROXY=0
PIDS=()
STARTED_NAMES=()

usage() {
  cat <<'EOF'
Start Cloud SQL proxy, app-subscription, pg_strapi4, and the Spring website.

Usage:
  ./start-local.sh
  ./start-local.sh stop
EOF
}

log() {
  printf '%s\n' "$*"
}

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

port_in_use() {
  lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
}

pid_alive() {
  kill -0 "$1" 2>/dev/null
}

kill_tree() {
  local pid="$1"
  local child
  if ! pid_alive "${pid}"; then
    return 0
  fi
  while read -r child; do
    [[ -n "${child}" ]] && kill_tree "${child}"
  done < <(pgrep -P "${pid}" 2>/dev/null || true)
  kill "${pid}" 2>/dev/null || true
}

write_pid() {
  printf '%s\n' "$2" > "${STATE_DIR}/$1.pid"
}

read_pid() {
  local file="${STATE_DIR}/$1.pid"
  if [[ -f "${file}" ]]; then
    tr -d '[:space:]' < "${file}"
  fi
}

stop_named() {
  local name="$1"
  local pid
  pid="$(read_pid "${name}" || true)"
  if [[ -n "${pid}" ]] && pid_alive "${pid}"; then
    log "Stopping ${name} (pid ${pid})"
    kill_tree "${pid}"
    wait "${pid}" 2>/dev/null || true
  fi
  rm -f "${STATE_DIR}/${name}.pid"
}

stop_all() {
  stop_named spring
  stop_named strapi
  stop_named subscription
  if [[ "${STARTED_PROXY}" -eq 1 ]]; then
    stop_named cloud-sql-proxy
  elif [[ "${1:-}" == "force" ]]; then
    stop_named cloud-sql-proxy
  fi
}

cleanup() {
  local status=$?
  trap - EXIT INT TERM
  log ""
  log "Stopping local stack..."
  stop_all
  exit "${status}"
}

resolve_java_home() {
  local candidate
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    return 0
  fi
  for candidate in \
    "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" \
    "/opt/homebrew/opt/openjdk@17" \
    "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  do
    if [[ -x "${candidate}/bin/java" ]]; then
      export JAVA_HOME="${candidate}"
      return 0
    fi
  done
  if [[ -x /usr/libexec/java_home ]]; then
    candidate="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    if [[ -n "${candidate}" && -x "${candidate}/bin/java" ]]; then
      export JAVA_HOME="${candidate}"
      return 0
    fi
  fi
  die "Java 17 not found. Install OpenJDK 17 or set JAVA_HOME."
}

require_dirs() {
  [[ -d "${STRAPI_DIR}" ]] || die "Missing Strapi project: ${STRAPI_DIR}"
  [[ -d "${SUBSCRIPTION_DIR}" ]] || die "Missing subscription project: ${SUBSCRIPTION_DIR}"
  command -v mvn >/dev/null 2>&1 || die "Maven (mvn) is not on PATH."
  command -v npm >/dev/null 2>&1 || die "npm is not on PATH."
}

ensure_npm_modules() {
  local dir="$1"
  if [[ ! -d "${dir}/node_modules" ]]; then
    log "Installing npm dependencies in ${dir}..."
    (cd "${dir}" && npm install)
  fi
}

wait_for_socket() {
  local seconds="${1:-45}"
  local i
  for ((i = 1; i <= seconds; i++)); do
    if [[ -S "${SOCKET_FILE}" ]]; then
      log "Cloud SQL socket is ready: ${SOCKET_FILE}"
      return 0
    fi
    sleep 1
  done
  die "Cloud SQL socket did not appear at ${SOCKET_FILE}"
}

wait_for_http() {
  local url="$1"
  local seconds="$2"
  local name="$3"
  local i
  local pid
  pid="$(read_pid "${name}" || true)"
  log "Waiting for ${name} at ${url}..."
  for ((i = 1; i <= seconds; i++)); do
    if [[ -n "${pid}" ]] && ! pid_alive "${pid}"; then
      log "----- ${name} exited. Last log -----"
      tail -n 80 "${LOG_DIR}/${name}.log" || true
      die "${name} exited before becoming ready"
    fi
    if curl -fsS --max-time 2 "${url}" >/dev/null 2>&1; then
      log "${name} is ready: ${url}"
      return 0
    fi
    if (( i % 10 == 0 )); then
      log "Still waiting for ${name} (${i}s/${seconds}s)..."
    fi
    sleep 1
  done
  log "----- ${name} timed out. Last log -----"
  tail -n 80 "${LOG_DIR}/${name}.log" || true
  die "${name} did not become ready at ${url}"
}

# Run a service with a TTY so npm/webpack/Maven flush live, and print every
# line to the terminal with a [name] prefix while also writing the log file.
start_logged() {
  local name="$1"
  local dir="$2"
  shift 2
  : > "${LOG_DIR}/${name}.log"
  log "Starting ${name}..."
  (
    cd "${dir}"
    if command -v script >/dev/null 2>&1; then
      exec script -q /dev/null "$@"
    fi
    exec "$@"
  ) > >(tee -a "${LOG_DIR}/${name}.log" | sed -l "s/^/[${name}] /") 2>&1 &
  local pid=$!
  PIDS+=("${pid}")
  STARTED_NAMES+=("${name}")
  write_pid "${name}" "${pid}"
  sleep 0.4
  if ! pid_alive "${pid}"; then
    log "----- ${name} exited. Last log -----"
    tail -n 80 "${LOG_DIR}/${name}.log" || true
    die "${name} exited immediately"
  fi
}

start_proxy() {
  if [[ -S "${SOCKET_FILE}" ]]; then
    log "Cloud SQL proxy already running (${SOCKET_FILE})"
    return 0
  fi
  [[ -x "${PROXY_SCRIPT}" ]] || die "Proxy script not found or not executable: ${PROXY_SCRIPT}"
  start_logged cloud-sql-proxy "$(dirname "${PROXY_SCRIPT}")" "${PROXY_SCRIPT}"
  STARTED_PROXY=1
  wait_for_socket 45
}

start_subscription() {
  if port_in_use "${SUBSCRIPTION_PORT}"; then
    log "app-subscription already listening on ${SUBSCRIPTION_PORT}"
    return 0
  fi
  ensure_npm_modules "${SUBSCRIPTION_DIR}"
  start_logged subscription "${SUBSCRIPTION_DIR}" npm run dev
  wait_for_http "http://127.0.0.1:${SUBSCRIPTION_PORT}/healthz" 60 "subscription"
}

start_strapi() {
  if port_in_use "${STRAPI_PORT}"; then
    log "pg_strapi4 already listening on ${STRAPI_PORT}"
    return 0
  fi
  ensure_npm_modules "${STRAPI_DIR}"
  start_logged strapi "${STRAPI_DIR}" npm run develop
  wait_for_http "http://127.0.0.1:${STRAPI_PORT}" 180 "strapi"
}

start_spring() {
  if port_in_use "${SPRING_PORT}"; then
    log "Spring already listening on ${SPRING_PORT}"
    return 0
  fi
  resolve_java_home
  export PATH="${JAVA_HOME}/bin:${PATH}"
  log "Using JAVA_HOME=${JAVA_HOME}"
  start_logged spring "${SPRING_DIR}" mvn spring-boot:run
  wait_for_http "http://127.0.0.1:${SPRING_PORT}" 180 "spring"
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

mkdir -p "${LOG_DIR}"

if [[ "${1:-}" == "stop" ]]; then
  STARTED_PROXY=1
  stop_all force
  log "Local stack stopped."
  exit 0
fi

if [[ -n "${1:-}" ]]; then
  usage >&2
  exit 1
fi

require_dirs
trap cleanup EXIT INT TERM

log "--- Starting local stack ---"
log "Logs: ${LOG_DIR}"
start_proxy
start_subscription
start_strapi
start_spring

log ""
log "Ready:"
log "  Cloud SQL socket   ${SOCKET_FILE}"
log "  app-subscription   http://127.0.0.1:${SUBSCRIPTION_PORT}"
log "  Strapi             http://127.0.0.1:${STRAPI_PORT}"
log "  Spring website     http://127.0.0.1:${SPRING_PORT}"
log ""
log "Ctrl+C stops the processes this script started."

# Output is already streaming from each service. Keep the script in the
# foreground and fail if a process we started dies.
while true; do
  for name in "${STARTED_NAMES[@]}"; do
    pid="$(read_pid "${name}" || true)"
    if [[ -n "${pid}" ]] && ! pid_alive "${pid}"; then
      log "----- ${name} exited. Last log -----"
      tail -n 80 "${LOG_DIR}/${name}.log" || true
      die "${name} exited"
    fi
  done
  sleep 2
done
