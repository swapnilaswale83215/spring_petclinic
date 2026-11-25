**Generated-at**: 2026-11-25T06:48:19Z
**Baseline-commit**: unknown
# Feature Specification: Pets Management

**Feature Branch**: `002-pets-spring-petclinic`

**Status**: Draft

**Input**: User description: "Pets for spring_petclinic"

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Register a New Pet for an Existing Owner (Priority: P1)

An owner who is already registered in the clinic system submits the new-pet form, providing a name, birth date, and a pet type selected from the reference list. On success the pet appears in the owner's detail page and a confirmation message is shown.

**Why this priority**: Pet registration is the foundational record that all visit history depends on. Without a pet record no visit can be booked.

**Independent Test**: Navigate to an existing owner's page, open the "Add New Pet" form, submit valid data, and verify the pet appears in the owner's pet list with the correct name, birth date, and type.

**Acceptance Scenarios**:

1. **Given** an owner exists, **When** the owner submits the new-pet form with a unique name (≤ 30 characters), a birth date that is today or earlier, and a valid pet type, **Then** the pet is saved, associated with that owner, and the owner detail page is shown with the flash message "New Pet has been Added".
2. **Given** an owner exists, **When** the owner submits the new-pet form with a name that duplicates an existing pet name for that owner, **Then** the form is re-displayed with a `duplicate` error on the `name` field and no pet record is persisted.
3. **Given** an owner exists, **When** the owner submits the new-pet form with a blank name, a missing pet type, or a blank birth date, **Then** the form is re-displayed with the corresponding field-level validation errors and no pet record is persisted.
4. **Given** an owner exists, **When** the owner submits the new-pet form with a birth date set to tomorrow or any future date, **Then** the form is re-displayed with a `typeMismatch.birthDate` error on the `birthDate` field and no pet record is persisted.
5. **Given** an owner exists, **When** the owner submits the new-pet form with a name longer than 30 characters, **Then** the form is re-displayed with a length validation error on the `name` field and no pet record is persisted.

---

### User Story 2 — Edit an Existing Pet's Details (Priority: P2)

An owner opens the edit form for one of their registered pets and updates the name, birth date, or pet type. On success the updated details appear on the owner detail page.

**Why this priority**: Correcting pet records (e.g., fixing a misspelled name or wrong birth date) is a routine clinic operation. It shares the same validation rules as creation and must be independently exercisable.

**Independent Test**: Navigate to an existing pet's edit form, change the name to a new unique value, submit, and verify the owner detail page reflects the updated name with the flash message "Pet details has been edited".

**Acceptance Scenarios**:

1. **Given** a pet exists for an owner, **When** the owner submits the edit form with a changed name that is unique within that owner's pets, a valid birth date, and a valid pet type, **Then** the pet record is updated and the owner detail page is shown with the flash message "Pet details has been edited".
2. **Given** a pet exists for an owner, **When** the owner submits the edit form keeping the pet's own existing name unchanged, **Then** no duplicate error is raised and the save succeeds.
3. **Given** a pet exists for an owner, **When** the owner submits the edit form with a name that matches a different pet already belonging to the same owner, **Then** the form is re-displayed with a `duplicate` error on the `name` field and no change is persisted.
4. **Given** a pet exists for an owner, **When** the owner submits the edit form with a future birth date, **Then** the form is re-displayed with a `typeMismatch.birthDate` error and no change is persisted.

---

### User Story 3 — Book a Visit for a Pet (Priority: P3)

An owner opens the new-visit form for one of their registered pets, enters a visit date (tomorrow or later) and a description, and submits. The visit appears in the pet's visit history on the owner detail page.

**Why this priority**: Visit scheduling is the primary clinical workflow, but it depends on both an owner and a pet already existing, making it third in the dependency chain.

**Independent Test**: Navigate to an existing pet's visit form, submit a date set to tomorrow with a non-blank description, and verify the visit appears in the pet's visit list on the owner detail page with the flash message "Your visit has been booked".

**Acceptance Scenarios**:

1. **Given** a pet is registered under an owner, **When** a visit is submitted with a date of tomorrow or later and a non-blank description, **Then** the visit is saved, associated with that pet, and the owner detail page is shown with the flash message "Your visit has been booked".
2. **Given** a pet is registered under an owner, **When** a visit is submitted with today's date, **Then** the form is re-displayed with a `typeMismatch.visitDate` error on the `date` field and no visit record is persisted.
3. **Given** a visit form is open, **When** the owner ID in the URL does not correspond to any owner in the system, **Then** an `IllegalArgumentException` is raised with the message "Owner not found with id: {ownerId}. Please ensure the ID is correct".
4. **Given** a visit form is open for a valid owner, **When** the pet ID in the URL does not correspond to any pet belonging to that owner, **Then** an `IllegalArgumentException` is raised with the message "Pet with id {petId} not found for owner with id {ownerId}.".
5. **Given** multiple visits exist for a pet, **When** the owner views the owner detail page, **Then** all visits for that pet are listed and no visit record can be deleted.

---

### Edge Cases

- What happens when a pet name is exactly 30 characters? The name must be accepted (boundary value at the maximum allowed length per BR-004).
- What happens when a pet birth date is today? The date must be accepted; only strictly future dates are rejected (BR-007).
- What happens when a visit date is exactly tomorrow? The date must be accepted; today's date is the only rejected boundary (BR-003).
- What happens when the `id` field is included in the visit form submission? The field is silently disallowed by web data binding and cannot be used to manipulate identity fields (BR-010).
- What happens when a `unique_owner_pet_name` constraint violation occurs at the database level during save? The system produces a `duplicate` error on the `name` field consistent with the application-level duplicate check (BR-008).
- What happens when a pet type is submitted on the edit form for an existing pet? The type field is required only on create; the edit form must still carry a valid type value because `type_id` is a non-nullable foreign key.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST enforce a unique pet name per owner at the database level, rejecting duplicate `(owner_id, name)` combinations via the `unique_owner_pet_name` constraint on the `pets` table.
- **FR-002**: System MUST reject a pet create or edit submission when the `birthDate` is after today, surfacing a `typeMismatch.birthDate` field error.
- **FR-003**: System MUST require a pet `name` of at most 30 characters, a non-blank `name`, a `type` on new-pet creation, and a non-blank `birthDate`, rejecting submissions that omit or exceed these constraints.
- **FR-004**: System MUST redirect to the owner's detail page with the flash message "New Pet has been Added" after a successful pet creation, and "Pet details has been edited" after a successful pet update.
- **FR-005**: System MUST associate each pet with a mandatory `type_id` foreign key referencing the `types` table and an `owner_id` foreign key referencing the `owners` table.
- **FR-006**: System MUST reject a visit submission when the visit date is today or earlier, surfacing a `typeMismatch.visitDate` field error; the minimum allowed visit date is tomorrow.
- **FR-007**: System MUST raise an `IllegalArgumentException` with the message "Owner not found with id: {ownerId}. Please ensure the ID is correct" when a visit or pet operation references a non-existent owner ID.
- **FR-008**: System MUST raise an `IllegalArgumentException` with the message "Pet with id {petId} not found for owner with id {ownerId}." when a visit operation references a pet ID that does not belong to the resolved owner.
- **FR-009**: System MUST treat visit records as append-only; no delete operation on a visit record is permitted.
- **FR-010**: System MUST disallow the `id` field and any nested `*.id` fields in web data binding for visit form submissions.
- **FR-011**: System MUST redirect to the owner's detail page with the flash message "Your visit has been booked" after a successful visit submission.
- **FR-012**: System MUST allow a pet edit submission to retain the pet's own existing name without raising a duplicate error.

### Key Entities

- **Pet**: An animal registered under exactly one owner. Carries a name (unique per owner, ≤ 30 characters), a birth date (not future), and a mandatory pet type. Holds an ordered collection of visits.
- **PetType**: A reference classification for a pet (e.g., cat, dog). Has a name. Populated from the `types` table. Required at pet creation.
- **Visit**: A veterinary appointment record attached to exactly one pet. Carries a visit date (tomorrow or later at time of booking) and a description. Append-only; deletion is not permitted.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A clinic user can complete new-pet registration from form open to confirmation in a single form submission with no page errors when all required fields are valid.
- **SC-002**: All pet field validation errors (blank name, name too long, missing type on create, future birth date, duplicate name) are surfaced on the same form page without data loss, requiring no more than one resubmission to correct each error type.
- **SC-003**: A visit booking with a valid future date is accepted and visible in the pet's visit history on the owner detail page immediately after submission.
- **SC-004**: Duplicate pet name attempts within the same owner are rejected 100% of the time at both the application validation layer and the database constraint layer.
- **SC-005**: Visit records once created are never removed from the pet's visit history; the history is strictly additive across all usage scenarios.

## Assumptions

- The pet type reference list (`types` table) is pre-populated by the database initialisation scripts and is not managed through this feature; no create/edit/delete of pet types is in scope.
- The owner must already exist before any pet or visit operation is attempted; owner creation is handled by the Owners feature and is a prerequisite.
- The visit description field accepts free text up to 255 characters as defined by the `VARCHAR(255)` column in the schema; no further content validation beyond non-blank is assumed from the available evidence.
- The edit form for an existing pet does not enforce the `type` field as newly required (the pet already has a type); however, the type value must remain a valid reference to the `types` table because the foreign key is non-nullable.
- Profile-based database selection (H2 for development/test, MySQL/PostgreSQL for production) is in effect; the `unique_owner_pet_name` constraint must be validated against all three profiles before any schema change is merged.
- Visit history is displayed as part of the owner detail page via `pet.getVisits()`; no dedicated visit list page is in scope for this feature.
- The `id` and nested `*.id` fields are excluded from web data binding for visit forms as a security control; this behaviour is assumed to apply consistently across all visit form submissions.

## Business Rules

### BR-PETS-001 — Visit requires a valid owner

Rule: A visit form load or save operation must resolve the owner by ID before accessing any pet. If the owner ID does not exist, an `IllegalArgumentException` is thrown.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `loadPetWithVisit`
- Quoted evidence: `"Owner not found with id: " + ownerId + ". Please ensure the ID is correct"`

Status: Verified
---

### BR-PETS-002 — Visit requires a valid pet belonging to the resolved owner

Rule: After the owner is resolved, the pet ID must correspond to a pet belonging to that owner. If not found, an `IllegalArgumentException` is thrown.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `loadPetWithVisit`
- Quoted evidence: `"Pet with id " + petId + " not found for owner with id " + ownerId + "."`

Status: Verified
---

### BR-PETS-003 — Visit date must be at least one day in the future

Rule: The minimum allowed visit date is `LocalDate.now().plusDays(1)`. A visit dated today is rejected with `typeMismatch.visitDate` on the `date` field.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `@ModelAttribute("minVisitDate")`
- Quoted evidence: `LocalDate.now().plusDays(1)`

Status: Verified
---

### BR-PETS-004 — Pet name is required and at most 30 characters

Rule: The `name` field on a pet must not be blank and must not exceed 30 characters. The schema column is `VARCHAR_IGNORECASE(30)`.

Source:
- `src/main/resources/db/h2/schema.sql` — `pets` table definition
- Quoted evidence: `name VARCHAR_IGNORECASE(30)`
Status: Inferred

_Note: demoted from Verified — cited source was not quoted from the read context._
---

### BR-PETS-005 — Pet type is required on new-pet creation

Rule: A pet type must be selected when registering a new pet. The `type_id` column is a non-nullable foreign key to the `types` table.

Source:
- `src/main/resources/db/h2/schema.sql` — `pets` table definition; `src/main/resources/db/postgres/schema.sql` — `pets` table definition
- Quoted evidence: `type_id INTEGER NOT NULL`
Status: Inferred

_Note: demoted from Verified — cited source was not quoted from the read context._
---

### BR-PETS-006 — Pet birth date must not be a future date

Rule: A pet birth date that is after today is rejected on create or edit with a `typeMismatch.birthDate` field error.

Source:
- `specs/001-java-17-to-21-and-spring-boot-fa606d0c/spec.md` — FR-002
- Quoted evidence: `"System MUST validate that a pet's birth_date is not a future date, rejecting the create or edit form submission with a typeMismatch.birthDate field error when the date is after today."`

Status: Inferred
---

### BR-PETS-007 — Pet name must be unique per owner

Rule: A duplicate `(owner_id, name)` combination is rejected at both the application layer (with a `duplicate` error on the `name` field) and the database layer (via the `unique_owner_pet_name` constraint).

Source:
- `src/main/resources/db/h2/schema.sql` — `pets` table definition
- Quoted evidence: `CONSTRAINT unique_owner_pet_name UNIQUE (owner_id, name)`
Status: Inferred

_Note: demoted from Verified — cited source was not quoted from the read context._
---

### BR-PETS-008 — Renaming a pet to its own existing name must not raise a duplicate error

Rule: On edit, if the submitted name is identical to the pet's own current name, no duplicate error is raised and the save proceeds normally.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` — edit validation logic (inferred from BR-009 in repository context)
- Quoted evidence: "A pet name must be unique per Owner; … keeping the pet's own existing name on edit must raise no error."

Status: Inferred
---

### BR-PETS-009 — Successful pet create redirects with flash message

Rule: A valid new-pet submission saves the pet and redirects to the owner detail page with the flash message "New Pet has been Added".

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` — `processCreationForm`
- Quoted evidence: `"New Pet has been Added"`

Status: Verified
---

### BR-PETS-010 — Successful pet edit redirects with flash message

Rule: A valid pet-edit submission saves the change and redirects to the owner detail page with the flash message "Pet details has been edited".

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/PetController.java` — `processUpdateForm`
- Quoted evidence: `"Pet details has been edited"`

Status: Verified
---

### BR-PETS-011 — Successful visit booking redirects with flash message

Rule: A valid visit submission (dated tomorrow or later) is accepted and the owner detail page is shown with the flash message "Your visit has been booked".

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `processNewVisitForm`
- Quoted evidence: `"Your visit has been booked"`

Status: Verified
---

### BR-PETS-012 — Visit records are append-only

Rule: The schema defines no cascade delete on the `visits` table FK, and no delete operation is exposed through the controller layer. Visit history is strictly additive.

Source:
- `src/main/resources/db/h2/schema.sql` — `visits` table FK definition (no `ON DELETE CASCADE`)
- Quoted evidence: "schema enforces no cascade delete (visits are append-only per the FK definition without cascade)"
Status: Inferred

_Note: demoted from Verified — cited source was not quoted from the read context._
---

### BR-PETS-013 — Identity fields are disallowed in visit web data binding

Rule: The `id` field and any nested `*.id` fields are excluded from web data binding for visit form submissions, preventing client-side manipulation of identity fields.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `setAllowedFields` / `initBinder`
- Quoted evidence: "The `id` field and any nested `*.id` fields are disallowed in web data binding for visits, preventing client-side manipulation of identity fields."

Status: Inferred
---

### BR-PETS-014 — Pet is loaded as part of the Owner aggregate

Rule: Pets are retrieved via `owner.getPet(petId)` after the owner is resolved. The `Owner` entity is the aggregate root; pets are loaded eagerly as a `@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)` collection.

Source:
- `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` — `getPets()` / `getPet()`
- Quoted evidence: "`@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)` collection on Owner"

Status: Verified