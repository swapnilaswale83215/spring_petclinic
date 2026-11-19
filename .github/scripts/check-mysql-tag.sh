#!/usr/bin/env bash
# =============================================================================
# check-mysql-tag.sh
#
# Purpose   : Verify that every mysql:<tag> reference in the repository uses
#             the single authoritative allowed tag defined by ALLOWED_TAG below.
#             Update ALLOWED_TAG here — and only here — to change the permitted
#             tag across the entire check without modifying any other file.
#
# Scopes    : <scan-root>/src/
#             <scan-root>/docker-compose*.yml and <scan-root>/compose*.yml
#             <scan-root>/.github/
#             Loose files at <scan-root>/ (one level, non-recursive)
#
# Exclusions: build/  .gradle/  .git/  binary files
#
# Exit codes:
#   0  — All mysql: references are compliant (only mysql:8.4 found, or none).
#   1  — One or more non-compliant mysql:<tag> references were found;
#         each violating file path and offending line is printed to stdout.
#   2  — Invocation error: the supplied scan-root directory does not exist,
#         is not a directory, or is not readable; the error is printed to stderr.
#
# Usage:
#   check-mysql-tag.sh [--dry-run] [<scan-root>]
#
#   --dry-run   Print all non-compliant occurrences with file path and line
#               number but exit 0 regardless of findings (audit mode).
#   <scan-root> Optional path to the repository root to scan.
#               Defaults to the directory two levels above this script
#               (i.e. the repository root when the script lives at
#               .github/scripts/check-mysql-tag.sh).
#
# Requirements: POSIX shell, grep (no third-party tooling required).
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Single authoritative allowed tag — edit this one line to change the policy.
# ---------------------------------------------------------------------------
ALLOWED_TAG="8.4"

# ---------------------------------------------------------------------------
# Parse arguments
# ---------------------------------------------------------------------------
DRY_RUN=false
SCAN_ROOT=""

for arg in "$@"; do
  case "$arg" in
    --dry-run)
      DRY_RUN=true
      ;;
    -*)
      echo "ERROR: Unknown option: $arg" >&2
      exit 2
      ;;
    *)
      if [ -n "$SCAN_ROOT" ]; then
        echo "ERROR: Too many positional arguments." >&2
        exit 2
      fi
      SCAN_ROOT="$arg"
      ;;
  esac
done

# Default scan root: two directories above this script (.github/scripts/ -> repo root)
if [ -z "$SCAN_ROOT" ]; then
  SCRIPT_DIR="$(CDPATH="" cd -- "$(dirname -- "$0")" && pwd)"
  SCAN_ROOT="$(dirname -- "$(dirname -- "$SCRIPT_DIR")")"
fi

# ---------------------------------------------------------------------------
# Validate scan root
# ---------------------------------------------------------------------------
if [ ! -e "$SCAN_ROOT" ]; then
  echo "ERROR: Scan root does not exist: $SCAN_ROOT" >&2
  exit 2
fi
if [ ! -d "$SCAN_ROOT" ]; then
  echo "ERROR: Scan root is not a directory: $SCAN_ROOT" >&2
  exit 2
fi
if [ ! -r "$SCAN_ROOT" ]; then
  echo "ERROR: Scan root is not readable: $SCAN_ROOT" >&2
  exit 2
fi

# ---------------------------------------------------------------------------
# Build the grep pattern:
#   Match any "mysql:" followed by a tag that is NOT the allowed tag.
#   Strategy: grep for all "mysql:" occurrences, then filter out compliant ones.
# ---------------------------------------------------------------------------
COMPLIANT_PATTERN="mysql:${ALLOWED_TAG}"

# Collect violations into a variable.
# We use a temp file to avoid subshell exit-code loss under set -e.
TMP_RESULTS="$(mktemp)"
trap 'rm -f "$TMP_RESULTS"' EXIT

run_grep() {
  local target="$1"
  shift
  # -r  recursive
  # -I  skip binary files
  # -n  print line numbers
  # --exclude-dir excludes generated/vcs directories
  grep -rIn \
    --exclude-dir=build \
    --exclude-dir=.gradle \
    --exclude-dir=.git \
    "$@" \
    "mysql:" "$target" 2>/dev/null \
    | grep -v "${COMPLIANT_PATTERN}" \
    >> "$TMP_RESULTS" || true
}

# Scope 1: src/
if [ -d "${SCAN_ROOT}/src" ]; then
  run_grep "${SCAN_ROOT}/src"
fi

# Scope 2: .github/
if [ -d "${SCAN_ROOT}/.github" ]; then
  run_grep "${SCAN_ROOT}/.github"
fi

# Scope 3: Docker Compose files at repo root (non-recursive, glob)
for compose_file in \
    "${SCAN_ROOT}"/docker-compose*.yml \
    "${SCAN_ROOT}"/docker-compose*.yaml \
    "${SCAN_ROOT}"/compose*.yml \
    "${SCAN_ROOT}"/compose*.yaml; do
  if [ -f "$compose_file" ]; then
    grep -In "mysql:" "$compose_file" 2>/dev/null \
      | grep -v "${COMPLIANT_PATTERN}" \
      | sed "s|^|${compose_file}:|" \
      >> "$TMP_RESULTS" || true
  fi
done

# Scope 4: Loose files at repo root (one level, non-recursive)
for root_file in \
    "${SCAN_ROOT}"/*.yml \
    "${SCAN_ROOT}"/*.yaml \
    "${SCAN_ROOT}"/*.xml \
    "${SCAN_ROOT}"/*.properties \
    "${SCAN_ROOT}"/*.txt \
    "${SCAN_ROOT}"/Dockerfile \
    "${SCAN_ROOT}"/*.sh; do
  if [ -f "$root_file" ]; then
    grep -In "mysql:" "$root_file" 2>/dev/null \
      | grep -v "${COMPLIANT_PATTERN}" \
      | sed "s|^|${root_file}:|" \
      >> "$TMP_RESULTS" || true
  fi
done

# ---------------------------------------------------------------------------
# Evaluate results
# ---------------------------------------------------------------------------
if [ ! -s "$TMP_RESULTS" ]; then
  echo "All mysql: references are compliant (mysql:${ALLOWED_TAG} only)."
  exit 0
fi

# Non-compliant references found
VIOLATION_COUNT="$(wc -l < "$TMP_RESULTS" | tr -d ' ')"
echo "Non-compliant mysql: tag(s) found — expected mysql:${ALLOWED_TAG} only."
echo "Violations (${VIOLATION_COUNT}):"
cat "$TMP_RESULTS"

if [ "$DRY_RUN" = true ]; then
  echo "(dry-run mode: exiting 0 despite violations)"
  exit 0
fi

exit 1
