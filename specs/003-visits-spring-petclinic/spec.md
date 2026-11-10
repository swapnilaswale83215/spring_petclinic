# Feature Specification: Visits for Spring PetClinic

**Feature Branch**: `003-visits-spring-petclinic`

**Created**: 2025-01-31

**Status**: Draft

**Input**: User description: "Visits for spring_petclinic"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Schedule a New Vet Visit (Priority: P1)

A pet owner (or clinic receptionist acting on their behalf) navigates to a specific pet's profile and books a future veterinary appointment by selecting a date and providing a reason for the visit. The system confirms the booking and immediately surfaces the new visit in the owner's visit history.

**Why this priority**: Scheduling a visit is the core action of this feature. Without it, no other visit-related capability has value. It is the minimum viable slice that delivers direct, demonstrable benefit to clinic staff and pet owners.

**Independent Test**: Can be fully tested by navigating to an existing owner's pet, submitting the visit form with a future date and a description, and verifying the new visit appears on the owner detail page — delivering a confirmed appointment record.

**Acceptance Scenarios**:

1. **Given** a registered owner with at least one registered pet, **When** the user submits the visit form with a date at least one day in the future and a non-empty description (≤ 255 characters), **Then** the visit is saved, the user is redirected to the owner detail page, and the new visit appears in the pet's visit history.
2. **Given** a registered owner with at least one registered pet, **When** the user opens the visit creation form, **Then** the form is pre-populated with the pet's name, the owner's name, and the earliest selectable date is tomorrow (not today or any past date).
3. **Given** a registered owner with at least one registered pet, **When** the user submits the visit form with all valid inputs, **Then** the system associates the visit exclusively with the correct pet and no other pet on the same owner.

---

### User Story 2 - Prevent Invalid Visit Dates (Priority: P2)

A user attempting to book a visit for today or a past date is stopped by the system before the record is saved. The form is re-displayed with a clear error message indicating that only future dates are accepted.

**Why this priority**: Accepting past or same-day dates would corrupt the appointment schedule and undermine the clinic's operational integrity. This guard is essential for data quality but depends on the basic scheduling flow (P1) existing first.

**Independent Test**: Can be fully tested by submitting the visit form with today's date and then with a past date, verifying that in both cases the form is re-displayed with a validation error and no visit record is created.

**Acceptance Scenarios**:

1. **Given** the visit creation form is displayed, **When** the user submits a visit date equal to today's date, **Then** the form is re-displayed with a validation error and the visit is not saved.
2. **Given** the visit creation form is displayed, **When** the user submits a visit date in the past, **Then** the form is re-displayed with a validation error and the visit is not saved.
3. **Given** the visit creation form is displayed, **When** the user submits a visit date that is tomorrow or later, **Then** no date-related validation error is shown and the visit proceeds to be saved.

---

### User Story 3 - Prevent Visits for Unknown Owners or Pets (Priority: P2)

When a visit URL references an owner ID or pet ID that does not exist in the system, the request is rejected before any visit processing occurs. The user receives a clear error rather than a partially processed or silently failed booking.

**Why this priority**: Referential integrity is non-negotiable. A visit must always be traceable to a real owner and a real pet. This scenario protects against stale bookmarks, manual URL manipulation, and data inconsistencies.

**Independent Test**: Can be fully tested by directly requesting the visit form URL with a non-existent owner ID and separately with a valid owner ID but a pet ID that does not belong to that owner, verifying that both cases result in an error response and no visit is created.

**Acceptance Scenarios**:

1. **Given** a visit form URL containing an owner ID that does not exist in the system, **When** the page is requested, **Then** the system rejects the request with an error before rendering the form.
2. **Given** a visit form URL containing a valid owner ID but a pet ID that belongs to a different owner, **When** the page is requested, **Then** the system rejects the request with an error before rendering the form.
3. **Given** a visit form URL containing both a valid owner ID and a pet ID that belongs to that owner, **When** the page is requested, **Then** the form is rendered successfully.

---

### User Story 4 - Prevent Empty or Oversized Visit Descriptions (Priority: P3)

A user who submits a visit form with a blank description or a description exceeding the allowed length is shown a validation error on the form. The visit is not saved until a valid description is provided.

**Why this priority**: Description quality is important for clinical records but is a secondary concern compared to the core scheduling and referential integrity flows. The constraint is well-defined and straightforward to enforce.

**Independent Test**: Can be fully tested by submitting the visit form with an empty description and then with a description of 256 or more characters, verifying that both cases re-display the form with a validation error and no visit is saved.

**Acceptance Scenarios**:

1. **Given** the visit creation form is displayed, **When** the user submits the form with an empty description, **Then** the form is re-displayed with a validation error indicating the description is required, and no visit is saved.
2. **Given** the visit creation form is displayed, **When** the user submits the form with a description exceeding 255 characters, **Then** the form is re-displayed with a validation error indicating the description is too long, and no visit is saved.
3. **Given** the visit creation form is displayed, **When** the user submits the form with a description between 1 and 255 characters inclusive, **Then** no description-related validation error is shown.

---

### User Story 5 - View Visit History for a Pet (Priority: P3)

A clinic receptionist or owner views the owner detail page and can see all previously recorded visits for each of the owner's pets, listed in a readable format with date and description.

**Why this priority**: Visit history is read-only and depends entirely on visits having been created (P1). It is valuable for clinical continuity but does not block the core scheduling capability.

**Independent Test**: Can be fully tested by navigating to an owner detail page for an owner whose pet has at least one recorded visit, and verifying that the visit date and description are displayed correctly under the appropriate pet.

**Acceptance Scenarios**:

1. **Given** an owner whose pet has one or more recorded visits, **When** the owner detail page is viewed, **Then** each visit is displayed with its date and description under the correct pet.
2. **Given** an owner whose pet has no recorded visits, **When** the owner detail page is viewed, **Then** no visit history entries are shown for that pet (no error, no placeholder data).
3. **Given** an owner with multiple pets, **When** the owner detail page is viewed, **Then** each pet's visit history is displayed separately and visits are not mixed between pets.

---

### Edge Cases

- What happens when the visit form is submitted with the `id` field manually injected into the POST body? The system must silently discard the submitted `id` value and never allow client-supplied identifiers to influence the persisted record.
- What happens when two concurrent requests attempt to book a visit for the same pet at the same time? The system must persist both visits independently without data loss or corruption, as visit records are append-only.
- What happens when the owner has no pets registered? The visit creation URL for a pet ID that does not exist on that owner must be rejected with an error, not silently create an orphaned visit.
- What happens when the description contains only whitespace characters? The system must treat a whitespace-only description as blank and reject it with the same validation error as an empty description.
- What happens when a visit record is created and then someone attempts to delete it? Visit records must not be deletable once created; the visit history is append-only per domain rules.
- What happens when the visit date field is submitted in an unrecognised format? The system must reject the submission with a clear date format error and re-display the form.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST resolve the owner identified by the URL path before any visit processing begins; if the owner does not exist, the request MUST be rejected with a descriptive error before the visit form is rendered or any visit data is processed.
- **FR-002**: System MUST resolve the pet identified by the URL path and confirm it belongs to the resolved owner; if the pet does not exist on that owner, the request MUST be rejected with a descriptive error.
- **FR-003**: System MUST enforce that a visit date is strictly in the future (at minimum, the day after the current date); visit dates equal to or before today MUST be rejected with a validation error.
- **FR-004**: System MUST enforce that a visit description is non-blank and does not exceed 255 characters; submissions violating either constraint MUST be rejected with a field-level validation error and the form re-displayed.
- **FR-005**: System MUST associate every successfully created visit with exactly one pet by appending it to that pet's visit history; a visit MUST NOT be created without a valid pet association.
- **FR-006**: System MUST present the visit creation form pre-populated with the resolved pet and owner details, and MUST communicate to the UI that the earliest selectable visit date is the day after the current date.
- **FR-007**: System MUST redirect the user to the owner detail page upon successful visit creation, where the newly created visit MUST be visible in the pet's visit history.
- **FR-008**: System MUST discard any client-supplied identifier values submitted via the visit form; identifier fields MUST NOT be bindable from user-submitted form data.
- **FR-009**: System MUST display all recorded visits for each pet on the owner detail page, showing at minimum the visit date and description for each visit.
- **FR-010**: System MUST NOT permit deletion or modification of existing visit records; visit history is append-only.
- **FR-011**: All user-facing labels, error messages, and display strings on the visit form and owner detail page MUST be externalised to internationalisation message property files and MUST be available in all supported languages.

### Key Entities

- **Visit**: Represents a single scheduled veterinary appointment for a pet. Key attributes: unique identifier (system-assigned), visit date (future date only), description (non-blank, up to 255 characters). A visit belongs to exactly one pet and is immutable once created.
- **Owner**: The registered guardian of one or more pets. Identified by a system-assigned ID. Serves as the entry point for navigating to a pet's visit history and for booking new visits.
- **Pet**: A registered animal belonging to exactly one owner. Identified by a system-assigned ID. Holds the ordered collection of all visits recorded against it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A clinic receptionist can complete the full visit booking flow — from opening the visit form to seeing the confirmed visit on the owner detail page — in under 2 minutes under normal operating conditions.
- **SC-002**: 100% of visit submissions with a date of today or earlier are rejected before any record is persisted, with a user-visible validation error displayed on the form.
- **SC-003**: 100% of visit submissions referencing a non-existent owner or a pet not belonging to the specified owner are rejected before any visit record is created.
- **SC-004**: 100% of visit submissions with a blank description or a description exceeding 255 characters are rejected with a field-level error and the form re-displayed with the user's previously entered data intact.
- **SC-005**: Every successfully created visit appears on the owner detail page immediately after the booking redirect, with the correct date and description displayed under the correct pet.
- **SC-006**: No visit record created through the booking flow contains a client-supplied identifier; all identifiers are system-assigned.
- **SC-007**: The visit form and owner detail page render correctly and display all user-facing strings in the active locale, with no untranslated or hard-coded display text visible to the user.
- **SC-008**: The system supports the concurrent creation of multiple visits for different pets without data loss or cross-pet contamination of visit records.

## Assumptions

- The visit scheduling feature covers creation and display of visits only; editing or cancelling existing visits is out of scope for this feature, consistent with the domain rule that visit records are append-only.
- Only clinic staff (receptionists) and the pet owner themselves are assumed to be the primary users of the visit booking flow; no role-based access control differentiation is introduced by this feature.
- The visit form is accessed via a URL that includes both the owner ID and the pet ID as path parameters; no session-based or cookie-based pet selection mechanism is assumed.
- The date format used in the visit form is `yyyy-MM-dd`; the UI is responsible for presenting a date picker that constrains selection to future dates, but server-side validation is the authoritative enforcement point.
- Visit history displayed on the owner detail page shows all visits for a pet with no pagination for the initial implementation; if a pet accumulates a very large number of visits, display performance is acceptable within normal clinic usage volumes.
- The feature operates within the existing single-clinic, modular monolith architecture; no multi-location or multi-clinic visit routing is in scope.
- Billing, invoicing, and payment processing associated with a visit are explicitly out of scope per the project constitution.
- The existing owner registration and pet registration flows are prerequisites; this feature assumes valid owners and pets already exist in the system.
- Veterinarian assignment to a specific visit is not part of this feature's scope; the visit entity records the appointment but does not require a vet to be designated at booking time.
- All supported database profiles (H2, MySQL, PostgreSQL) are assumed to handle the visit persistence correctly; no profile-specific visit storage behaviour is introduced.