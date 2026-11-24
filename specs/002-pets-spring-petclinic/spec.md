# Feature Specification: Pets Management for Spring PetClinic

**Feature Branch**: `002-pets-spring-petclinic`

**Created**: 2025-01-31

**Status**: Draft

**Input**: User description: "Pets for spring_petclinic"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register a New Pet for an Owner (Priority: P1)

A clinic receptionist navigates to an owner's profile and registers a new pet by providing the pet's name, birth date, and type (e.g., cat, dog). The system validates the input and saves the pet under that owner, making it immediately visible on the owner's detail page.

**Why this priority**: Pet registration is the foundational capability of the Pets module. Without it, no visit history can be recorded and the clinic cannot track patient animals. All other pet-related stories depend on at least one pet existing.

**Independent Test**: Can be fully tested by navigating to `/owners/{ownerId}/pets/new`, submitting a valid pet name, birth date, and type, and confirming the pet appears on the owner's detail page at `/owners/{ownerId}`.

**Acceptance Scenarios**:

1. **Given** an owner exists in the system, **When** a receptionist submits a new pet form with a unique name, a valid birth date, and a selected pet type, **Then** the pet is saved and displayed on the owner's detail page with the correct name, birth date, and type.
2. **Given** an owner already has a pet named "Whiskers", **When** a receptionist attempts to register another pet named "Whiskers" for the same owner, **Then** the form is re-displayed with a validation error indicating the name is already in use for that owner.
3. **Given** a receptionist submits the new pet form with no name entered, **When** the form is submitted, **Then** the form is re-displayed with a validation error requiring a pet name.
4. **Given** a receptionist submits the new pet form with no pet type selected, **When** the form is submitted, **Then** the form is re-displayed with a validation error requiring a pet type selection.

---

### User Story 2 - Edit an Existing Pet's Details (Priority: P2)

A clinic receptionist opens an existing pet's edit form from the owner's detail page and updates the pet's name, birth date, or type. The system validates the changes and saves them, reflecting the updated information on the owner's detail page.

**Why this priority**: Pet details may need correction after initial registration (e.g., a misspelled name or incorrect birth date). This story ensures the clinic's records remain accurate without requiring deletion and re-creation of a pet record.

**Independent Test**: Can be fully tested by navigating to `/owners/{ownerId}/pets/{petId}/edit`, modifying one or more fields, submitting the form, and confirming the updated values appear on the owner's detail page.

**Acceptance Scenarios**:

1. **Given** a pet exists under an owner, **When** a receptionist updates the pet's name to a new unique name and submits the edit form, **Then** the pet record reflects the new name on the owner's detail page.
2. **Given** a pet exists under an owner, **When** a receptionist updates the pet's birth date to a valid past date and submits the form, **Then** the pet record reflects the updated birth date.
3. **Given** an owner has two pets named "Max" and "Bella", **When** a receptionist attempts to rename "Max" to "Bella" via the edit form, **Then** the form is re-displayed with a validation error indicating the name is already in use for that owner.
4. **Given** a receptionist clears the pet name field and submits the edit form, **When** the form is submitted, **Then** the form is re-displayed with a validation error requiring a pet name.

---

### User Story 3 - View a Pet's Visit History on the Owner Detail Page (Priority: P2)

A clinic receptionist or vet views an owner's detail page and sees each of the owner's pets listed alongside that pet's chronological visit history, including visit dates and descriptions.

**Why this priority**: Visibility of visit history is essential for clinical continuity. Staff must be able to review past encounters before scheduling or conducting a new visit. This story delivers read-only value independently of visit creation.

**Independent Test**: Can be fully tested by loading `/owners/{ownerId}` for an owner whose pets have at least one recorded visit and confirming that visit dates and descriptions are displayed beneath each pet.

**Acceptance Scenarios**:

1. **Given** an owner has a pet with two recorded visits, **When** a receptionist views the owner's detail page, **Then** both visits are listed under the pet in chronological order with their dates and descriptions visible.
2. **Given** an owner has a pet with no recorded visits, **When** a receptionist views the owner's detail page, **Then** the pet is displayed with an empty visit history section and no error is shown.
3. **Given** an owner has multiple pets, **When** a receptionist views the owner's detail page, **Then** each pet is displayed with its own independent visit history.

---

### User Story 4 - Prevent Duplicate Pet Names Under the Same Owner via Concurrent Requests (Priority: P3)

When two simultaneous registration requests are submitted for the same owner with the same pet name, the system ensures only one pet is created, preserving data integrity without requiring manual deduplication.

**Why this priority**: This is a data-integrity safeguard rather than a primary user-facing workflow. It protects against race conditions in high-traffic or multi-tab scenarios but does not affect the day-to-day experience of a single receptionist.

**Independent Test**: Can be fully tested by submitting two concurrent POST requests to `/owners/{ownerId}/pets/new` with identical pet names and confirming that exactly one pet is persisted and the owner's pet count increases by exactly one.

**Acceptance Scenarios**:

1. **Given** two concurrent POST requests are submitted for the same owner with the same pet name, birth date, and type, **When** both requests race to persist the new pet simultaneously, **Then** exactly one request succeeds, the owner's total pet count increases by exactly one, and only one pet with that name exists under the owner.
2. **Given** the losing concurrent request is rejected, **When** the response is returned to that client, **Then** the response does not indicate a server error; it indicates the name is already in use.

---

### User Story 5 - Look Up a Pet by Name, Ignoring Unsaved Records (Priority: P3)

The system's internal name-collision check operates only against persisted pet records. An in-memory (unsaved) pet with a duplicate name does not trigger a false conflict during the registration flow.

**Why this priority**: This is an internal correctness requirement that prevents false validation errors during the new-pet registration flow. It is not directly visible to end users but underpins the reliability of Story 1.

**Independent Test**: Can be fully tested by calling the owner's pet lookup with `ignoreNew = true` when both a persisted and an unsaved pet share the same name, and confirming only the persisted pet is returned.

**Acceptance Scenarios**:

1. **Given** an owner has a persisted pet named "Whiskers" and an in-memory (unsaved) pet also named "Whiskers", **When** a name-collision check is performed with `ignoreNew = true`, **Then** only the persisted pet is returned and no false duplicate error is raised.
2. **Given** an owner has only an unsaved pet named "Whiskers" and no persisted pet with that name, **When** a name-collision check is performed with `ignoreNew = true`, **Then** no collision is detected and the registration proceeds.

---

### Edge Cases

- What happens when an owner is not found by the provided owner ID during pet registration or edit? The system rejects the request with an appropriate error rather than creating an orphaned pet record.
- What happens when a pet type is not available in the system (e.g., the types list is empty)? The registration form cannot be completed; the system must surface an appropriate message.
- What happens when a pet's name contains only whitespace? The system treats this as a blank name and rejects it with a validation error.
- What happens when a pet's birth date is set to a future date? The system should accept the field format but the clinic's operational expectation is that birth dates are in the past; this boundary should be validated.
- What happens when a pet ID referenced in a visit creation request does not belong to the resolved owner? The system rejects the visit with an error and does not create an orphaned visit record.
- What happens when two pets under different owners share the same name? This is permitted; the uniqueness constraint is scoped to a single owner.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow a receptionist to register a new pet for an existing owner by providing a pet name, birth date, and pet type via a form at `/owners/{ownerId}/pets/new`.
- **FR-002**: System MUST reject a new pet registration if the pet name is blank or contains only whitespace, re-displaying the form with a descriptive validation error.
- **FR-003**: System MUST reject a new pet registration if no pet type is selected, re-displaying the form with a descriptive validation error.
- **FR-004**: System MUST reject a new pet registration if the owner already has a persisted pet with the same name (case-insensitive), re-displaying the form with a name-in-use validation error.
- **FR-005**: System MUST allow a receptionist to edit an existing pet's name, birth date, and type via a form at `/owners/{ownerId}/pets/{petId}/edit`.
- **FR-006**: System MUST reject a pet edit if the updated name conflicts with another persisted pet's name under the same owner (case-insensitive), re-displaying the form with a name-in-use validation error.
- **FR-007**: System MUST display all pets belonging to an owner on the owner's detail page, each accompanied by its chronological visit history (date and description).
- **FR-008**: System MUST populate the pet registration and edit forms with all available pet types sourced from the pet type reference data.
- **FR-009**: System MUST enforce that every pet belongs to exactly one owner; pet creation without a valid, existing owner MUST be rejected.
- **FR-010**: System MUST enforce that every pet has a valid pet type assigned at the point of registration; a pet without a type MUST NOT be persisted.
- **FR-011**: System MUST prevent client-side manipulation of pet or visit identifier fields; `id` and nested `*.id` fields MUST NOT be bindable from form submissions.
- **FR-012**: System MUST ensure that concurrent registration requests for the same owner and pet name result in exactly one persisted pet record, with the duplicate request receiving a name-in-use response rather than a server error.
- **FR-013**: System MUST perform pet name-collision checks only against persisted pet records when the `ignoreNew` flag is active, so that unsaved in-memory pets do not trigger false duplicate errors during the registration flow.
- **FR-014**: System MUST ensure seed date data for pet and owner records is consistent across all supported database profiles (H2 and MySQL), with zero mismatches permitted across all seed entries.

### Key Entities

- **Pet**: A registered animal belonging to exactly one owner. Key attributes: name (unique per owner, case-insensitive), birth date, and pet type. Carries an append-only list of visit records.
- **PetType**: A reference classification for a pet (e.g., cat, dog, bird, hamster). Selected at pet registration and required; sourced from a managed reference list. Has no outbound relationships of its own.
- **Owner**: The registered guardian of one or more pets. Pet registration and lookup are always owner-scoped; a pet cannot exist without an owner.
- **Visit**: An append-only veterinary encounter record associated with exactly one pet. Displayed as part of the pet's history on the owner detail page. Visit records must never be deleted.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A receptionist can complete new pet registration — from opening the form to seeing the pet on the owner's detail page — in under 2 minutes under normal clinic conditions.
- **SC-002**: 100% of pet registration attempts with a duplicate name (case-insensitive, same owner) are rejected with a user-readable error message; zero duplicate pets are persisted.
- **SC-003**: 100% of concurrent duplicate registration attempts result in exactly one persisted pet record; the rejected request receives a name-in-use response rather than a server error.
- **SC-004**: The owner detail page displays all pets and their complete visit histories with zero missing records for any owner with up to 20 pets and 50 visits per pet.
- **SC-005**: All mandatory form fields (name, pet type) produce visible, descriptive validation errors when left blank; zero silent failures occur on form submission.
- **SC-006**: Seed date parity across H2 and MySQL profiles shows zero mismatches across all registered seed entries on every test run.
- **SC-007**: 95% of receptionists can successfully register a new pet on their first attempt without requiring help documentation, as measured by task-completion observation.

## Assumptions

- The pet registration and edit forms are accessed by authenticated clinic staff (receptionists); no public-facing or patient-facing access to these forms is in scope.
- Mobile-specific layout optimisation for the pet forms is out of scope for this feature; the existing responsive Thymeleaf layout is sufficient.
- Pet deletion is out of scope; the clinic retains all pet records indefinitely in alignment with the append-only visit record policy.
- The list of available pet types is managed separately (via the admin/vet management area) and is assumed to be non-empty when the pet registration form is used; seeding of default types (cat, dog, lizard, snake, bird, hamster) is handled by existing database seed scripts.
- Birth date validation enforces correct date format; whether future birth dates are rejected as a business rule requires confirmation from clinic operations — the assumption is that the system accepts any valid date and relies on staff to enter accurate data.
- The uniqueness constraint on pet names is scoped to a single owner; two different owners may have pets with the same name without conflict.
- All three database profiles (H2, MySQL, PostgreSQL) must remain functional after any changes to the Pets module, consistent with the profile-aware persistence principle.
- The existing `OwnerRepository` is the sole data-access point for both owner and pet retrieval; no new repository interface is required for this feature.
- Internationalisation (i18n) message keys for any new or modified validation messages must be added to all supported message property files, and `I18nPropertiesSyncTest` must continue to pass.