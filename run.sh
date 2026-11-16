#!/usr/bin/env bash
# JavaChatApplication helper: compile, run, and profile the chat server.
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="${SRC_DIR:-$ROOT_DIR/src}"
BUILD_DIR="${BUILD_DIR:-$ROOT_DIR/build}"
PROFILE_DIR="${PROFILE_DIR:-$ROOT_DIR/profiling}"

SERVER_CLASS="${SERVER_CLASS:-server.ChatServer}"
CLIENT_CLASS="${CLIENT_CLASS:-client.ChatClient}"
GUI_CLASS="${GUI_CLASS:-gui.ChatClientGUI}"

log()  { printf '\033[1;32m[INFO]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[WARN]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[ERROR]\033[0m %s\n' "$*" >&2; exit 1; }

usage() {
  cat <<'USAGE'
JavaChatApplication automation script

Usage:
  ./run.sh compile
  ./run.sh server [port]
  ./run.sh profile-server [port]
  ./run.sh client [main-class] [client-arguments...]
  ./run.sh gui [main-class] [gui-arguments...]
  ./run.sh clean
  ./run.sh help

Examples:
  ./run.sh compile
  ./run.sh server 5000
  ./run.sh profile-server 5000
  ./run.sh client
  ./run.sh gui

Optional class overrides:
  SERVER_CLASS=server.ChatServer ./run.sh server 5000
  CLIENT_CLASS=your.package.ClientMain ./run.sh client
  GUI_CLASS=your.package.GuiMain ./run.sh gui

The script compiles every .java file found under ./src into ./build.
USAGE
}

require_java() {
  command -v java >/dev/null 2>&1 || die "Java runtime not found. Install a JDK and add java to PATH."
  command -v javac >/dev/null 2>&1 || die "Java compiler (javac) not found. Install a JDK and add javac to PATH."
}

compile_project() {
  require_java
  [[ -d "$SRC_DIR" ]] || die "Source directory not found: $SRC_DIR"
  local java_count
  java_count="$(find "$SRC_DIR" -type f -name '*.java' | wc -l | tr -d ' ')"
  [[ "$java_count" -gt 0 ]] || die "No .java source files found under $SRC_DIR"

  mkdir -p "$BUILD_DIR"
  log "Compiling $java_count Java source file(s)..."
  # xargs -0 safely handles spaces in source filenames.
  find "$SRC_DIR" -type f -name '*.java' -print0 |
    xargs -0 javac -encoding UTF-8 -d "$BUILD_DIR"
  log "Compilation successful. Class files are in: $BUILD_DIR"
}

ensure_compiled() {
  if [[ ! -d "$BUILD_DIR" ]] || ! find "$BUILD_DIR" -type f -name '*.class' -print -quit | grep -q .; then
    compile_project
  fi
}

run_main() {
  local main_class="$1"
  shift
  ensure_compiled
  log "Starting $main_class..."
  exec java -cp "$BUILD_DIR" "$main_class" "$@"
}

case "${1:-help}" in
  compile)
    compile_project
    ;;
  server)
    shift
    run_main "$SERVER_CLASS" "$@"
    ;;
  profile-server)
    shift
    ensure_compiled
    mkdir -p "$PROFILE_DIR"
    timestamp="$(date '+%Y%m%d-%H%M%S')"
    recording="$PROFILE_DIR/chatserver-$timestamp.jfr"
    log "Starting server with Java Flight Recorder enabled."
    log "Recording will be written to: $recording"
    log "Stop the server with Ctrl+C to finish the recording."
    exec java \
      "-XX:StartFlightRecording=filename=$recording,settings=profile,dumponexit=true" \
      -cp "$BUILD_DIR" "$SERVER_CLASS" "$@"
    ;;
  client)
    shift
    main_class="$CLIENT_CLASS"
    if [[ "${1:-}" == *.* ]]; then
      main_class="$1"
      shift
    fi
    run_main "$main_class" "$@"
    ;;
  gui)
    shift
    main_class="$GUI_CLASS"
    if [[ "${1:-}" == *.* ]]; then
      main_class="$1"
      shift
    fi
    run_main "$main_class" "$@"
    ;;
  clean)
    rm -rf "$BUILD_DIR"
    log "Removed compiled build directory."
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    usage
    die "Unknown command: $1"
    ;;
esac
