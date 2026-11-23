#!/usr/bin/env python3
"""CodeRig checks: secret scan and no-bypass, over the lines this change adds. Generated; do not edit.

Usage (run from the repository root, as the sensor runner does):
    python .coderig/checks.py secret-scan
    python .coderig/checks.py no-bypass
    python .coderig/checks.py dependency-policy

Exit 0 passes. Exit 1 lists each finding as file:line with the rule name. It never prints the
matched value, so the output cannot leak the secret it found. Exit 2 is a usage error.

Scope: the lines this change adds, i.e. the working tree against HEAD plus untracked files. Work
that is already committed is not re-checked here; CI runs the same rules over the whole branch.

Standard library only, so it runs on a developer laptop or in CI without installing anything.
"""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path.cwd()

# Matched against each added line. The rule name is what the developer sees; the value is never shown.
SECRETS = [
    ("private key", re.compile(r"-----BEGIN (RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----")),
    ("AWS access key", re.compile(r"\b(AKIA|ASIA)[0-9A-Z]{16}\b")),
    ("GitHub token", re.compile(r"\b(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{36,}\b|\bgithub_pat_[A-Za-z0-9_]{60,}\b")),
    ("Slack token", re.compile(r"\bxox[abprs]-[A-Za-z0-9-]{10,}")),
    ("JSON web token", re.compile(r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}")),
    ("credential assignment", re.compile(
        r"""(?i)\b(password|passwd|pwd|pw|secret|api[_-]?key|access[_-]?token|auth[_-]?token|"""
        r"""client[_-]?secret|connection[_-]?string)\b\s*[:=]\s*["']([^"'\s$<{]{8,})["']""")),
    # .env and properties style: DB_PASSWORD=..., spring.datasource.password: ..., export API_KEY=...
    # A whole line that assigns a literal, so `password = getPassword()` and `pw = user.pw;` are not findings.
    ("credential setting", re.compile(
        r"""(?i)^\s*(export\s+)?[a-z0-9_.-]*(password|passwd|secret|api[_-]?key|access[_-]?key|"""
        r"""access[_-]?token|auth[_-]?token|client[_-]?secret|private[_-]?key)[a-z0-9_.-]*\s*[:=]\s*"""
        r"""["']?([^\s"'$<{#()\[\];,]{8,})["']?\s*$""")),
]

# A line that reads its value from the environment, or says it is a placeholder, is not a finding.
PLACEHOLDER = re.compile(
    r"(?i)example|changeme|placeholder|dummy|xxxx|\*\*\*|<[^>]*>|\$\{|process\.env|os\.environ|getenv"
    r"|your[_-]"
)

BYPASS = [
    ("disabled test", re.compile(
        r"@Disabled\b|@Ignore\b|\bxit\(|\bxdescribe\(|\.skip\(|pytest\.mark\.skip|@unittest\.skip")),
    ("lint or type suppression", re.compile(
        r"eslint-disable|@ts-ignore|@ts-nocheck|@SuppressWarnings\(|#\s*noqa\b|#\s*type:\s*ignore|"
        r"NOSONAR|checkstyle:off|//\s*nolint")),
    ("skipped verification", re.compile(
        r"--no-verify|-DskipTests|-Dcheckstyle\.skip|-Dspring-javaformat\.skip|HUSKY=0")),
]

# CodeRig's own files describe these patterns ("never use --no-verify"), so they are never scanned.
# Keep in step with IGNORED_PATHS in coderig/emit/verify.py: a generated CLAUDE.md or skill that is
# not committed yet would otherwise fail no-bypass on every stop, and the change could never pass.
SKIPPED_PREFIXES = (".coderig/", ".claude/", ".cursor/", ".codex/", ".gemini/", ".vscode/mcp.json",
                    ".github/instructions/", ".github/copilot-instructions.md", "CLAUDE.md", "AGENTS.md",
                    "GEMINI.md", ".mcp.json", ".github/workflows/coderig-gate.yml")
SKIPPED_SUFFIXES = (".lock", ".min.js", ".map")


def git(*args: str) -> str:
    done = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True,
                          encoding="utf-8", errors="replace")
    return done.stdout


def added_lines() -> list[tuple[str, int, str]]:
    """(path, line number, text) for every line this change adds."""
    found: list[tuple[str, int, str]] = []
    path: str | None = None
    line_no = 0
    for raw in git("diff", "-U0", "HEAD").splitlines():
        if raw.startswith("+++ "):
            name = raw[4:]
            path = None if name == "/dev/null" else name.removeprefix("b/")
        elif raw.startswith("@@"):
            match = re.search(r"\+(\d+)", raw)
            line_no = int(match.group(1)) if match else 0
        elif raw.startswith("+") and path is not None:
            found.append((path, line_no, raw[1:]))
            line_no += 1
    for rel in git("ls-files", "--others", "--exclude-standard").splitlines():
        try:
            text = (ROOT / rel).read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue  # binary or unreadable: nothing a text pattern can judge
        for number, line in enumerate(text.splitlines(), 1):
            found.append((rel, number, line))
    return found


APPROVED_FILE = ROOT / "approved-dependencies.txt"


def _local(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def pom_dependencies(text: str) -> set[str]:
    """groupId:artifactId of every dependency a pom declares, in <dependencies> or <dependencyManagement>."""
    root = ET.fromstring(text)
    parent = {child: node for node in root.iter() for child in node}
    found = set()
    for element in root.iter():
        if _local(element.tag) != "dependency" or _local(parent[element].tag) != "dependencies":
            continue
        values = {_local(c.tag): (c.text or "").strip() for c in element}
        if values.get("groupId") and values.get("artifactId"):
            found.add(f"{values['groupId']}:{values['artifactId']}")
    return found


def approved_list() -> list[str] | None:
    """Lines of approved-dependencies.txt: groupId:artifactId, or groupId:* for a whole group."""
    if not APPROVED_FILE.is_file():
        return None
    lines = (line.split("#", 1)[0].strip() for line in APPROVED_FILE.read_text(encoding="utf-8").splitlines())
    return [line for line in lines if line]


def is_approved(coordinate: str, approved: list[str]) -> bool:
    group = coordinate.split(":", 1)[0]
    return coordinate in approved or f"{group}:*" in approved


def changed_poms() -> list[str]:
    changed = git("diff", "--name-only", "HEAD").splitlines()
    changed += git("ls-files", "--others", "--exclude-standard").splitlines()
    return sorted({p for p in changed if p == "pom.xml" or p.endswith("/pom.xml")})


def base_pom(rel: str) -> str:
    """The pom as committed; empty when the file is new."""
    done = subprocess.run(["git", "show", f"HEAD:{rel}"], cwd=ROOT, capture_output=True,
                          text=True, encoding="utf-8", errors="replace")
    return done.stdout if done.returncode == 0 else ""


def check_dependencies() -> list[str]:
    """Each dependency this change adds to a pom must be on the approved list. A version change to an
    approved dependency is not checked here. Fails closed when the list is missing and a pom adds one."""
    findings = []
    added: list[tuple[str, str]] = []
    for rel in changed_poms():
        try:
            now = pom_dependencies((ROOT / rel).read_text(encoding="utf-8"))
        except (OSError, ET.ParseError):
            findings.append(f"{rel}: cannot be read or parsed as XML")
            continue
        try:
            before = pom_dependencies(base_pom(rel)) if base_pom(rel) else set()
        except ET.ParseError:
            before = set()
        added += [(rel, coordinate) for coordinate in sorted(now - before)]
    if not added:
        return findings
    approved = approved_list()
    if approved is None:
        findings.append("approved-dependencies.txt is missing, so new dependencies cannot be judged. "
                        "The project must list the approved libraries first.")
        return findings
    for rel, coordinate in added:
        if not is_approved(coordinate, approved):
            findings.append(f"{rel}: new dependency {coordinate} is not on the approved list")
    return findings


def check(kind: str) -> list[str]:
    if kind == "dependency-policy":
        return check_dependencies()
    rules = SECRETS if kind == "secret-scan" else BYPASS
    findings = []
    for path, number, text in added_lines():
        if path.startswith(SKIPPED_PREFIXES) or path.endswith(SKIPPED_SUFFIXES):
            continue
        if kind == "secret-scan" and PLACEHOLDER.search(text):
            continue
        for name, pattern in rules:
            if pattern.search(text):
                findings.append(f"{path}:{number}: {name} (value not shown)")
    return findings


def main() -> int:
    kinds = ("secret-scan", "no-bypass", "dependency-policy")
    if len(sys.argv) != 2 or sys.argv[1] not in kinds:
        print("usage: python .coderig/checks.py " + " | ".join(kinds), file=sys.stderr)
        return 2
    findings = check(sys.argv[1])
    if not findings:
        print(f"{sys.argv[1]}: no findings in this change")
        return 0
    print("\n".join(findings))
    print(f"{len(findings)} finding(s). Fix each one. Do not add a suppression or an exception "
          "to get a green result.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
