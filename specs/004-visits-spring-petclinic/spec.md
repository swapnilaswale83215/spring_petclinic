# Feature Specification: Visit Scheduling & Management

**Feature Branch**: `004-visits-spring-petclinic`

**Created**: 2025-01-31

**Status**: Draft

**Input**: User description: "Visits for spring_petclinic"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Schedule a New Visit for a Pet (Priority: P1)

A clinic receptionist or pet owner navigates to a pet's profile and books a future veterinary visit by providing a date and a brief description of the reason for the visit. The system validates the inputs and records the visit against the correct pet.

**Why this priority**: Scheduling a visit is the core capability of this feature. Without it, no other visit-related behaviour has meaning. It is the primary action users take and delivers immediate, standalone value.

**Independent Test**: Can be fully tested by navigating to an existing owner's pet, submitting the new visit form with a valid future date and a description, and confirming the visit appears in the pet's visit history on the owner detail page.

**Acceptance Scenarios**:

1. **Given** a known owner exists and that owner has a registered pet, **When** the user submits the new visit form with a date set to at least one day in the future and a description of 255 characters or fewer, **Then** the visit is saved and associated with the correct pet, and the user is redirected to the owner's detail page.
2. **Given** the visit form is displayed, **When** the page loads, **Then** the minimum selectable date is tomorrow (the next calendar day), preventing selection of today or any past date.
3. **Given** a visit is successfully saved, **When** the owner detail page is displayed, **Then** the new visit appears in the pet's visit history with the correct date and description.

---

### User Story 2 - Reject a Visit with a Non-Future Date (Priority: P2)

A user attempts to book a visit for today or a past date. The system detects the invalid date and returns the form with a clear error, without persisting any data.

**Why this priority**: Date validation is a critical business rule that protects the integrity of the scheduling system. It must be enforced consistently to prevent logically invalid records from entering the system.

**Independent Test**: Can be fully tested by submitting the new visit form with today's date and confirming the form is re-displayed with a validation error on the date field and no visit record is created.

**Acceptance Scenarios**:

1. **Given** an owner and pet exist, **When** the user submits the visit form with a date equal to today, **Then** the form is re-displayed with a validation error on the date field and no visit is persisted.
2. **Given** an owner and pet exist, **When** the user submits the visit form with a date in the past, **Then** the form is re-displayed with a validation error on the date field and no visit is persisted.
3. **Given** the form is re-displayed after a date validation failure, **When** the user corrects the date to a valid future date and resubmits, **Then** the visit is saved successfully.

---

### User Story 3 - Reject a Visit with an Oversized Description (Priority: P3)

A user submits a visit form with a description that exceeds the maximum allowed length. The system returns the form with a validation error on the description field without saving any data.

**Why this priority**: Description length validation protects data integrity and prevents storage anomalies. It is lower priority than date validation because it is less likely to occur in normal use, but it must still be enforced.

**Independent Test**: Can be fully tested by submitting the new visit form with a valid future date and a description of 256 or more characters, then confirming the form is re-displayed with a validation error on the description field and no visit is created.

**Acceptance Scenarios**:

1. **Given** an owner and pet exist, **When** the user submits the visit form with a valid future date but a description of 256 or more characters, **Then** the form is re-displayed with a validation error on the description field and no visit is persisted.
2. **Given** an owner and pet exist, **When** the user submits the visit form with a description of exactly 255 characters and a valid future date, **Then** the visit is saved successfully.
3. **Given** the form is re-displayed after a description validation failure, **When** the user shortens the description to 255 characters or fewer and resubmits, **Then** the visit is saved successfully.

---

### User Story 4 - Prevent Visit Creation for Unknown Owner or Pet (Priority: P2)

A request to create a visit references an owner ID or pet ID that does not exist in the system. The system rejects the request before constructing any visit object.

**Why this priority**: Referential integrity is a hard constraint. Allowing visits to be attached to non-existent owners or pets would corrupt the domain model. This must be enforced at the same priority level as date validation.

**Independent Test**: Can be fully tested by issuing a visit creation request with a non-existent owner ID or a pet ID that does not belong to the resolved owner, and confirming the system returns an error without persisting any visit.

**Acceptance Scenarios**:

1. **Given** no owner exists with the supplied owner ID, **When** a visit creation request is submitted, **Then** the system rejects the request with an error before any visit object is constructed.
2. **Given** a valid owner exists but the supplied pet ID does not belong to that owner, **When** a visit creation request is submitted, **Then** the system rejects the request with an error before any visit object is constructed.
3. **Given** both owner and pet are valid, **When** a visit creation request is submitted with valid inputs, **Then** the visit is created and associated with the correct pet under the correct owner.

---

### Edge Cases

- What happens when the owner ID in the URL does not match any owner in the system? The system must reject the request immediately with an error before any visit data is processed.
- What happens when the pet ID in the URL does not belong to the resolved owner (e.g., the pet exists but under a different owner)? The system must reject the request, preventing cross-owner visit assignment.
- What happens when the description is exactly 255 characters? The visit must be accepted as valid.
- What happens when the description is blank or empty? The visit must be rejected with a validation error on the description field.
- What happens when a client attempts to supply an ID value in the visit form submission? The system must silently discard the supplied ID and assign its own, preventing identity spoofing.
- What happens when the visit date is exactly today? The visit must be rejected; only dates strictly after today are permitted.
- What happens when a user attempts to delete an existing visit? The system must not expose any delete capability; visit records are append-only.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST resolve the owner by the `ownerId` path variable before processing any visit form request; if no owner is found, the request MUST be rejected with an error before any visit object is constructed.
- **FR-002**: System MUST resolve the pet by the `petId` path variable under the resolved owner before processing any visit form request; if the pet is not found under that owner, the request MUST be rejected with an error.
- **FR-003**: System MUST enforce that the visit date is strictly after the current calendar day; a date equal to today or any past date MUST be rejected with a field-level validation error on the date field.
- **FR-004**: System MUST enforce that the visit description is not blank and does not exceed 255 characters; violations MUST be rejected with a field-level validation error on the description field.
- **FR-005**: System MUST disallow client-supplied ID values on the visit form; any `id` or nested `*.id` field submitted in the form payload MUST be ignored and must not overwrite persisted visit identity.
- **FR-006**: System MUST associate a successfully validated visit with exactly one pet by appending it to the resolved pet's visit collection.
- **FR-007**: System MUST redirect the user to the owner's detail page after a visit is successfully saved.
- **FR-008**: System MUST re-display the visit creation form with all field errors intact when validation fails, without persisting any data.
- **FR-009**: System MUST expose the minimum allowed visit date (tomorrow) to the visit form view so the date picker can enforce the future-date constraint at the presentation layer.
- **FR-010**: System MUST NOT expose any capability to delete or modify existing visit records; visit persistence is append-only.
- **FR-011**: System MUST display the visit creation form in response to a request to create a new visit for a pet, pre-loading the resolved pet and owner context without relying on session-scoped state.

### Key Entities

- **Visit**: Represents a scheduled veterinary encounter for a pet. Key attributes: date (must be a future date), description (non-blank, maximum 255 characters). Always associated with exactly one pet. Append-only; records are never deleted or modified after creation.
- **Pet**: The animal for which the visit is being scheduled. Resolved by its identifier under a specific owner. Holds the collection of all visits associated with it.
- **Owner**: The registered guardian of the pet. Resolved by their identifier. Provides the context for locating the correct pet and serves as the destination for post-booking redirection.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A clinic user can complete the full visit booking flow — from opening the visit form to receiving confirmation — in under 2 minutes under normal operating conditions.
- **SC-002**: 100% of visit submissions with a date on or before today are rejected with a visible field-level error; no such visit record is persisted.
- **SC-003**: 100% of visit submissions with a blank description or a description exceeding 255 characters are rejected with a visible field-level error; no such visit record is persisted.
- **SC-004**: 100% of visit creation attempts referencing a non-existent owner or a pet not belonging to the resolved owner are rejected before any visit object is constructed.
- **SC-005**: All successfully saved visits appear in the pet's visit history on the owner detail page immediately after booking, with no additional navigation or refresh required.
- **SC-006**: No visit record created through the booking flow can be deleted or modified through any user-accessible path; the visit history is strictly append-only.
- **SC-007**: The visit form correctly pre-populates the minimum selectable date as tomorrow on every page load, with no reliance on cached or session-held state from a previous request.

## Assumptions

- The visit scheduling feature covers creation of new visit records only. Viewing visit history is a read-only concern satisfied by the existing owner detail page and is not a new deliverable of this feature.
- Only one visit can be submitted per form interaction; bulk visit creation is out of scope.
- The clinic operates as a single location; multi-clinic or multi-location scheduling is explicitly out of scope per project governance.
- Veterinarian assignment to a visit is not required at the time of booking; a visit may be created without designating a specific vet.
- The visit form is accessible to any authenticated user with access to the owner's pet profile; role-based access control beyond what already exists in the application is out of scope for this feature.
- Email or SMS confirmation notifications to the owner after booking are out of scope for this feature.
- The date picker minimum constraint is enforced at the presentation layer as a usability aid; server-side validation of the future-date rule is the authoritative enforcement point.
- All three database profiles (H2, MySQL, PostgreSQL) must support the visits schema without profile-specific changes to visit persistence logic.
- The `I18nPropertiesSyncTest` must remain green; any new user-facing validation messages introduced by this feature must be added to all supported message property files.