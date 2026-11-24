---

description: "Task list for a change to an existing system"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story so each can be implemented and tested independently.

## Format: `[ID] [P?] [Story] Description (FR ids, work-plan item id)`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story the task belongs to (US1, US2 ...)
- Name the exact file paths of the existing repository.
- End every task with the requirement ids it implements and the work-plan item it belongs to, in parentheses:
  `(FR-003, E1-S2)`. Keep the ids as written; do not write links. Every mandatory requirement has at least one task.

<!--
  ============================================================================
  The tasks below are SAMPLES. The __SPECKIT_COMMAND_TASKS__ command MUST replace them with tasks based on the
  user stories of spec.md, the requirements and waves of plan.md, the entities of data-model.md and the contracts.
  Do not keep the samples.
  ============================================================================
-->

## Phase 1: Baseline (before any change)

**Purpose**: Capture how the system behaves today, so the change can be proved safe.

- [ ] T001 Record the build, test and startup results of the current version (FR-001)
- [ ] T002 [P] Record the responses of the interfaces that must not change (FR-002)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: What every user story depends on (toolchain, configuration, shared libraries).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 Update [toolchain or build file] to [target] (FR-003, E1-S1)

**Checkpoint**: Foundation ready - the system builds on the target.

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [What this story changes]

**Independent Test**: [How to verify this story alone]

- [ ] T004 [P] [US1] [Change] in [exact file path] (FR-004, E2-S1)
- [ ] T005 [US1] [Change] in [exact file path] (FR-004, E2-S2)

**Checkpoint**: User Story 1 works on the target and the preserved behaviour is unchanged.

---

## Phase N: Verification and Rollback

**Purpose**: Prove behaviour is preserved and the rollback works.

- [ ] T0NN Run the baseline checks on the changed system and compare them item by item (FR-002)
- [ ] T0NN Rehearse the rollback procedure of the spec (FR-005)

---

## Dependencies & Execution Order

- Phase 1 first, then Phase 2, then the user stories in priority order, then verification.
- Tasks marked [P] in one phase can run together.
