#!/usr/bin/env python3
"""CodeRig hook I/O: one adapter for every coding agent's hook contract. Generated; do not edit.

The controls (guard.py, guide.py, verify.py) decide what to do; this module reads the agent's event
and writes the answer the way that agent understands it. Each agent's hook config calls the same
script with `--agent <name>`. Formats, from each vendor's hook reference:

  claude  stdin tool_input / prompt / session_id; block = exit 2 + stderr;
          inject = hookSpecificOutput.additionalContext; continue after Stop = exit 2 + stderr
  codex   stdin as claude (apply_patch puts the patch in tool_input.command); block = exit 2 + stderr;
          inject = plain stdout on exit 0; continue after Stop = {"decision": "block", "reason"}
  gemini  stdin tool_name / tool_input / prompt / session_id; stdout must be JSON only;
          block = exit 2 + stderr; inject = hookSpecificOutput.additionalContext (BeforeAgent);
          retry after AfterAgent = {"decision": "deny", "reason"}
  cursor  stdin tool_input.file_path (preToolUse Write) / command (beforeShellExecution) / conversation_id;
          answers are JSON: block = {"permission": "deny", "user_message", "agent_message"};
          allow = no output, so Cursor's own approval rules still apply; continue after stop =
          {"followup_message"}
  copilot camelCase events (.github/hooks/*.json): stdin toolName / toolArgs (an object or a JSON string)
          / sessionId; block = {"permissionDecision": "deny", "permissionDecisionReason"}; continue after
          agentStop = {"decision": "block", "reason"}; a prompt hook cannot add context
  devin   stdin as claude (.devin/hooks.v1.json; shell is `exec`); block = {"decision": "block", "reason"};
          inject = hookSpecificOutput.additionalContext; continue after Stop = {"decision": "block", "reason"}

Refusing a prompt (refuse_prompt): claude and devin {"decision": "block", "reason"}, cursor beforeSubmitPrompt
{"continue": false, "user_message"}, codex and gemini exit 2 + stderr; copilot cannot.
"""
from __future__ import annotations

import json
import os
import re
import sys
from pathlib import Path

AGENTS = ("claude", "codex", "gemini", "cursor", "copilot", "devin")
_PATCH_FILE = re.compile(r"^\*\*\* (?:Add|Update|Delete) File: (.+)$|^\*\*\* Move to: (.+)$", re.MULTILINE)
# Tools that only read. An agent whose hook is not narrowed by a matcher (Copilot's) sends these too, and
# a path they name is being read, not written: a blocked path stays readable, as it is for every agent.
_READ_ONLY = {"view", "read", "glob", "grep", "web_fetch", "ask_user", "task"}
ROOT = Path(__file__).resolve().parent.parent


def _set(name: str) -> bool:
    return os.getenv(name, "").strip().lower() not in ("", "0", "false", "no")


def _main_checkout(root: Path) -> Path | None:
    """The main checkout of a linked git worktree (`git worktree add`), or None. Agents that run each session
    in its own worktree (the Copilot app, Claude Code, Cursor) start from committed files only, and the
    git-ignored role.json stays in the checkout the developer connected. Read from the worktree's `.git` file
    and its `commondir`, without running git, so the hook stays fast."""
    try:
        dot_git = root / ".git"
        if not dot_git.is_file():
            return None
        line = dot_git.read_text(encoding="utf-8").strip()
        if not line.startswith("gitdir:"):
            return None
        gitdir = Path(line[len("gitdir:"):].strip())
        gitdir = gitdir if gitdir.is_absolute() else (root / gitdir)
        common = (gitdir / "commondir").read_text(encoding="utf-8").strip()
        common_dir = Path(common) if Path(common).is_absolute() else (gitdir / common)
        common_dir = common_dir.resolve()
        main = common_dir.parent if common_dir.name == ".git" else None
        return main if main and main != root.resolve() else None
    except (OSError, ValueError):
        return None


def checkouts() -> list[Path]:
    """Where this checkout's connection may live: here first, then the main checkout of a worktree."""
    main = _main_checkout(ROOT)
    return [ROOT, main] if main else [ROOT]


def role() -> str | None:
    """The role Launchpad gave this developer when they connected this checkout (or the repository a worktree
    was made from); never one the session states. .claude/settings.local.json is where a checkout connected
    before .coderig/role.json keeps it."""
    for base in checkouts():
        for path, key in ((base / ".coderig" / "role.json", "role"),
                          (base / ".claude" / "settings.local.json", "_coderig_role")):
            try:
                found = json.loads(path.read_text(encoding="utf-8")).get(key)
            except (OSError, ValueError, AttributeError):
                continue
            if isinstance(found, str) and found:
                return found
    return None


def connected() -> bool:
    """Whether `coderig connect` has run for this checkout on this machine. It writes .coderig/role.json
    (git-ignored) with the role Launchpad gave the developer, so a fresh clone or a pulled branch has none until
    its developer connects; a worktree of a connected checkout counts as connected. CI and cloud agents
    (CI / GITHUB_ACTIONS set) are held by the CI gate instead."""
    if _set("CI") or _set("GITHUB_ACTIONS"):
        return True
    for base in checkouts():
        if (base / ".coderig" / "role.json").is_file():  # connected, even with no role ({"role": null})
            return True
        try:  # where a checkout connected before .coderig/role.json keeps its role
            settings = json.loads((base / ".claude" / "settings.local.json").read_text(encoding="utf-8"))
            if settings.get("_coderig_role"):
                return True
        except (OSError, ValueError, AttributeError):
            continue
    return False


def not_connected(hint: str) -> str:
    return ("CodeRig is not set up for this repository on this machine, so this session cannot work here yet.\n"
            f"  Connect it: {hint}\n"
            "  Your role comes from your membership in the Launchpad workspace. Start a new session afterwards.")


def refuse_prompt(name: str, message: str) -> int:
    """Stop a prompt before the agent works on it (UserPromptSubmit / BeforeAgent / beforeSubmitPrompt).
    Copilot's prompt hook cannot refuse a prompt; its guard refuses the edits instead."""
    if name in ("claude", "devin"):
        print(json.dumps({"decision": "block", "reason": message}))
        return 0
    if name == "cursor":
        print(json.dumps({"continue": False, "user_message": message}))
        return 0
    if name == "copilot":
        return 0
    print(message, file=sys.stderr)  # codex, gemini: exit 2 refuses the prompt and shows why
    return 2


def _stands_down() -> bool:
    """True when this is the Claude Code copy of a hook running inside Devin, which also loads
    .claude/settings.json and sets DEVIN_PROJECT_DIR for its hooks. Devin runs CodeRig's own Devin hooks as
    well, so the Claude copy steps aside and the guard, guide and sensors run once, in Devin's format."""
    return (agent() == "claude" and bool(os.getenv("DEVIN_PROJECT_DIR"))
            and (ROOT / ".devin" / "hooks.v1.json").is_file())


def agent() -> str:
    if "--agent" in sys.argv:
        position = sys.argv.index("--agent")
        if position + 1 < len(sys.argv) and sys.argv[position + 1] in AGENTS:
            return sys.argv[position + 1]
    return "claude"


def read_event() -> dict:
    """The agent's hook payload as {event, tool, paths, command, prompt, session}; empty on bad input."""
    # Bytes, as UTF-8: on Windows text-mode stdin is decoded with the console's code page, and a payload
    # piped from PowerShell starts with a byte-order mark that json.loads rejects.
    stream = getattr(sys.stdin, "buffer", None)
    try:
        text = stream.read().decode("utf-8-sig", errors="replace") if stream else sys.stdin.read()
        raw = json.loads(text or "{}")
    except ValueError:
        raw = {}
    if not isinstance(raw, dict):
        raw = {}
    if _stands_down():
        sys.exit(0)
    tool = str(raw.get("tool_name") or raw.get("toolName") or "")
    # Copilot's camelCase payload puts the arguments in toolArgs
    tool_input = raw.get("tool_input") or raw.get("toolArgs") or {}
    if isinstance(tool_input, str):  # Cursor's beforeMCPExecution and Copilot can send a JSON string
        try:
            tool_input = json.loads(tool_input)
        except ValueError:
            tool_input = {}
    if not isinstance(tool_input, dict):
        tool_input = {}

    paths: list[str] = []
    for key in ("file_path", "path", "notebook_path", "absolute_path"):
        value = tool_input.get(key)
        if isinstance(value, str):
            paths.append(value)
    for edit in tool_input.get("edits") or []:
        if isinstance(edit, dict) and isinstance(edit.get("file_path"), str):
            paths.append(edit["file_path"])
    if isinstance(raw.get("file_path"), str):  # Cursor: beforeReadFile / afterFileEdit
        paths.append(raw["file_path"])

    command = raw.get("command") if isinstance(raw.get("command"), str) else tool_input.get("command")
    command = command if isinstance(command, str) else ""
    # apply_patch: the files are in the patch headers (Codex puts the patch in `command`, Devin may not)
    patches = [v for v in [command, *tool_input.values()] if isinstance(v, str) and v.startswith("*** Begin Patch")]
    for patch in patches:
        paths += [a or b for a, b in _PATCH_FILE.findall(patch)]
    if command.startswith("*** Begin Patch"):
        command = ""
    if tool.lower() in _READ_ONLY:
        paths, command = [], ""

    return {
        "event": str(raw.get("hook_event_name") or ""),
        "tool": tool,
        "paths": [p.strip() for p in dict.fromkeys(paths) if p and p.strip()],
        "command": command,
        "prompt": str(raw.get("prompt") or ""),
        "session": str(raw.get("session_id") or raw.get("sessionId") or raw.get("conversation_id") or ""),
    }


def block(name: str, message: str) -> int:
    """Refuse the tool call. Returns the exit code to leave with."""
    if name == "cursor":
        print(json.dumps({"permission": "deny", "user_message": message, "agent_message": message}))
        return 0
    if name == "copilot":
        print(json.dumps({"permissionDecision": "deny", "permissionDecisionReason": message}))
        return 0
    if name == "devin":
        print(json.dumps({"decision": "block", "reason": message}))
        return 0
    print(message, file=sys.stderr)
    return 2


def inject(name: str, text: str) -> int:
    """Add text to the agent's context for this turn (UserPromptSubmit / BeforeAgent)."""
    if name == "codex":
        print(text)
    elif name == "gemini":
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "BeforeAgent", "additionalContext": text}}))
    elif name in ("claude", "devin"):
        print(json.dumps({"hookSpecificOutput": {"hookEventName": "UserPromptSubmit", "additionalContext": text}}))
    return 0  # cursor and copilot have no per-prompt injection; nothing is printed


def feedback(name: str, text: str) -> int:
    """Send the agent back to work after a failed finish (Stop / AfterAgent / stop / agentStop)."""
    if name in ("codex", "copilot", "devin"):
        print(json.dumps({"decision": "block", "reason": text}))
        return 0
    if name == "gemini":
        print(json.dumps({"decision": "deny", "reason": text}))
        return 0
    if name == "cursor":
        print(json.dumps({"followup_message": text}))
        return 0
    print(text, file=sys.stderr)
    return 2


def notice(name: str, text: str) -> int:
    """Tell the developer something without blocking (checks could not run, attempts used up)."""
    if name in ("claude", "codex", "gemini"):
        print(json.dumps({"systemMessage": text}))
    return 0
