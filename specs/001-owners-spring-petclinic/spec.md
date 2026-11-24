# Feature Specification: Owner Management

**Feature Branch**: `001-owners-spring-petclinic`

**Created**: 2025-01-31

**Status**: Draft

**Input**: User description: "Owners for spring_petclinic"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register a New Owner (Priority: P1)

A clinic receptionist navigates to the new owner registration form, fills in the owner's personal details — first name, last name, address, city, and telephone number — and submits the form. On success, they are taken directly to the new owner's profile page where they can immediately begin adding pets.

**Why this priority**: Owner registration is the entry point for all downstream clinic activity. No pet can be registered, no visit can be scheduled, and no appointment history can be maintained without a valid owner record. This is the foundational capability of the module.

**Independent Test**: Can be fully tested by navigating to `/owners/new`, submitting a valid form, and confirming the redirect to `/owners/{id}` with the saved owner's details displayed. Delivers a complete, usable owner record with no other stories required.

**Acceptance Scenarios**:

1. **Given** a user navigates to `/owners/new`, **When** they submit the form with valid `firstName`, `lastName`, `address` (≤255 characters), `city`, and a 10-digit `telephone`, **Then** the owner is persisted and the user is redirected to `/owners/{newId}` showing the saved details.
2. **Given** a user submits the registration form with an `address` field exceeding 255 characters, **When** the form is submitted, **Then** the form is re-displayed with a field-level validation error on `address` and no owner record is created.
3. **Given** a user submits the registration form with a `telephone` that is not exactly 10 digits, **When** the form is submitted, **Then** the form is re-displayed with a field-level validation error on `telephone` and no owner record is created.
4. **Given** a user submits the registration form with any mandatory field left blank, **When** the form is submitted, **Then** the form is re-displayed with appropriate field-level errors and no owner record is created.
5. **Given** a user submits a valid registration form, **When** the system processes the request, **Then** the `id` field cannot be set by the submitted form data (it is assigned by the system).

---

### User Story 2 - Search for Owners by Last Name (Priority: P2)

A clinic receptionist needs to locate an existing owner to view their record, add a pet, or schedule a visit. They navigate to the owner search page, enter a last name (or part of one), and are presented with a paginated list of matching owners. They can then click through to the owner's profile.

**Why this priority**: Without search, staff cannot locate existing owners to perform any follow-on action. This story unlocks access to all existing owner records and is required before update, pet management, or visit scheduling can be used in practice.

**Independent Test**: Can be fully tested by creating one or more owners and then searching by last name via `/owners/find`. Delivers a usable search-and-navigate workflow independently of owner editing or pet management.

**Acceptance Scenarios**:

1. **Given** at least one owner exists with last name "Smith", **When** a user searches for "Smith" at `/owners/find`, **Then** a paginated list of all owners with last name containing "Smith" is displayed.
2. **Given** owners exist across multiple pages, **When** a user requests a page number beyond the last valid page, **Then** the application redirects to page 1 of the same search results.
3. **Given** a user submits the search form with no last name entered, **When** the form is submitted, **Then** all owners are returned (broadest possible search, treating blank as a wildcard).
4. **Given** a user searches with leading or trailing whitespace in the last name field, **When** the form is submitted, **Then** the whitespace is stripped before the query is executed and results match the trimmed value.
5. **Given** exactly one owner matches the search, **When** the search is submitted, **Then** the user is redirected directly to that owner's profile page rather than a list.
6. **Given** no owners match the search term, **When** the search is submitted, **Then** the search form is re-displayed with a message indicating no owners were found.

---

### User Story 3 - View an Owner's Profile (Priority: P2)

A clinic receptionist selects an owner from search results or navigates directly to an owner's profile page. The profile displays the owner's personal details and a list of their registered pets, each with their visit history.

**Why this priority**: The owner profile is the hub from which all pet and visit management is initiated. It must be available before staff can act on any owner record.

**Independent Test**: Can be fully tested by navigating to `/owners/{id}` for an existing owner and verifying that personal details and associated pets are displayed. Delivers a complete read-only view of an owner record independently.

**Acceptance Scenarios**:

1. **Given** an owner with id `5` exists and has two registered pets, **When** a user navigates to `/owners/5`, **Then** the owner's name, address, city, and telephone are displayed alongside both pets and their visit histories.
2. **Given** a user navigates to `/owners/{id}` where no owner with that id exists, **When** the page is requested, **Then** the application raises an error indicating the owner was not found, rather than displaying an empty or broken page.
3. **Given** an owner has no registered pets, **When** their profile is viewed, **Then** the pets section is displayed as empty without errors.

---

### User Story 4 - Update an Existing Owner's Details (Priority: P3)

A clinic receptionist needs to correct or update an owner's contact information — for example, a new address or telephone number. They navigate to the owner's profile, open the edit form, make changes, and save.

**Why this priority**: Contact details change over time. While not required for initial registration or search, keeping owner records accurate is important for clinic operations. This story depends on owner registration (P1) and profile view (P2) being in place.

**Independent Test**: Can be fully tested by navigating to `/owners/{id}/edit`, submitting updated valid details, and confirming the profile page reflects the changes. Delivers a complete update workflow independently.

**Acceptance Scenarios**:

1. **Given** an owner exists with id `3`, **When** a user submits the update form at `/owners/3/edit` with valid updated details, **Then** the owner record is updated and the user is redirected to `/owners/3` showing the new details.
2. **Given** a user submits the update form with an invalid telephone number, **When** the form is submitted, **Then** the form is re-displayed with a field-level validation error and the owner record is not modified.
3. **Given** a user submits the update form with a blank mandatory field, **When** the form is submitted, **Then** the form is re-displayed with appropriate errors and no update is persisted.
4. **Given** a user attempts to submit the update form with a manually crafted `id` field in the request body, **When** the form is processed, **Then** the submitted `id` value is ignored and the owner's system-assigned id is preserved.

---

### Edge Cases

- What happens when an owner search returns exactly one result? The user is redirected directly to that owner's profile rather than displaying a single-item list.
- What happens when the `page` query parameter is `0` or negative? The application redirects to page 1 of the current search results.
- What happens when the `page` query parameter exceeds the total number of pages? The application redirects to page 1 of the current search results.
- What happens when a null `lastName` is passed to the search? It is treated as an empty string, returning all owners.
- What happens when `lastName` contains only whitespace? It is stripped to an empty string before querying, returning all owners.
- What happens when a form submission includes an `id` or nested `*.id` field? These fields are disallowed from binding and are silently ignored; the system-assigned identifier is always used.
- What happens when an owner profile is requested for a non-existent id? An error is raised with a descriptive message; no blank or partially rendered profile is shown.
- What happens when an owner has pets with names that differ only in case? Pet name lookup is case-insensitive; the system treats them as the same name for duplicate detection purposes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose a `GET /owners/new` endpoint that renders an empty owner creation form for data entry.
- **FR-002**: System MUST expose a `POST /owners/new` endpoint that validates the submitted owner data, persists a new owner record on success, and redirects the user to the new owner's profile page at `/owners/{id}`.
- **FR-003**: System MUST reject owner creation submissions that fail validation, re-display the creation form with field-level error messages, and ensure no owner record is persisted.
- **FR-004**: System MUST expose a `GET /owners/find` endpoint that renders an owner search form.
- **FR-005**: System MUST expose a `GET /owners` endpoint that accepts a `lastName` query parameter and a `page` parameter, queries owners whose last name matches the supplied value, and returns a paginated list of results.
- **FR-006**: System MUST treat a null or blank `lastName` search parameter as a wildcard that returns all owners, after stripping any surrounding whitespace from non-null values.
- **FR-007**: System MUST redirect any out-of-range `page` parameter (less than 1 or greater than the total number of pages) to page 1 of the same search results.
- **FR-008**: System MUST redirect directly to the owner's profile page when a search returns exactly one matching owner.
- **FR-009**: System MUST expose a `GET /owners/{ownerId}` endpoint that displays the owner's personal details and their registered pets with visit histories.
- **FR-010**: System MUST raise a descriptive error when an owner profile or owner-scoped operation is requested for an `ownerId` that does not exist in the system.
- **FR-011**: System MUST expose a `GET /owners/{ownerId}/edit` endpoint that renders the owner update form pre-populated with the owner's current details.
- **FR-012**: System MUST expose a `POST /owners/{ownerId}/edit` endpoint that validates the submitted update, persists changes on success, and redirects to the owner's profile page.
- **FR-013**: System MUST reject owner update submissions that fail validation, re-display the update form with field-level error messages, and ensure no changes are persisted.
- **FR-014**: System MUST prevent client-side manipulation of owner identity by disallowing binding of the `id` field and any nested `*.id` fields from all owner-bound form submissions.
- **FR-015**: System MUST enforce that the `telephone` field contains exactly 10 digits and that `address` does not exceed 255 characters, `city` does not exceed 80 characters, and all mandatory fields (`firstName`, `lastName`, `address`, `city`, `telephone`) are non-blank.

### Key Entities

- **Owner**: Represents a registered pet owner and clinic customer. Key attributes: first name, last name, address (up to 255 characters), city (up to 80 characters), telephone (exactly 10 digits). Owns one or more pets. Identity is system-assigned and cannot be set by external input.
- **Pet**: An animal belonging to exactly one owner. Relevant to the owner module as a child collection displayed on the owner profile, ordered by name. Each pet carries a visit history.
- **Visit**: A veterinary encounter record associated with a pet. Displayed as part of the owner profile view within each pet's history. Append-only; existing visit records are never deleted.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A clinic receptionist can complete new owner registration — from opening the form to landing on the confirmed profile page — in under 2 minutes under normal conditions.
- **SC-002**: Owner search by last name returns a results page in under 1 second for datasets up to 10,000 owner records.
- **SC-003**: 100% of form submissions with invalid data are rejected without persisting any partial record, and the user is returned to the form with at least one visible field-level error message.
- **SC-004**: Out-of-range page requests are handled gracefully in 100% of cases, with the user always landing on a valid results page rather than an error screen.
- **SC-005**: Owner profile pages load and display all associated pets and visit history in under 2 seconds for owners with up to 20 pets and 100 visits.
- **SC-006**: 100% of attempts to inject or override a system-assigned owner identifier via form submission are silently rejected, with the system-assigned value preserved.
- **SC-007**: Sensitive owner fields (name, address, telephone) do not appear in any application log output, error response body, or metrics surface under any tested scenario.

## Assumptions

- The owner management module operates within a single-clinic context; multi-location or multi-tenant scenarios are out of scope.
- The telephone field accepts exactly 10 consecutive digits with no formatting characters (no spaces, dashes, or parentheses); this matches the existing validation constraint in the codebase.
- The application uses server-side rendering via Thymeleaf; no separate REST API or client-side rendering is introduced for this feature.
- Authentication and authorisation of clinic staff are handled by an existing or separately specified mechanism; this feature does not introduce new access control rules.
- The H2 in-memory database is used for development and testing; MySQL and PostgreSQL profiles are used for integration and production scenarios, and all schema changes must be compatible with all three profiles.
- Sensitive owner data (name, address, telephone) must not appear in logs, error responses, or any externally observable output, in accordance with the project constitution.
- Visit records associated with an owner's pets are read-only within the owner module; creation of visits is handled by the visit scheduling feature and is out of scope here.
- The owner list pagination uses a default page size consistent with the existing application configuration; no new page size configuration is introduced by this feature.
- Mobile-specific UI optimisation is out of scope for this specification; the existing responsive layout provided by the Thymeleaf templates is assumed to be sufficient.
- Bulk import or export of owner records is out of scope for this feature.