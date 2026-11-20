#!/usr/bin/env bash
# scripts/check-mysql-tag.sh
#
# Consistency check: every mysql:<tag> reference in the repository must use
# the single authoritative tag defined by ALLOWED_TAG below.
#
# Scan scope (single invocation covers all three required areas):
#   - src/          (all text files; db/migration/, build/, .gradle/, .git/ excluded)
#   - docker-compose*.yml / compose*.yml  (root and subdirectories)
#   - .github/      (workflow YAML files and action definitions)
#
# Usage:
#   ./scripts/check-mysql-tag.sh            # normal mode  — exits 1 on violations
#   ./scripts/check-mysql-tag.sh --dry-run  # audit mode   — prints findings, exits 0
#
# Run from the repository root.
# Requires only a POSIX shell and grep (no third-party tooling needed).
#
# Exit codes:
#   0  all mysql: references are compliant and at least one was found
#   1  violations found, or zero compliant mysql:8.4 references found
#   2  invocation error (unknown flag)

set -euo pipefail

# ── Single authoritative allowed tag ─────────────────────────────────────────
# Edit exactly this one line to change the permitted MySQL image tag.
ALLOWED_TAG="8.4"

# ── Argument parsing ──────────────────────────────────────────────────────────
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --dry-run)
            DRY_RUN=true
            ;;
        --help|-h)
            printf 'Usage: %s [--dry-run]\n' "$0"
            printf '  --dry-run  Print all non-compliant mysql:<tag> occurrences without\n'
            printf '             exiting non-zero (audit / exploratory mode).\n'
            exit 0
            ;;
        *)
            printf 'ERROR: Unknown option: %s\n' "$1" >&2
            printf 'Usage: %s [--dry-run]\n' "$0" >&2
            exit 2
            ;;
    esac
    shift
done

# ── Counters ──────────────────────────────────────────────────────────────────
VIOLATIONS=0
COMPLIANT=0

# ── scan_file <filepath> ──────────────────────────────────────────────────────
# Scans one file for mysql:<tag> patterns.
# Compliant (mysql:ALLOWED_TAG) occurrences increment COMPLIANT.
# Non-compliant occurrences are printed to stdout and increment VIOLATIONS.
scan_file() {
    local file="$1"
    [ -f "$file" ] || return 0

    # -I  skip binary files silently
    # -n  prefix each matching line with its line number
    # -E  extended regex (POSIX-compatible; no -P flag used)
    # || true  prevents set -e from aborting when grep exits 1 (no match found)
    local raw
    raw=$(grep -InE 'mysql:[a-zA-Z0-9._-]+' "$file" 2>/dev/null || true)
    [ -z "$raw" ] && return 0

    while IFS= read -r match_line; do
        [ -z "$match_line" ] && continue
        local lineno="${match_line%%:*}"
        local content="${match_line#*:}"

        # Extract every mysql:<tag> token on this line (a line may contain several)
        local tokens
        tokens=$(printf '%s\n' "$content" | grep -oE 'mysql:[a-zA-Z0-9._-]+' || true)
        while IFS= read -r token; do
            [ -z "$token" ] && continue
            local tag="${token#mysql:}"
            if [ "$tag" = "$ALLOWED_TAG" ]; then
                COMPLIANT=$((COMPLIANT + 1))
            else
                printf '%s:%s: %s\n' "$file" "$lineno" "$token"
                VIOLATIONS=$((VIOLATIONS + 1))
            fi
        done <<< "$tokens"
    done <<< "$raw"
}

# ── Scan scope ────────────────────────────────────────────────────────────────

# 1. src/ — all text files, excluding db/migration and generated build output
if [ -d "src" ]; then
    while IFS= read -r f; do
        scan_file "$f"
    done < <(find src \
        -not -path '*/db/migration/*' \
        -not -path '*/build/*' \
        -not -path '*/.gradle/*' \
        -not -path '*/.git/*' \
        -type f 2>/dev/null)
fi

# 2. Docker Compose files (root and subdirectories, excluding generated dirs)
while IFS= read -r f; do
    scan_file "$f"
done < <(find . \
    -not -path './.git/*' \
    -not -path './build/*' \
    -not -path './.gradle/*' \
    -not -path './src/*' \
    \( -name 'docker-compose*.yml' -o -name 'compose*.yml' \) \
    -type f 2>/dev/null)

# 3. .github/ — workflow YAML files and action definitions
if [ -d ".github" ]; then
    while IFS= read -r f; do
        scan_file "$f"
    done < <(find .github \
        -not -path '*/.git/*' \
        -type f 2>/dev/null)
fi

# ── Summary ───────────────────────────────────────────────────────────────────
printf 'Found %d compliant mysql:%s reference(s); %d violations.\n' \
    "$COMPLIANT" "$ALLOWED_TAG" "$VIOLATIONS"

# Guard against accidental removal of all mysql: references (FR-032)
if [ "$COMPLIANT" -eq 0 ]; then
    printf 'WARNING: zero mysql:%s references found \xe2\x80\x94 possible accidental removal.\n' \
        "$ALLOWED_TAG"
    exit 1
fi

# In dry-run mode, always exit 0 regardless of violations (audit without blocking)
if [ "$DRY_RUN" = true ]; then
    exit 0
fi

# Normal mode: fail when any violation was found
if [ "$VIOLATIONS" -gt 0 ]; then
    exit 1
fi

exit 0
