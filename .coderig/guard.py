#!/usr/bin/env python3
"""CodeRig guard — refuses edits to blocked paths, and a likely secret on its way into the repository.
Generated; do not edit.

Run by each agent's pre-tool hook with --agent <name> (Claude Code, Codex and Devin PreToolUse,
Gemini CLI BeforeTool, Cursor preToolUse / beforeShellExecution, Copilot preToolUse). hookio.py reads that agent's
payload and answers in its format. Any unexpected failure allows the call: a broken guard must
never wedge a developer's session, because CI still enforces the same rules.

Secrets: a file git ignores (`git check-ignore`) is local configuration, such as a developer's .env, and may hold
real values. A likely secret written to any other file in the repository would be committed, so the workspace's
secret policy decides: block (refuse the write), warn (allow it and tell the developer) or off.

Regenerate with: coderig connect
"""
from __future__ import annotations

import fnmatch
import json
import re
import shlex
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hookio  # noqa: E402  (generated next to this file)

BLOCKED = json.loads(r'''[
  ".github/workflows/**",
  "**/auth/**",
  "**/Auth/**",
  "**/security/**",
  "**/Security/**",
  "**/*Authentication*",
  "**/*Authorization*",
  "**/*.pem",
  "**/*.p12",
  "**/*.pfx",
  "**/id_rsa",
  "**/credentials"
]''')
REASONS = json.loads(r'''{
  ".github/workflows/**": "CI config defines the gates; agents must not edit it",
  "**/*.pem": "Private key material \u2014 never touch",
  "**/id_rsa": "Private key material \u2014 never touch"
}''')
SECRET_RULES = [(name, re.compile(rule)) for name, rule in json.loads(r'''[["private key", "-----BEGIN (RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----"], ["AWS access key", "\\b(AKIA|ASIA)[0-9A-Z]{16}\\b"], ["GitHub token", "\\b(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{36,}\\b|\\bgithub_pat_[A-Za-z0-9_]{60,}\\b"], ["Slack token", "\\bxox[abprs]-[A-Za-z0-9-]{10,}"], ["JSON web token", "\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}"], ["credential assignment", "(?i)\\b(password|passwd|pwd|pw|secret|api[_-]?key|access[_-]?token|auth[_-]?token|client[_-]?secret|connection[_-]?string)\\b\\s*[:=]\\s*[\"']([^\"'\\s$<{]{8,})[\"']"], ["credential setting", "(?i)^\\s*(export\\s+)?[a-z0-9_.-]*(password|passwd|secret|api[_-]?key|access[_-]?key|access[_-]?token|auth[_-]?token|client[_-]?secret|private[_-]?key)[a-z0-9_.-]*\\s*[:=]\\s*[\"']?([^\\s\"'$<{#()\\[\\];,]{8,})[\"']?\\s*$"]]''')]
PLACEHOLDER = re.compile(json.loads(r'''"(?i)example|changeme|placeholder|dummy|xxxx|\\*\\*\\*|<[^>]*>|\\$\\{|process\\.env|os\\.environ|getenv|your[_-]"'''))
SECRETS = json.loads(r'''{"mode": "block", "local_files": ["**/.env", "**/.env.*", ".env", ".env.*"]}''')
# Role -> paths that role may write. The role is the one CodeRig connected this
# checkout with (.coderig/role.json, for every agent). Enforced here rather than as
# Claude Code permission rules, where a broad deny always beats a narrow allow.
ROLE_SCOPE = json.loads(r'''{
  "developer": [
    "src/**",
    "tests/**",
    "docs/**"
  ],
  "tech-lead": [
    "src/**",
    "tests/**",
    "docs/**",
    "*.yaml",
    "*.yml",
    "*.toml",
    "*.json"
  ],
  "qa": [
    "tests/**",
    "docs/**"
  ],
  "ba": [
    "docs/**"
  ],
  "product-owner": [
    "docs/**"
  ]
}''')
# How to connect this checkout, for a developer who has not yet (hookio.connected).
CONNECT = json.loads(r'''"coderig connect --launchpad http://localhost:8000 --workspace proj-d9b4cf --agents copilot  (or Connect agents in the CodeRig app)"''')


def current_role(root: Path):
    # hookio.role also finds the role of the checkout a git worktree was made from
    return hookio.role()


def in_scope(rel: str, patterns: list) -> bool:
    return any(fnmatch.fnmatch(rel, p) for p in patterns)


WRITES = re.compile(
    r"(^|[\s;&|(])(rm|mv|cp|tee|truncate|touch|chmod|chown|ln|install|dd|rsync|unlink|del|copy|move)\b"
    r"|>|\bsed\s+-i|\bperl\s+-pi|\bgit\s+(checkout|restore|rm|mv|apply)\b"
    r"|Set-Content|Add-Content|Out-File|Remove-Item|Move-Item|Copy-Item|New-Item")


def command_paths(command: str) -> list[str]:
    """Path-like words of a shell command that looks like it writes. Only the blocked patterns are
    checked against these: a command that would change `.env` or a CI file is refused, but a read
    (`cat`, `grep`) is not, and a role's write scope is not judged from a command line, which
    cannot say reliably what it will write."""
    if not WRITES.search(command):
        return []
    words = command_words(command)
    found = []
    for word in words:
        for part in word.replace(">", " ").replace("<", " ").replace("=", " ").split():
            if "/" in part or "." in part:
                found.append(part.strip("'\"`;|&()"))
    return [p for p in found if p]


def normalise(raw: str, root: Path) -> str:
    path = Path(raw)
    try:
        return path.resolve().relative_to(root).as_posix()
    except (ValueError, OSError):
        # str.lstrip() strips CHARACTERS, not a prefix. Using it here turned
        # ".github/workflows/ci.yml" into "github/workflows/ci.yml" and
        # ".env" into "env", so every dotfile-rooted blocked pattern silently
        # stopped matching — including the CI and secrets rules.
        text = path.as_posix()
        while text.startswith("./"):
            text = text[2:]
        return text


def blocked_by(rel: str):
    for pattern in BLOCKED:
        if fnmatch.fnmatch(rel, pattern) or fnmatch.fnmatch(f"./{rel}", pattern):
            return pattern
    return None


def command_words(command: str) -> list[str]:
    try:
        return shlex.split(command, posix=True)
    except ValueError:
        return command.split()


def secrets_in(text: str) -> list:
    """The rule names a likely secret in this text matches. Never the value."""
    found = []
    for line in text.splitlines():
        if PLACEHOLDER.search(line):
            continue
        found += [name for name, rule in SECRET_RULES if name not in found and rule.search(line)]
    return found


def git_ignored(root: Path, rel: str) -> bool:
    """Whether git ignores this path, so what is written there never reaches the repository. A tracked file is
    never ignored, whatever .gitignore says."""
    try:
        done = subprocess.run(["git", "check-ignore", "-q", "--", rel], cwd=root, capture_output=True, timeout=5)
    except (OSError, subprocess.SubprocessError):
        return False
    return done.returncode == 0


def secret_problem(root: Path, rel: str, text: str):
    """The message for a likely secret written to a file that would be committed, else None."""
    if SECRETS.get("mode", "block") == "off" or not text or Path(rel).is_absolute() or rel.startswith("../"):
        return None                     # outside the repository: never committed from here
    names = secrets_in(text)
    if not names or git_ignored(root, rel):
        return None
    if in_scope(rel, SECRETS.get("local_files") or []):
        why = "a local configuration file git does not ignore, so it would be committed"
        instead = ("add it to .gitignore and keep real values there; a committed template such as .env.example "
                   "holds names and placeholders only")
    else:
        why = "a file git tracks or would commit"
        instead = ("read the value from the environment or the secret store; real values belong only in a "
                   "git-ignored local file such as .env")
    return (f"{rel}\n"
            f"  found:   a likely secret ({', '.join(names)})\n"
            f"  why:     {why}\n"
            f"  instead: {instead}")


def refuse(agent: str, rel: str, pattern: str) -> int:
    reason = REASONS.get(pattern, "blocked by the repository risk matrix")
    return hookio.block(agent,
        f"BLOCKED by CodeRig: {rel}\n"
        f"  matched: {pattern}\n"
        f"  reason:  {reason}\n"
        f"  This change needs a human. Stop and tell the developer "
        f"rather than working around it.")


def main() -> int:
    agent = hookio.agent()
    event = hookio.read_event()

    # The repository is wherever this script lives (.coderig/ is one level down), not the
    # agent's current directory: an agent that has `cd`-ed into a subfolder would otherwise
    # make every blocked pattern fail to match.
    root = Path(__file__).resolve().parent.parent
    role = current_role(root)
    scope = ROLE_SCOPE.get(role) if role else None
    # Cursor's beforeReadFile names a file that is about to be read, not written.
    writing = event["event"] != "beforeReadFile"
    # Not connected on this machine: no edits and no commands until it is (reads stay allowed). Copilot, whose
    # prompt hook cannot refuse a prompt, is stopped here; the other agents' prompt hook stops them sooner.
    if not hookio.connected() and ((writing and event["paths"]) or event["command"]):
        return hookio.block(agent, hookio.not_connected(CONNECT))
    secret_problems = []
    for raw in event["paths"]:
        rel = normalise(raw, root)
        # A blocked path is refused for every role, so its reason is the one to give, before the role's scope.
        pattern = blocked_by(rel)
        if pattern:
            return refuse(agent, rel, pattern)
        if writing and scope is not None and scope != ["**"] and not in_scope(rel, scope):
            allowed = ", ".join(scope) if scope else "nothing (read-only role)"
            return hookio.block(agent,
                f"BLOCKED by CodeRig: {rel}\n"
                f"  role:    {role} may write only: {allowed}\n"
                f"  Stop and tell the developer; this role cannot make this change.")
        problem = secret_problem(root, rel, event.get("content") or "") if writing else None
        if problem:
            secret_problems.append(problem)
    # A command's secret is in one of its words (`echo "API_KEY=..." > config.yml`), not on a line of its own.
    command_text = "\n".join([event["command"], *command_words(event["command"])]) if event["command"] else ""
    for raw in command_paths(event["command"]):
        rel = normalise(raw, root)
        pattern = blocked_by(rel)
        if pattern:
            return refuse(agent, rel, pattern)
        problem = secret_problem(root, rel, command_text)
        if problem:
            secret_problems.append(problem)
    if secret_problems:
        if SECRETS.get("mode", "block") == "block":
            return hookio.block(agent, "BLOCKED by CodeRig: " + "\n".join(secret_problems)
                                + "\n  Stop and tell the developer rather than working around it.")
        return hookio.notice(agent, "CodeRig warning (the secret policy allows this write): "
                             + "\n".join(secret_problems))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception:  # never wedge the session; CI is the real gate
        sys.exit(0)
