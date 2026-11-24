# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Change specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `__SPECKIT_COMMAND_PLAN__` command; its definition describes the execution workflow.

<!--
  Conventions (the publisher relies on them):
  - Keep the ids of the spec and of the approved work plan (FR-001, SPEC-APP-2, E1-S2 ...) as written.
  - End each decision and each wave with the ids it serves, in parentheses. Do not write links.
-->

## Summary

[Primary requirement of the change and the technical approach, from research]

## Technical Context

**Current Language/Version**: [what runs today, or NEEDS CLARIFICATION]

**Target Language/Version**: [what runs after the change]

**Primary Dependencies**: [libraries and frameworks, with the versions that change]

**Storage**: [databases and files the change touches, or N/A]

**Testing**: [existing test suites and how they are run; the regression checks]

**Target Platform**: [runtime and deployment]

**Constraints**: [compatibility, downtime, performance, compliance]

**Scale/Scope**: [modules, services, endpoints, data volumes in scope]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Each gate of the constitution, and whether the change meets it]

## Tracks

How each architecture track is affected (application, data, integration, platform, security): what changes, what
stays, the spec ids it serves. Leave out a track the change does not touch.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (__SPECKIT_COMMAND_PLAN__ command output)
├── research.md          # Phase 0 output (__SPECKIT_COMMAND_PLAN__ command)
├── data-model.md        # Phase 1 output (__SPECKIT_COMMAND_PLAN__ command)
├── quickstart.md        # Phase 1 output (__SPECKIT_COMMAND_PLAN__ command)
├── contracts/           # Phase 1 output (__SPECKIT_COMMAND_PLAN__ command)
└── tasks.md             # Phase 2 output (__SPECKIT_COMMAND_TASKS__ command - NOT created by __SPECKIT_COMMAND_PLAN__)
```

### Source Code (existing repository)

Describe the repository layout as it is, and mark the paths the change touches. Do not propose a new project
structure.

**Structure Decision**: [the directories the change works in]

## Migration Waves

Ordered waves of the work plan, each with its goal, the work items it holds, and the check that lets the next wave
start.

| Wave | Goal | Items | Exit check |
|------|------|-------|------------|
| [W1] | [goal] | [item ids] | [check] |

## Rollback

How each wave is undone, and the point after which rollback is no longer possible.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., two versions in parallel] | [current need] | [why a single cut-over is not possible] |
