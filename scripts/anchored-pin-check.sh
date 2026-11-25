#!/usr/bin/env bash
# Anchored Java pin check for the Java 17 -> 21 upgrade.
# Uses file+key patterns (not a free-text search for "17").
# Exit 0 = all expectations met. Exit 1 = mismatch.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MODE="${1:-post}" # pre = positive control on main (expect 17); post = after change (expect 21)
FAIL=0

pass() { echo "PASS: $*"; }
fail() { echo "FAIL: $*"; FAIL=1; }

count_matches() {
  local file="$1"
  local pattern="$2"
  if [[ ! -f "$file" ]]; then
    echo 0
    return
  fi
  # -F fixed string; count matching lines
  grep -F -c -- "$pattern" "$file" 2>/dev/null || true
}

expect_count() {
  local file="$1"
  local pattern="$2"
  local want="$3"
  local label="$4"
  local got
  got="$(count_matches "$file" "$pattern")"
  if [[ "$got" == "$want" ]]; then
    pass "$label ($file: '$pattern' count=$got)"
  else
    fail "$label ($file: '$pattern' want=$want got=$got)"
  fi
}

file_absent() {
  local file="$1"
  local label="$2"
  if [[ ! -e "$file" ]]; then
    pass "$label ($file absent)"
  else
    fail "$label ($file present but expected absent)"
  fi
}

echo "=== anchored-pin-check mode=$MODE root=$ROOT ==="
echo "HEAD=$(git rev-parse HEAD 2>/dev/null || echo unknown)"

if [[ "$MODE" == "pre" ]]; then
  # Positive control: every in-scope Java 17 pin must match on main.
  expect_count "pom.xml" "<java.version>17</java.version>" 1 "PIN-01 java.version=17"
  expect_count "build.gradle" "JavaLanguageVersion.of(17)" 1 "PIN-02 toolchain=17"
  expect_count ".github/workflows/maven-build.yml" "java: [ '17' ]" 1 "PIN-03 maven matrix=17"
  expect_count ".github/workflows/gradle-build.yml" "java: [ '17' ]" 1 "PIN-04 gradle matrix=17"
  expect_count ".devcontainer/Dockerfile" "ARG VARIANT=17-bullseye" 1 "PIN-05 VARIANT=17"
  expect_count ".devcontainer/Dockerfile" "java:0-\${VARIANT}" 1 "PIN-06 FROM prefix 0-"
  expect_count ".devcontainer/Dockerfile" "ARG JAVA_VERSION=17.0.7-ms" 1 "PIN-07 JAVA_VERSION=17 sdkman"

  # Java 21 patterns must be absent in in-scope build pins.
  expect_count "pom.xml" "<java.version>21</java.version>" 0 "pre: no java.version=21"
  expect_count "build.gradle" "JavaLanguageVersion.of(21)" 0 "pre: no toolchain=21"
  expect_count ".github/workflows/maven-build.yml" "java: [ '21' ]" 0 "pre: no maven matrix=21"
  expect_count ".github/workflows/gradle-build.yml" "java: [ '21' ]" 0 "pre: no gradle matrix=21"

elif [[ "$MODE" == "post" ]]; then
  # After upgrade: Java 17 pins gone; Java 21 pins present.
  expect_count "pom.xml" "<java.version>17</java.version>" 0 "PIN-01 no java.version=17"
  expect_count "pom.xml" "<java.version>21</java.version>" 1 "PIN-01 java.version=21"
  expect_count "build.gradle" "JavaLanguageVersion.of(17)" 0 "PIN-02 no toolchain=17"
  expect_count "build.gradle" "JavaLanguageVersion.of(21)" 1 "PIN-02 toolchain=21"
  expect_count ".github/workflows/maven-build.yml" "java: [ '17' ]" 0 "PIN-03 no maven matrix=17"
  expect_count ".github/workflows/maven-build.yml" "java: [ '21' ]" 1 "PIN-03 maven matrix=21"
  expect_count ".github/workflows/gradle-build.yml" "java: [ '17' ]" 0 "PIN-04 no gradle matrix=17"
  expect_count ".github/workflows/gradle-build.yml" "java: [ '21' ]" 1 "PIN-04 gradle matrix=21"
  expect_count ".devcontainer/Dockerfile" "ARG VARIANT=17-bullseye" 0 "PIN-05 no VARIANT=17"
  expect_count ".devcontainer/Dockerfile" "ARG VARIANT=21-bullseye" 1 "PIN-05 VARIANT=21"
  expect_count ".devcontainer/Dockerfile" "java:0-\${VARIANT}" 0 "PIN-06 no FROM 0-"
  expect_count ".devcontainer/Dockerfile" "java:1-\${VARIANT}" 1 "PIN-06 FROM 1-"
  expect_count ".devcontainer/Dockerfile" "ARG JAVA_VERSION=17.0.7-ms" 0 "PIN-07 no JAVA_VERSION=17"
  expect_count ".devcontainer/Dockerfile" "ARG JAVA_VERSION=21.0.13.1-ms" 1 "PIN-07 JAVA_VERSION=21.0.13.1-ms"
else
  echo "Usage: $0 [pre|post]"
  exit 2
fi

# Out of scope / removed (both modes).
file_absent ".github/workflows/coderig-gate.yml" "EXC coderig-gate removed"
file_absent ".coderig/harness.yaml" "EXC harness.yaml removed (CodeRig offline)"
file_absent ".coderig/sensors.json" "EXC sensors.json removed (CodeRig offline)"

# Docs may still say 17; do not fail the pin check (out of scope).
if [[ -f README.md ]]; then
  readme17="$(count_matches README.md "Java 17")"
  echo "INFO: README.md 'Java 17' mentions=$readme17 (out_of_scope; not a gate failure)"
fi

# Inventory artifact should exist when this check is used in the research track.
if [[ -f .coderig/java17-pin-inventory.json ]]; then
  pass "inventory artifact present (.coderig/java17-pin-inventory.json)"
else
  echo "WARN: .coderig/java17-pin-inventory.json missing"
fi

echo "=== done mode=$MODE fail=$FAIL ==="
exit "$FAIL"
