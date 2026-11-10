# Feature Specification: Pet Management for Spring PetClinic

**Feature Branch**: `002-pets-spring-petclinic`

**Created**: 2025-01-31

**Status**: Draft

**Input**: User description: "Pets for spring_petclinic"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register a New Pet for an Owner (Priority: P1)

A clinic receptionist navigates to an existing owner's profile and adds a new pet by providing the pet's name, birth date, and species type. The system validates the input and saves the pet under that owner.

**Why this priority**: Pet registration is the foundational capability of the Pets module. Without it, no visits can be scheduled and no pet history can be maintained. All other pet-related workflows depend on a pet existing in the system.

**Independent Test**: Can be fully tested by navigating to an owner's detail page, clicking "Add New Pet", completing the form with a valid name, birth date, and type, and confirming the pet appears in the owner's pet list.

**Acceptance Scenarios**:

1. **Given** a registered owner exists, **When** the receptionist submits a new pet form with a valid name (≤ 30 characters), a birth date, and a selected pet type, **Then** the pet is saved and displayed in the owner's pet list.
2. **Given** a registered owner exists, **When** the receptionist submits a new pet form with a blank name, **Then** the system rejects the submission and displays a validation error indicating the name is required.
3. **Given** a registered owner exists, **When** the receptionist submits a new pet form with a name exceeding 30 characters, **Then** the system rejects the submission and displays a validation error indicating the name is too long.
4. **Given** a registered owner exists, **When** the receptionist submits a new pet form without selecting a pet type, **Then** the system rejects the submission and displays a validation error indicating the type is required.
5. **Given** a registered owner exists, **When** the receptionist submits a new pet form without a birth date, **Then** the system rejects the submission and displays a validation error indicating the birth date is required.
6. **Given** an owner already has a pet named "Buddy", **When** the receptionist attempts to register another pet named "buddy" (different case) under the same owner, **Then** the system rejects the duplicate and displays an appropriate error message.

---

### User Story 2 - Edit an Existing Pet's Details (Priority: P2)

A clinic receptionist opens an existing pet's record from the owner's profile and updates the pet's name, birth date, or type to correct or refresh the information.

**Why this priority**: Pet details may need correction after initial registration (e.g., a misspelled name or incorrect birth date). Editing is essential for data accuracy but is secondary to the initial registration capability.

**Independent Test**: Can be fully tested by navigating to an owner's detail page, selecting an existing pet's "Edit" action, modifying one or more fields, saving, and confirming the updated values are reflected on the owner's profile.

**Acceptance Scenarios**:

1. **Given** a pet exists under an owner, **When** the receptionist updates the pet's name to a valid value and saves, **Then** the pet record reflects the new name.
2. **Given** a pet exists under an owner, **When** the receptionist updates the pet's birth date to a valid past date and saves, **Then** the pet record reflects the new birth date.
3. **Given** a pet exists under an owner, **When** the receptionist attempts to save an edit with a blank name, **Then** the system rejects the change and displays a validation error.
4. **Given** a pet exists under an owner, **When** the receptionist changes the pet type to a different valid type and saves, **Then** the pet record reflects the updated type.
5. **Given** an owner has two pets named "Max" and "Bella", **When** the receptionist attempts to rename "Bella" to "max" (case-insensitive duplicate), **Then** the system rejects the change and displays a duplicate name error.

---

### User Story 3 - View a Pet's Profile and Visit History (Priority: P2)

A clinic receptionist or veterinarian views a pet's full profile — including its name, type, birth date, and complete visit history — from the owner's detail page.

**Why this priority**: Viewing pet details and visit history is critical for clinical decision-making and continuity of care. It is equally important as editing but ranked alongside it because it depends on pets already existing in the system.

**Independent Test**: Can be fully tested by navigating to an owner's detail page and confirming that each pet's name, type, birth date, and associated visits (date and description) are displayed correctly.

**Acceptance Scenarios**:

1. **Given** a pet with recorded visits exists under an owner, **When** the receptionist views the owner's detail page, **Then** the pet's name, type, birth date, and all past visits (date and description) are displayed.
2. **Given** a pet with no visits exists under an owner, **When** the receptionist views the owner's detail page, **Then** the pet's details are displayed with an empty visit history section (no error or missing data).
3. **Given** an owner has multiple pets, **When** the receptionist views the owner's detail page, **Then** all pets and their respective visit histories are displayed without mixing data between pets.

---

### User Story 4 - Schedule a Veterinary Visit for a Pet (Priority: P3)

A clinic receptionist books a future veterinary visit for a specific pet by providing a visit date (at least the next calendar day) and a description of the reason for the visit.

**Why this priority**: Visit scheduling is a downstream capability that depends on pet registration being complete. It is a distinct workflow from pet management itself and is ranked lower because it extends rather than defines the core pet record.

**Independent Test**: Can be fully tested by navigating to an owner's pet, clicking "Add Visit", entering a future date and a description, saving, and confirming the visit appears in the pet's visit history.

**Acceptance Scenarios**:

1. **Given** a pet exists under a valid owner, **When** the receptionist submits a visit form with a date set to tomorrow and a non-blank description, **Then** the visit is saved and appears in the pet's visit history.
2. **Given** a pet exists under a valid owner, **When** the receptionist submits a visit form with today's date, **Then** the system rejects the submission and displays an error indicating the visit date must be in the future.
3. **Given** a pet exists under a valid owner, **When** the receptionist submits a visit form with a past date, **Then** the system rejects the submission and displays an error indicating the visit date must be in the future.
4. **Given** a pet exists under a valid owner, **When** the receptionist submits a visit form with a valid future date but no description, **Then** the system rejects the submission and displays a validation error indicating the description is required.
5. **Given** an invalid owner ID is used in the visit URL, **When** the system attempts to resolve the owner, **Then** the system returns an appropriate error response and does not create a visit record.
6. **Given** a valid owner ID but an invalid pet ID is used in the visit URL, **When** the system attempts to resolve the pet, **Then** the system returns an appropriate error response and does not create a visit record.

---

### Edge Cases

- What happens when a pet name contains only whitespace characters? The system must treat this as a blank name and reject it.
- What happens when two different owners each have a pet with the same name? This is permitted — the uniqueness constraint applies only within a single owner's pets.
- What happens when the pet type list is empty (no types configured in the system)? The add/edit pet form should render without a selectable type, and submission should be blocked by the required-type validation rule.
- What happens when a visit is submitted with a date exactly one day in the future (boundary condition)? The system must accept this as a valid visit date.
- What happens when a visit description exceeds 255 characters? The system must reject the submission with a validation error indicating the description is too long.
- How does the system handle concurrent attempts to register two pets with the same name under the same owner? The database-level unique constraint on `(owner_id, name)` must prevent duplicate records, and the user must receive a meaningful error rather than a system exception.
- What happens when a user attempts to access the pet edit form for a pet that does not belong to the specified owner? The system must reject the request and not expose another owner's pet data.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow a receptionist to add a new pet to an existing owner's record by providing a name, birth date, and pet type.
- **FR-002**: System MUST validate that a pet name is not blank and does not exceed 30 characters before saving.
- **FR-003**: System MUST validate that a pet type is selected when registering a new pet.
- **FR-004**: System MUST validate that a pet birth date is provided when registering or editing a pet.
- **FR-005**: System MUST enforce that pet names are unique within a single owner's pet collection, using case-insensitive comparison.
- **FR-006**: System MUST allow a receptionist to edit an existing pet's name, birth date, and type.
- **FR-007**: System MUST display all pets belonging to an owner on the owner's detail page, including each pet's name, type, birth date, and visit history.
- **FR-008**: System MUST present pet type options in alphabetical order when displaying the pet registration and edit forms.
- **FR-009**: System MUST allow a receptionist to schedule a future veterinary visit for a pet by providing a visit date and a description.
- **FR-010**: System MUST reject visit dates that are today or in the past; only dates from the next calendar day onward are permitted.
- **FR-011**: System MUST reject visit submissions that do not include a description.
- **FR-012**: System MUST reject visit submissions where the description exceeds 255 characters.
- **FR-013**: System MUST reject visit creation requests that reference an owner ID that does not exist in the system.
- **FR-014**: System MUST reject visit creation requests that reference a pet ID that does not belong to the resolved owner.
- **FR-015**: System MUST preserve the complete, immutable visit history for each pet; previously recorded visits MUST NOT be editable or deletable.
- **FR-016**: System MUST prevent direct assignment to internal identifier fields (such as record IDs) through form submissions.
- **FR-017**: All user-facing labels, validation messages, and page text related to pet management MUST be externalised to internationalisation message files and MUST be available in all supported languages.

### Key Entities

- **Pet**: Represents an animal registered under exactly one owner. Key attributes: name (required, ≤ 30 characters, unique per owner case-insensitively), birth date (required), pet type (required). Belongs to one Owner; has zero or more Visits.
- **PetType**: Represents the species or category of a pet (e.g., Dog, Cat, Hamster, Bird). Key attribute: name (required, not blank). Used as a classification reference on Pet records. Retrieved in alphabetical order for display.
- **Visit**: Represents a single veterinary appointment record for a pet. Key attributes: visit date (required, must be a future date — at minimum the next calendar day), description (required, ≤ 255 characters). Belongs to exactly one Pet. Immutable once created.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A receptionist can complete the registration of a new pet under an existing owner — from opening the add-pet form to seeing the pet appear in the owner's profile — in under 2 minutes.
- **SC-002**: All validation errors (blank name, missing type, missing birth date, duplicate name, invalid visit date) are surfaced to the user on the same page without data loss, so the receptionist does not need to re-enter valid fields.
- **SC-003**: 100% of submitted pet records that violate uniqueness constraints (duplicate name under the same owner) are rejected before reaching persistent storage.
- **SC-004**: A receptionist can schedule a veterinary visit for a pet — from opening the add-visit form to seeing the visit appear in the pet's history — in under 90 seconds.
- **SC-005**: The pet type dropdown is populated and rendered correctly on the add-pet and edit-pet forms 100% of the time when pet types exist in the system.
- **SC-006**: Visit history for a pet is displayed in full on the owner's detail page, with no visits omitted, regardless of how many visits have been recorded.
- **SC-007**: All pet management and visit scheduling workflows function correctly across all supported database profiles (H2, MySQL, PostgreSQL) with no profile-specific data loss or constraint violations.
- **SC-008**: All user-facing strings introduced by this feature are fully translated in all supported language files, keeping the i18n sync test green.

## Assumptions

- The application remains a modular monolith with server-side rendering; no REST API endpoints are introduced for pet management as part of this feature.
- Only clinic receptionists and staff interact with pet management workflows; there is no self-service portal for pet owners in this scope.
- Pet types (species categories) are pre-configured in the system by an administrator; this feature does not include a UI for creating or managing pet types.
- The "next calendar day" constraint for visit dates is evaluated against the server's local date at the time of form submission, consistent with the existing implementation.
- Mobile-specific UI optimisation is out of scope; the existing responsive layout of the application is sufficient.
- Authentication and authorisation (restricting which staff members can add or edit pets) are out of scope for this feature; access control is assumed to be handled by the existing application security layer.
- Bulk import of pet records is out of scope; all pet registration is performed one record at a time through the existing form-based UI.
- The visit description field accepts free-form plain text; rich text formatting is not required.
- Existing pet records and visit history in the database are assumed to be valid and do not require a data migration as part of this feature.
- The `PetValidator` (Spring `Validator` interface) is retained for cross-field validation (e.g., duplicate name check requiring owner context) that cannot be expressed as a single Bean Validation annotation, in compliance with the project constitution.