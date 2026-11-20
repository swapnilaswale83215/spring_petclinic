#!/bin/sh
# check-mysql-tag.sh — POSIX consistency check for mysql image tag
#
# Scans the repository for any mysql:<tag> reference that is not mysql:8.4.
# Exits 0 when all references are conforming; exits 1 when any violation is found.
#
# Usage:
#   sh .github/scripts/check-mysql-tag.sh
#   (or mark executable: git update-index --chmod=+x .github/scripts/check-mysql-tag.sh)
#
# Scanned paths:
#   src/                    — all files (covers *.java, *.properties per FR-015)
#   docker-compose*.yml     — Compose service definitions
#   .github/                — CI workflow files
#   **/.env                 — environment variable files (FR-016)
#   *.md, *.txt, *.gradle   — README, setup guides, build scripts
#
# Comment-line filtering (NFR-009):
#   Lines whose first non-whitespace character is '#' are skipped so that
#   shell/YAML/properties comment blocks do not produce false positives.
#   Blank lines are also skipped.
#
# Known false-positive patterns (NFR-009):
#   - Changelog or migration prose that mentions a historical tag on a
#     non-comment line (e.g. "migrated from mysql:9.7 to mysql:8.4") will
#     be flagged. Prefix such lines with '#' or move them into a comment
#     block to suppress the warning.
#   - This script cannot distinguish active image references from inline
#     documentation describing historical tags; document any such
#     occurrences in this header comment.

CANONICAL_TAG='mysql:8.4'

# Temporary file accumulates violation lines so we can count and print together.
TMPFILE=$(mktemp)
trap 'rm -f "$TMPFILE"' EXIT INT TERM

# scan_file <filepath>
# Appends violation lines to TMPFILE for any non-conforming mysql:<tag> found
# on non-blank, non-comment lines of the given file.
scan_file() {
    _sf_path="$1"
    [ -f "$_sf_path" ] || return 0
    awk -v filepath="$_sf_path" '
    {
        # Skip blank lines
        if ($0 ~ /^[[:space:]]*$/) next

        # Skip comment lines: strip leading whitespace, check first char
        stripped = $0
        sub(/^[[:space:]]+/, "", stripped)
        if (substr(stripped, 1, 1) == "#") next

        # Walk the line looking for mysql:<tag> tokens
        tmp = $0
        found = 0
        while (match(tmp, /mysql:[^[:space:]"]+/)) {
            token = substr(tmp, RSTART, RLENGTH)
            # Strip trailing punctuation that is not part of the tag
            # (e.g. a trailing comma or closing bracket in YAML/Java)
            gsub(/[,;)>]+$/, "", token)
            if (token != "mysql:8.4") {
                found = 1
                break
            }
            tmp = substr(tmp, RSTART + RLENGTH)
        }
        if (found) {
            printf "%s:%d: %s\n", filepath, NR, $0
        }
    }
    ' "$_sf_path" >> "$TMPFILE"
}

# ---------------------------------------------------------------------------
# 1. src/ — all files
# ---------------------------------------------------------------------------
if [ -d src ]; then
    find src -type f | while IFS= read -r f; do
        scan_file "$f"
    done
fi

# ---------------------------------------------------------------------------
# 2. docker-compose*.yml at repository root
# ---------------------------------------------------------------------------
for f in docker-compose*.yml; do
    scan_file "$f"
done

# ---------------------------------------------------------------------------
# 3. .github/ — all files
# ---------------------------------------------------------------------------
if [ -d .github ]; then
    find .github -type f | while IFS= read -r f; do
        scan_file "$f"
    done
fi

# ---------------------------------------------------------------------------
# 4. .env files anywhere in the repository (FR-016)
# ---------------------------------------------------------------------------
find . -name '.env' -type f | while IFS= read -r f; do
    scan_file "$f"
done

# ---------------------------------------------------------------------------
# 5. *.md, *.txt, *.gradle files (README, setup guides, build scripts)
# ---------------------------------------------------------------------------
find . -maxdepth 5 \( -name '*.md' -o -name '*.txt' -o -name '*.gradle' \) -type f \
    | while IFS= read -r f; do
        scan_file "$f"
    done

# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
if [ -s "$TMPFILE" ]; then
    cat "$TMPFILE"
fi

total=$(wc -l < "$TMPFILE" | tr -d ' 	')
printf 'Total non-conforming occurrences: %d\n' "$total"

[ "$total" -eq 0 ]
