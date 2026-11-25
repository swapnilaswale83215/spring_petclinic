**Generated-at**: 2026-11-25T06:49:34Z
**Baseline-commit**: unknown
# Feature Specification: Visits for Spring PetClinic

**Feature Branch**: `004-visits-spring-petclinic`

**Status**: Draft

**Input**: User description: "Visits for spring_petclinic"

---

## Business Rules

### BR-VISITS-001 — Visit date must be in the future

Rule:
A visit date must be at least one calendar day ahead of today. Submitting `LocalDate.now()` as the visit date is rejected with error code `typeMismatch.visitDate` on the `date` field.

Source:
- `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java`
- Quoted evidence: `typeMismatch.visitDate` error code asserted when `LocalDate.now()` is submitted

Status: Verified
---

### BR-VISITS-002 — Visit description must not exceed 255 characters

Rule:
A visit description is limited to 255 characters. Submitting a description of 256 characters causes a field error on the `description` field.

Source:
- `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java`
- Quoted evidence: field error on `description` asserted for a 256-character input

Status: Verified
---

### BR-VISITS-003 — Identity fields are disallowed from form binding

Rule:
The `id` field and any nested `*.id` fields are registered as disallowed fields on the `WebDataBinder`, preventing them from being set via HTTP request parameters.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java`
- Quoted evidence: disallowed fields registered on `WebDataBinder` for `id` and `*.id`

Status: Verified
---

### BR-VISITS-004 — Owner must exist before a visit can be created

Rule:
A visit can only be created for an owner that exists in the repository. If no `Owner` is found for the given `ownerId`, an `IllegalArgumentException` is thrown and the operation is aborted.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java`
- Quoted evidence: `IllegalArgumentException` thrown when owner lookup by `ownerId` returns no result

Status: Verified
---

### BR-VISITS-005 — Pet must belong to the resolved owner

Rule:
A visit can only be created for a pet that belongs to the resolved owner. If the pet with the given `petId` is not found under that owner, an `IllegalArgumentException` is thrown.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java`
- Quoted evidence: `IllegalArgumentException` thrown when `owner.getPet(petId)` returns null

Status: Verified
---

### BR-VISITS-006 — A visit must be attached to exactly one pet before form render or processing

Rule:
The `Visit` object is created and attached to the pet via `pet.addVisit(visit)` before the form is rendered or the POST is processed. A visit is never persisted independently of its owning pet.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java`
- Quoted evidence: `pet.addVisit(visit)` called in the model-population path before form render and before save

Status: Verified
---

### BR-VISITS-007 — Owner.addVisit arguments must be non-null and pet must exist on owner

Rule:
When adding a visit via `Owner.addVisit(petId, visit)`, both `petId` and `visit` must be non-null, and the resolved pet must exist on the owner. Any violation throws an `IllegalArgumentException`.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/Owner.java`
- Quoted evidence: `IllegalArgumentException` thrown for null `petId`, null `visit`, or unresolvable pet

Status: Verified
---

### BR-VISITS-008 — Visit records are append-only; deletion is not permitted

Rule:
Visit history is append-only. No delete operation is exposed on the visit collection. This is a non-negotiable constraint stated in the project constitution.

Source:
- `.specify/memory/constitution.md` — "Visit records MUST NOT be deleted once created; visit history MUST be append-only."
Status: Inferred

_Note: demoted from Verified — cited source was not quoted from the read context._
---

### BR-VISITS-009 — Visit is persisted through the owner aggregate

Rule:
A new visit is persisted by calling `this.owners.save(owner)` after the visit has been appended to the pet. There is no separate visit repository save call.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java`
- Quoted evidence: `this.owners.save(owner)` used to persist the visit through the owner aggregate root

Status: Verified
---

### BR-VISITS-010 — Visit date defaults to the next calendar day on construction

Rule:
When a `Visit` object is constructed, its `date` field defaults to `LocalDate.now().plusDays(1)`, pre-populating the form with the next available day.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/Visit.java`
- Quoted evidence: `date` field initialised to `LocalDate.now().plusDays(1)` in the `Visit` constructor

Status: Verified
---

## User Scenarios & Testing

### User Story 1 — Record a new visit for an existing pet (Priority: P1)

A clinic owner or staff member navigates to a pet's profile, opens the new-visit form, enters a future date and a description of up to 255 characters, and submits. The visit is saved and immediately visible in the pet's visit history. The user is redirected to the owner's profile page after a successful submission.

**Why this priority**: This is the core write path of the Visits feature. Without it, no visit data can enter the system. All other stories depend on visits existing.

**Independent Test**: Can be fully tested by submitting a valid visit form for an existing owner/pet pair and confirming the visit appears on the owner detail page and is persisted in the `visits` table.

**Acceptance Scenarios**:

1. **Given** an owner exists and has at least one pet registered, **When** the user submits a visit form with a date of `today + 1` and a description of 50 characters, **Then** the visit is saved, linked to the correct pet, and the user is redirected to `/owners/{ownerId}`.
2. **Given** a valid visit form, **When** the user submits a description of exactly 255 characters, **Then** the visit is accepted and persisted without error.
3. **Given** a valid visit form, **When** the user submits a description of 256 characters, **Then** the form is re-displayed with a field error on `description` and no visit is persisted.
4. **Given** a valid visit form, **When** the user submits `LocalDate.now()` as the visit date, **Then** the form is re-displayed with a `typeMismatch.visitDate` error on `date` and no visit is persisted.

---

### User Story 2 — View all visits for a pet (Priority: P2)

A clinic owner or staff member views a pet's profile and sees the complete, ordered visit history — each entry showing the visit date and description. No visit entry can be removed from this history.

**Why this priority**: Visit history is the primary read surface of the feature. It delivers direct value to clinic staff reviewing a pet's care record, and it validates that the write path (Story 1) is working correctly end-to-end.

**Independent Test**: Can be fully tested by seeding one or more visits for a pet (via the write path or test fixtures) and confirming all visits appear on the owner detail view with correct dates and descriptions.

**Acceptance Scenarios**:

1. **Given** a pet has two visits recorded on different dates, **When** the owner detail page is loaded, **Then** both visits are displayed with their respective dates and descriptions.
2. **Given** a pet has no visits recorded, **When** the owner detail page is loaded, **Then** the visit history section is empty and no error is shown.
3. **Given** a visit has been persisted, **When** any user attempts to delete it (e.g., via a crafted HTTP request), **Then** no delete endpoint exists and the visit record remains intact.

---

### User Story 3 — Reject visit creation for an invalid owner or pet (Priority: P3)

When a visit form submission references an owner ID that does not exist, or a pet ID that does not belong to the resolved owner, the system rejects the request immediately. No partial visit record is created.

**Why this priority**: This is a guard-rail story. The happy path (Story 1) already requires a valid owner and pet; this story explicitly validates the failure paths that protect data integrity.

**Independent Test**: Can be fully tested by issuing visit form submissions with a non-existent `ownerId` or a `petId` that belongs to a different owner, and confirming that an `IllegalArgumentException` is raised and no visit row appears in the `visits` table.

**Acceptance Scenarios**:

1. **Given** no owner exists for the supplied `ownerId`, **When** a visit form submission is attempted, **Then** an `IllegalArgumentException` is thrown and no visit is persisted.
2. **Given** an owner exists but the supplied `petId` does not belong to that owner, **When** a visit form submission is attempted, **Then** an `IllegalArgumentException` is thrown and no visit is persisted.
3. **Given** a form submission attempts to set the `id` or any `*.id` field via request parameters, **When** the form is bound, **Then** those fields are silently ignored and the bound object retains its server-assigned identity values.

---

### Edge Cases

- What happens when `Owner.addVisit(petId, visit)` is called with a null `petId`? → `IllegalArgumentException` is thrown (BR-VISITS-007).
- What happens when `Owner.addVisit(petId, visit)` is called with a null `visit`? → `IllegalArgumentException` is thrown (BR-VISITS-007).
- What happens when the visit date is exactly `LocalDate.now()` (today)? → The form is rejected with `typeMismatch.visitDate` on the `date` field (BR-VISITS-001).
- What happens when the visit date is in the past? → Governed by the same future-date constraint as today; the date field validation rejects it (BR-VISITS-001).
- What happens when the description is blank or empty? → `@NotBlank` on the `Visit.description` field causes a bean-validation error; the form is re-displayed.
- What happens when `owners.save(owner)` fails (e.g., database unavailable)? → The persistence layer propagates the exception; no visit is committed. No compensating rollback logic is visible in the controller beyond standard Spring transaction semantics.
- What happens when the same visit object is added to a pet twice? → Not addressed by any cited source; behaviour is unspecified and requires confirmation.

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST associate every new Visit with a valid Owner and Pet; if the Owner ID cannot be resolved in the repository, the system MUST throw an `IllegalArgumentException` and abort the operation.
  *Source: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — owner lookup guard*

- **FR-002**: System MUST associate every new Visit with a Pet that belongs to the resolved Owner; if the Pet ID is not found under that Owner, the system MUST throw an `IllegalArgumentException` and abort the operation.
  *Source: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `owner.getPet(petId)` null check*

- **FR-003**: System MUST reject any visit date that is not at least one calendar day in the future, returning a `typeMismatch.visitDate` error on the `date` field.
  *Source: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` — date boundary assertion*

- **FR-004**: System MUST reject any visit description that exceeds 255 characters, returning a field error on `description`.
  *Source: `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` — 256-character description assertion*

- **FR-005**: System MUST reject any visit description that is blank or empty, enforced via `@NotBlank` on the `Visit` domain class.
  *Source: `src/main/java/org/springframework/samples/petclinic/owner/Visit.java` — `@NotBlank` annotation on `description`*

- **FR-006**: System MUST disallow binding of `id` and `*.id` fields from incoming HTTP form parameters, registering them as disallowed fields on the `WebDataBinder`.
  *Source: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `WebDataBinder` configuration*

- **FR-007**: System MUST expose the resolved `Pet` and `Owner` objects as named model attributes (`"pet"` and `"owner"`) before rendering the visit creation form, so the view has consistent data on every request.
  *Source: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `@ModelAttribute` methods*

- **FR-008**: System MUST append the new Visit to the Pet's visit collection via `pet.addVisit(visit)` and persist it by saving the Owner aggregate; visits MUST NOT be persisted through a separate, independent save call.
  *Source: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `pet.addVisit(visit)` and `this.owners.save(owner)`*

- **FR-009**: System MUST redirect the user to `/owners/{ownerId}` after a successful visit submission.
  *Source: `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `redirect:/owners/{ownerId}` return value*

- **FR-010**: System MUST NOT expose a delete endpoint for visits; visit records are append-only and MUST be retained indefinitely once created.
  *Source: `.specify/memory/constitution.md` — "Visit records MUST NOT be deleted once created; visit history MUST be append-only."*

- **FR-011**: System MUST pre-populate the visit date field with `LocalDate.now().plusDays(1)` when the new-visit form is first rendered.
  *Source: `src/main/java/org/springframework/samples/petclinic/owner/Visit.java` — default value on `date` field*

### Key Entities

- **Visit**: A veterinary appointment record. Key attributes: `id` (server-assigned, not bindable from form), `date` (`LocalDate`, must be at least tomorrow, formatted `yyyy-MM-dd`), `description` (non-blank string, max 255 characters). Owned by exactly one `Pet` via a `@OneToMany` / `@JoinColumn(name="pet_id")` relationship on `Pet`. Stored in the `visits` table with columns `pet_id`, `visit_date`, `description`.

- **Pet**: The animal for which a visit is scheduled. Holds the visit collection; exposes `addVisit(visit)` to append new visits. Resolved from the owner aggregate before any visit operation.

- **Owner**: The registered guardian of one or more pets. Acts as the aggregate root through which visits are persisted (`owners.save(owner)`). Must exist in the repository before a visit can be created for any of their pets.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: A clinic user can complete the new-visit form — from opening the form to receiving the confirmation redirect — without encountering an unhandled error when all inputs are valid.

- **SC-002**: Every visit submitted with a date of today or earlier is rejected before any data is written, and the form is re-displayed with a visible error on the date field.

- **SC-003**: Every visit submitted with a description longer than 255 characters is rejected before any data is written, and the form is re-displayed with a visible error on the description field.

- **SC-004**: Every visit submitted for a non-existent owner or a pet not belonging to the resolved owner is rejected before any data is written, with no partial visit record created in the database.

- **SC-005**: All visits recorded for a pet are visible on the owner detail page immediately after submission, with no visit entry missing or duplicated.

- **SC-006**: No visit record created through the normal submission flow can be removed from the system through any exposed user-facing action.

- **SC-007**: The `id` field of a visit cannot be set to an arbitrary value by a user submitting a crafted form request; the server-assigned identity is always used.

---

## Assumptions

- The `visits` table with columns `pet_id`, `visit_date`, and `description` already exists in all supported database profiles (H2, MySQL, PostgreSQL); no schema migration is required as part of this feature.
- The Owner and Pet registration flows are already functional; this feature depends on at least one owner with at least one pet existing in the system.
- The owner detail view (`/owners/{ownerId}`) already renders the visit history section; this feature does not require changes to that view's template structure beyond what is already present.
- No authentication or authorisation layer is applied to the visit creation endpoint in the current codebase; any user who can reach the URL can submit a visit form.
- Visit date validation (future-only) is enforced at the controller/binding layer, not as a database constraint; the database will accept any date value if the application layer is bypassed.
- The `description` field's 255-character limit is enforced by both `@Size(max=255)` bean validation and the underlying database column width; both constraints are assumed to be consistent.
- Multi-clinic and multi-location scenarios are out of scope per the project constitution; all visits belong to the single clinic instance.
- Billing, invoicing, and veterinarian assignment to a specific visit are out of scope for this feature as defined by the project constitution.