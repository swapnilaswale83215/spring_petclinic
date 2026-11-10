# Feature Specification: Owner Management

**Feature Branch**: `001-owners-spring-petclinic`

**Created**: 2025-01-31

**Status**: Draft

**Input**: User description: "Owners for spring_petclinic"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register a New Owner (Priority: P1)

A clinic receptionist needs to register a new pet owner in the system so that the owner's pets and visits can be tracked. The receptionist navigates to the owner creation form, fills in the owner's personal details (first name, last name, address, city, telephone), and submits the form. On success, they are taken directly to the new owner's profile page with a confirmation message.

**Why this priority**: Owner registration is the foundational entry point for all downstream clinic activity. No pet can be registered and no visit can be scheduled without a valid owner record. This is the highest-value, most-blocking capability.

**Independent Test**: Can be fully tested by navigating to `/owners/new`, submitting a valid form, and verifying the redirect to the new owner's detail page with the "New Owner Created" flash message.

**Acceptance Scenarios**:

1. **Given** the receptionist is on the new owner form at `/owners/new`, **When** they submit a form with valid first name, last name, address, city, and telephone, **Then** the owner is saved and the receptionist is redirected to `/owners/{newId}` with a "New Owner Created" confirmation message.
2. **Given** the receptionist is on the new owner form, **When** they submit a form with a missing or blank last name, **Then** the form is re-displayed with a validation error and no owner record is created.
3. **Given** the receptionist is on the new owner form, **When** they submit a form with a first name exceeding 30 characters, **Then** the form is re-displayed with a validation error and no owner record is created.
4. **Given** the receptionist is on the new owner form, **When** they submit a form with a missing telephone number, **Then** the form is re-displayed with a validation error and no owner record is created.
5. **Given** the receptionist is on the new owner form, **When** they attempt to inject an `id` field via the HTTP request, **Then** the `id` field is ignored and the system assigns its own identifier.

---

### User Story 2 - Search for an Existing Owner (Priority: P2)

A clinic receptionist needs to find an existing owner to view their profile, update their details, or manage their pets. The receptionist navigates to the find-owners page, enters a partial or full last name, and is either taken directly to the owner's profile (single match) or presented with a paginated list of matching owners to select from.

**Why this priority**: Finding owners is the gateway to all owner-related operations (update, pet management, visit scheduling). Without search, staff cannot access existing records. It is second only to registration because it requires at least one owner to already exist.

**Independent Test**: Can be fully tested by first creating an owner, then navigating to `/owners/find`, searching by last name, and verifying the correct owner record appears in results or as a direct redirect.

**Acceptance Scenarios**:

1. **Given** one owner with last name "Smith" exists, **When** the receptionist searches for "Smith" on the find-owners page, **Then** they are redirected directly to that owner's detail page.
2. **Given** multiple owners with last names starting with "Sm" exist, **When** the receptionist searches for "Sm", **Then** a paginated list of all matching owners is displayed.
3. **Given** no owners match the search term, **When** the receptionist searches for "Zzzzz", **Then** the results page shows an empty list with an appropriate message.
4. **Given** the receptionist submits the find-owners form with a blank last name, **When** the search executes, **Then** all owners are returned in a paginated list (blank search broadens to all owners).
5. **Given** the receptionist enters a last name with leading or trailing whitespace, **When** the search executes, **Then** the whitespace is stripped and results match the trimmed value.

---

### User Story 3 - View Owner Profile and Pet History (Priority: P2)

A clinic receptionist or veterinarian needs to view a complete owner profile, including the owner's contact details and a list of all registered pets with their visit histories, so they can understand the owner's relationship with the clinic at a glance.

**Why this priority**: The owner detail page is the central hub for all owner-related activity. It surfaces pet registrations and visit histories, enabling informed clinical decisions. It is equally important as search since it is the destination of both search results and post-registration redirects.

**Independent Test**: Can be fully tested by navigating to `/owners/{ownerId}` for a known owner and verifying that the owner's details, their pets, and each pet's visit history are all displayed correctly.

**Acceptance Scenarios**:

1. **Given** an owner with two registered pets each having visit records exists, **When** the receptionist navigates to `/owners/{ownerId}`, **Then** the page displays the owner's name, address, city, and telephone, along with both pets and their respective visit histories.
2. **Given** an owner with no pets exists, **When** the receptionist navigates to `/owners/{ownerId}`, **Then** the page displays the owner's details with an empty pets section.
3. **Given** a request is made for a non-existent owner ID, **When** the system attempts to resolve the owner, **Then** an appropriate error is surfaced to the user indicating the owner was not found.

---

### User Story 4 - Update an Existing Owner's Details (Priority: P3)

A clinic receptionist needs to update an existing owner's contact information (address, city, telephone, or name) when the owner's details change, so that clinic records remain accurate and communications reach the right person.

**Why this priority**: Owner details change over time (address moves, phone number updates). Keeping records current is important for clinic operations but is less critical than the ability to create and find owners in the first place.

**Independent Test**: Can be fully tested by navigating to `/owners/{ownerId}/edit`, submitting updated details, and verifying the owner's profile page reflects the changes.

**Acceptance Scenarios**:

1. **Given** an existing owner, **When** the receptionist submits the edit form with a new valid telephone number, **Then** the owner record is updated and the receptionist is redirected to the owner's detail page with a confirmation message.
2. **Given** an existing owner, **When** the receptionist submits the edit form with a blank first name, **Then** the form is re-displayed with a validation error and the owner record is not changed.
3. **Given** an existing owner, **When** the receptionist attempts to change the owner's `id` via the HTTP form, **Then** the `id` field is ignored and the existing identifier is preserved.
4. **Given** a request to edit a non-existent owner ID, **When** the system attempts to resolve the owner, **Then** an appropriate error is surfaced indicating the owner was not found.

---

### Edge Cases

- What happens when an owner ID in the URL does not exist in the database? The system throws an `IllegalArgumentException` and must surface a user-friendly error page rather than an unhandled exception stack trace.
- What happens when the last-name search input is `null`? The system treats it as an empty string and returns all owners in a paginated list.
- What happens when a last-name search input contains only whitespace? The whitespace is stripped, resulting in an empty string, and all owners are returned.
- What happens when an owner form is submitted with an `id` field injected via HTTP parameters? The `id` field is blocked from web binding and the injected value is silently ignored.
- What happens when two owners share the same last name? Both are returned in the search results list; the receptionist must select the correct one.
- What happens when the owner list is very large? Results are paginated via `Pageable`; the user navigates through pages rather than receiving an unbounded result set.
- What happens when a first or last name is exactly 30 characters? The record is accepted; 30 characters is the maximum allowed length.
- What happens when a first or last name is 31 characters? The form is rejected with a size validation error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow a receptionist to create a new Owner by submitting a form with first name, last name, address, city, and telephone; the form MUST be re-displayed with validation errors if any required field is missing or violates length constraints, and no owner record MUST be persisted on failure.
- **FR-002**: System MUST persist a successfully validated Owner record and redirect the user to the newly created Owner's detail page, surfacing a "New Owner Created" confirmation message.
- **FR-003**: System MUST prevent the `id` field (and any nested `*.id` fields) from being set via HTTP request parameters on both Owner create and Owner update form submissions.
- **FR-004**: System MUST provide a find-owners search form at `/owners/find` that accepts a partial or full last name and returns matching owners; a blank or null last name MUST return all owners.
- **FR-005**: System MUST strip leading and trailing whitespace from the last-name search input before executing the query.
- **FR-006**: System MUST redirect directly to the Owner's detail page when exactly one owner matches the search criteria; when multiple owners match, a paginated list MUST be displayed for the user to select from.
- **FR-007**: System MUST display a complete Owner profile page showing the owner's personal details (first name, last name, address, city, telephone) and a list of all registered pets with their visit histories.
- **FR-008**: System MUST allow a receptionist to update an existing Owner's details (first name, last name, address, city, telephone) via an edit form, subject to the same validation rules as creation.
- **FR-009**: System MUST resolve an Owner by `ownerId` path variable before processing any Owner-scoped request (view, edit, pet operations, visit operations), and MUST surface a descriptive error to the user when no matching Owner exists.
- **FR-010**: Owner first name and last name MUST each be non-blank and MUST NOT exceed 30 characters; Owner MUST have a valid telephone number and address before registration is complete.
- **FR-011**: System MUST enforce that Owner sensitive fields (name, address, telephone) are not exposed in unprotected endpoints, debug logs, or error responses.

### Key Entities

- **Owner**: Represents a registered pet owner and clinic customer. Key attributes: first name (max 30 chars, required), last name (max 30 chars, required), address (required), city (required), telephone (required). Holds a one-to-many relationship with Pets; an Owner may have zero or more Pets.
- **Pet**: Belongs to exactly one Owner. Key attributes: name (max 30 chars, required), birth date (required), pet type (required on creation). Holds a one-to-many relationship with Visits.
- **PetType**: Classification of a pet (e.g., Cat, Dog, Hamster). Referenced by Pet. Always returned ordered by name.
- **Visit**: A veterinary visit record associated with exactly one Pet. Key attributes: date (must be at least one day in the future), description. Visit records are append-only and MUST NOT be deleted once created.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A receptionist can complete new owner registration — from opening the form to landing on the confirmed owner profile page — in under 2 minutes under normal clinic conditions.
- **SC-002**: Owner search returns matching results within 2 seconds for a database containing up to 10,000 owner records.
- **SC-003**: 95% of owner registration attempts with valid data succeed on the first submission without requiring a retry.
- **SC-004**: 100% of form submissions with missing required fields or fields exceeding length limits are rejected with a visible, specific validation message; no invalid owner record is ever persisted.
- **SC-005**: Owner search with a partial last name (minimum 1 character) returns all matching owners with no false negatives for records present in the database.
- **SC-006**: The owner detail page loads and displays all pets and visit histories for an owner with up to 20 pets (each with up to 50 visits) within 3 seconds.
- **SC-007**: Zero instances of Owner PII (name, address, telephone) appear in application error responses, debug logs, or unprotected page endpoints.

## Assumptions

- The application operates as a single-clinic, single-location system; multi-clinic scenarios are out of scope per the project constitution.
- The acting user (receptionist) is already authenticated and authorised to perform owner management operations; this feature does not introduce new authentication or role-based access control beyond what the existing application provides.
- Telephone number format validation (e.g., digit-only, length constraints) follows the existing application convention; no new telephone format rules are introduced by this feature.
- Pagination page size for owner search results follows the existing application default configuration and does not need to be configurable as part of this feature.
- The owner edit form pre-populates all existing field values so the receptionist only needs to change the fields that have updated.
- All user-facing strings introduced by this feature will be externalised to i18n message property files and translated in all supported languages before the feature is merged, in compliance with the project constitution.
- Mobile-specific UI optimisation is out of scope for this feature; the existing responsive layout is considered sufficient.
- Bulk import or batch creation of owners is out of scope; this feature covers single-owner creation and update only.
- Deletion of owner records is out of scope; the clinic retains all owner records for historical and compliance purposes.