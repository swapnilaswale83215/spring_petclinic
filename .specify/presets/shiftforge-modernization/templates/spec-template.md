# Change Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`

**Created**: [DATE]

**Status**: Draft

**Input**: User description: "$ARGUMENTS"

<!--
  This template specifies a CHANGE to an existing system, not a new product.
  Conventions (the publisher relies on them):
  - Keep every id the context gives you exactly as written (KPI-1, AC-3, SPEC-APP-2, ADR-DATA-1 ...).
  - End each requirement, scenario and success criterion with the ids it comes from, in parentheses:
    "(AC-3, SPEC-APP-2)". Do not write links or file names: the publisher adds a Sources line with the links.
  - Never invent an id. A statement the context does not settle is marked [NEEDS CLARIFICATION: ...].
-->

## Current State *(mandatory)*

What the system is and does today, in the words of the repository and its baseline specs: technology and versions,
the modules the change touches, the interfaces other systems depend on.

## Target State *(mandatory)*

What is true when the change is done: the technology and versions, what changes, and what deliberately does not.

## Behaviour That Must Be Preserved *(mandatory)*

Behaviour users and other systems rely on today that stays exactly as it is, each with how it is checked.

- [Behaviour, e.g. "GET /owners/find returns the same fields"] — verified by [test or regression check] ([source ids])

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Brief Title] (Priority: P1)

[Who is affected and what they see change or not change]

**Why this priority**: [Value and risk]

**Independent Test**: [How this story is tested on its own]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome] ([source ids])

---

### User Story 2 - [Brief Title] (Priority: P2)

[Description]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome] ([source ids])

---

### Edge Cases

- What happens to [data, configuration or integration] that exists today when [boundary condition]?
- How does the system behave during the change (mixed versions, partial rollout)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST [specific capability of the change] ([source ids])
- **FR-002**: System MUST [specific capability of the change] ([source ids])
- **FR-003**: System MUST keep [behaviour that must not break] ([source ids])

Every requirement cites at least one source id; mark what the context does not settle:

- **FR-004**: System MUST [capability] [NEEDS CLARIFICATION: what is missing]

### Key Entities *(include if the change touches data)*

- **[Entity]**: [What it is today, what changes, what stays]

## Rollback *(mandatory)*

How the change is undone if it fails: the trigger, the steps, the data that needs restoring, and what is lost.

- **Trigger**: [what makes the team roll back]
- **Procedure**: [ordered steps]
- **Verification after rollback**: [what proves the system is back to the current state]

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: [Measurable outcome with its target and baseline] ([KPI id])
- **SC-002**: [Measurable outcome] ([KPI id])

## Assumptions

- [Assumption about scope, environment or dependencies]
