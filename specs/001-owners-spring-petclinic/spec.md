**Generated-at**: 2026-11-25T06:47:44Z
**Baseline-commit**: unknown
# Feature Specification: Owners

**Feature Branch**: `001-owners-spring-petclinic`

**Status**: Draft

**Input**: User description: "Owners for spring_petclinic"

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Register a New Owner (Priority: P1)

A clinic staff member opens the new-owner registration form, fills in the person's first name, last name, address, city, and a ten-digit telephone number, and submits. The system saves the record and immediately shows the new owner's detail page with a confirmation message.

**Why this priority**: Without owner registration no pet can be registered and no visit can be scheduled. It is the entry point for every other clinic workflow.

**Independent Test**: Navigate to the new-owner form, submit valid data, and verify the owner detail page loads with the saved values and the flash message "New Owner Created".

**Acceptance Scenarios**:

1. **Given** no owner record exists for a person, **When** a staff member submits the registration form with valid first name, last name, address, city, and a ten-digit telephone number, **Then** the owner is persisted and the user is redirected to `/owners/{id}` with the flash message "New Owner Created".
2. **Given** the registration form is open, **When** a staff member submits with any mandatory field blank or with a telephone number that is not exactly ten digits, **Then** the form is re-displayed with validation errors, the flash attribute `error` is set to "There was an error in creating the owner.", and no record is saved.
3. **Given** the registration form is open, **When** a staff member attempts to inject an `id` value via the form payload, **Then** the submitted `id` is silently discarded and the system assigns its own identifier.

---

### User Story 2 — Search for an Owner by Last Name (Priority: P2)

A staff member types a last name (or leaves the field blank) into the owner search form and submits. The system returns a paginated list of matching owners. If exactly one owner matches, the user is taken directly to that owner's detail page. If no owners match, an inline error is shown on the search form.

**Why this priority**: Staff must be able to locate existing owners before editing records, adding pets, or scheduling visits. Search is the primary navigation path to any owner.

**Independent Test**: Seed the database with at least two owners sharing a last-name prefix and one unique owner. Verify list pagination, single-result redirect, and no-result error independently of create or edit flows.

**Acceptance Scenarios**:

1. **Given** multiple owners exist, **When** a staff member searches by a last name that matches more than one owner, **Then** a paginated list of matching owners is displayed showing each owner's name and contact details.
2. **Given** exactly one owner exists with the searched last name, **When** the staff member submits the search, **Then** the user is redirected directly to that owner's detail page without showing a list.
3. **Given** no owner exists with the searched last name, **When** the staff member submits the search, **Then** a "not found" field error is displayed on the search form and no list is rendered.
4. **Given** the search form is submitted with no last name entered, **When** the system processes the request, **Then** the search is treated as a broadest-possible search (equivalent to an empty-string filter) and all owners are returned paginated.
5. **Given** a paginated search result exists, **When** a staff member requests a page number outside the valid range, **Then** the user is redirected to page 1 of the same search, preserving any last-name filter that was active.

---

### User Story 3 — View an Owner's Detail Page (Priority: P2)

A staff member navigates to an owner's detail page by ID. The page displays the owner's personal information and the list of their registered pets (ordered alphabetically by pet name).

**Why this priority**: The detail page is the hub from which staff access pet records and visit history. It must be reliable before edit, pet, and visit workflows are layered on top.

**Independent Test**: Load `/owners/{id}` for a known owner with at least two pets and verify that all owner fields and the pet list (in name order) are present.

**Acceptance Scenarios**:

1. **Given** an owner with ID `{id}` exists and has two or more pets, **When** a staff member navigates to `/owners/{id}`, **Then** the page displays the owner's first name, last name, address, city, and telephone, and lists the owner's pets in ascending alphabetical order by pet name.
2. **Given** no owner exists with the requested ID, **When** a staff member navigates to `/owners/{id}`, **Then** the system raises an error indicating the owner was not found.

---

### User Story 4 — Update an Owner's Contact Information (Priority: P3)

A staff member opens the edit form for an existing owner, changes the address, city, or telephone number, and submits. The system saves the updated values and returns the user to the owner's detail page.

**Why this priority**: Contact details change over time; the ability to correct them is important but does not block initial registration or search.

**Independent Test**: Load the edit form for an existing owner, change the telephone number to a different valid ten-digit value, submit, and verify the detail page reflects the new value.

**Acceptance Scenarios**:

1. **Given** an owner record exists, **When** a staff member submits the edit form with valid updated address, city, or telephone, **Then** the updated values are persisted and the owner's detail page reflects the changes immediately.
2. **Given** the edit form is open, **When** a staff member submits with an invalid telephone number (not exactly ten digits) or a blank mandatory field, **Then** the form is re-displayed with validation errors and no update is persisted.
3. **Given** the edit form is open for owner with path ID `X`, **When** a staff member submits a payload whose embedded owner ID does not match `X`, **Then** the update is rejected, an error flash attribute is set, and the user is redirected back to the edit form for owner `X`.

---

### Edge Cases

- What happens when an owner ID referenced in a URL does not exist in the repository? The system throws `IllegalArgumentException` with the message "Owner not found with id: {id}. Please ensure the ID is correct and the owner exists in the database." (Source: `OwnerController.findOwner` — `orElseThrow` block).
- What happens when the `page` query parameter is less than 1 or greater than the total number of pages? The system redirects to page 1, preserving any active `lastName` filter (Source: `OwnerController.processFindForm` — out-of-range page guard).
- What happens when a search returns exactly one result? The user is redirected directly to that owner's detail page rather than seeing a one-row list (Source: `OwnerController.processFindForm` — `getTotalElements() == 1` branch).
- What happens when `id` or `*.id` is submitted in an owner form payload? The `WebDataBinder` disallows those fields; they are silently dropped before binding (Source: `OwnerController.setAllowedFields`).
- What happens when a staff member submits a null last name in the search form? The system treats it as an empty string, broadening the search to all owners (Source: `OwnerController.processFindForm` — null-to-empty-string coercion).

---

## Business Rules *(mandatory)*

### BR-OWNERS-001 — ID fields are disallowed from web binding

**Rule**: The `id` field and any nested `*.id` field are removed from the set of bindable fields on every Owner form submission. A client cannot set or override an owner's internal identifier via a form POST.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` — `setAllowedFields`
- Quoted evidence: `dataBinder.setDisallowedFields("id", "*.id");`

**Status**: Verified
---

### BR-OWNERS-002 — Owner must exist before any operation proceeds

**Rule**: Any controller method that operates on an existing owner resolves the owner from the repository by path variable `ownerId`. If no owner is found, an `IllegalArgumentException` is thrown immediately and no further processing occurs.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` — `findOwner` `@ModelAttribute` method
- Quoted evidence: `this.owners.findById(ownerId).orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + ownerId + ". Please ensure the ID is correct " + "and the owner exists in the database."));`

**Status**: Verified
---

### BR-OWNERS-003 — Invalid owner creation form is rejected without persisting data

**Rule**: When the owner creation form is submitted and `BindingResult` reports errors, the system adds a flash attribute `error` with the value "There was an error in creating the owner." and returns the create/update form view. No `save` call is made.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` — `processCreationForm`
- Quoted evidence: `if (result.hasErrors()) { redirectAttributes.addFlashAttribute("error", "There was an error in creating the owner."); return VIEWS_OWNER_CREATE_OR_UPDATE_FORM; }`

**Status**: Verified
---

### BR-OWNERS-004 — Null last name in search is treated as empty string

**Rule**: Before executing an owner search, the system checks whether the submitted last name is null. If it is, the value is replaced with an empty string, which causes the repository query to return all owners rather than filtering by name.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` — `processFindForm`
- Quoted evidence: `if (lastName == null) { lastName = ""; // empty string signifies broadest possible search }`

**Status**: Verified
---

### BR-OWNERS-005 — Out-of-range page number redirects to page 1

**Rule**: After executing a paginated owner search, if the requested `page` parameter is less than 1 or greater than the total number of pages (treating zero total pages as 1), the system redirects to page 1. Any active `lastName` filter is preserved in the redirect.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` — `processFindForm`
- Quoted evidence: `if (page < 1 || page > Math.max(ownersResults.getTotalPages(), 1)) { redirectAttributes.addAttribute("page", 1); if (!lastName.isEmpty()) { redirectAttributes.addAttribute("lastName", lastName); } return "redirect:/owners"; }`

**Status**: Verified
---

### BR-OWNERS-006 — Single search result redirects directly to owner detail

**Rule**: When a paginated owner search returns exactly one result, the system bypasses the list view and redirects the user directly to that owner's detail page.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` — `processFindForm`
- Quoted evidence: `if (ownersResults.getTotalElements() == 1) { owner = ownersResults.iterator().next(); return "redirect:/owners/" + owner.getId(); }`

**Status**: Verified
---

### BR-OWNERS-007 — Owner update rejects mismatched path ID and payload ID

**Rule**: When an owner edit form is submitted, if the owner object's embedded ID does not match the `ownerId` path variable, the update is rejected with an error flash attribute and the user is redirected back to the edit form for the path owner ID.

**Source**:
- `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java` — update mismatch test
- Quoted evidence: `mockMvc.perform(MockMvcRequestBuilders.post("/owners/{ownerId}/edit", pathOwnerId).flashAttr("owner", owner)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/owners/" + pathOwnerId + "/edit")).andExpect(flash().attributeExists("error"));`

**Status**: Verified
---

### BR-OWNERS-008 — Pet must exist under the owner before a visit can be created

**Rule**: Before a visit is created, the system retrieves the pet by ID from the owner aggregate. If `owner.getPet(petId)` returns null, an `IllegalArgumentException` is thrown and no visit is created.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `loadPetWithVisit`
- Quoted evidence: `Pet pet = owner.getPet(petId); if (pet == null) { throw new IllegalArgumentException("Pet with id " + petId + " not found for owner with id " + ownerId + "."); }`

**Status**: Verified
---

### BR-OWNERS-009 — Visit date must be at least tomorrow

**Rule**: The minimum allowed visit date is `LocalDate.now().plusDays(1)`. The form exposes this value as the `minVisitDate` model attribute. Same-day and past dates are not permitted.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `minVisitDate` `@ModelAttribute` method
- Quoted evidence: `@ModelAttribute("minVisitDate") public LocalDate minVisitDate() { return LocalDate.now().plusDays(1); }`

**Status**: Verified
---

### BR-OWNERS-010 — Visit ID fields are disallowed from web binding

**Rule**: The `id` and `*.id` fields are removed from the bindable set on every Visit form submission, preventing a client from setting or overriding a visit's internal identifier.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `setAllowedFields`
- Quoted evidence: `dataBinder.setDisallowedFields("id", "*.id");`

**Status**: Verified
---

### BR-OWNERS-011 — Visit records are append-only

**Rule**: Visits are added to a pet via `pet.addVisit(visit)` and are never deleted. The visit history for any pet is strictly append-only.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/VisitController.java` — `processNewVisitForm`; constitution Principle II
- Quoted evidence: `pet.addVisit(visit)` (VisitController); "Visit records MUST NOT be deleted once created; visit history MUST be append-only." (constitution)

**Status**: Verified
---

### BR-OWNERS-012 — Owner telephone must be exactly ten digits

**Rule**: The `telephone` field on Owner is constrained by `@Pattern(regexp="\\d{10}")`. Any value that is not exactly ten consecutive digits fails validation and the form is rejected.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` — `telephone` field annotation
- Quoted evidence: `@Pattern(regexp="\\d{10}")`

**Status**: Verified
---

### BR-OWNERS-013 — Owner address and city are mandatory and length-bounded

**Rule**: The `address` field is `@NotBlank` with `@Size(max=255)`. The `city` field is `@NotBlank` with `@Size(max=80)`. Both must be present and within their respective length limits for an owner record to be valid.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` — `address` and `city` field annotations
- Quoted evidence: `@NotBlank @Size(max=255)` (address); `@NotBlank @Size(max=80)` (city)

**Status**: Verified
---

### BR-OWNERS-014 — A pet must belong to exactly one owner

**Rule**: The `Pet` entity is related to `Owner` via a `@OneToMany` / `@ManyToOne` relationship where `owner_id` is the foreign key. A pet cannot exist without an owning owner record.

**Source**:
- `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` — `@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)` on the `pets` collection; constitution Principle II
- Quoted evidence: `@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)` joined on `owner_id`; "A Pet MUST always belong to exactly one Owner." (constitution)

**Status**: Verified
---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow creation of a new Owner by accepting a form submission with first name, last name, address, city, and telephone; on success the owner is persisted and the user is redirected to the owner's detail page with the flash message "New Owner Created". *(Source: BR-OWNERS-003, OwnerController.processCreationForm)*
- **FR-002**: System MUST reject an Owner creation or update form submission that fails validation, re-display the form with errors, set the flash attribute `error` to "There was an error in creating the owner.", and make no persistence call. *(Source: BR-OWNERS-003)*
- **FR-003**: System MUST support searching for owners by last name via a paginated results view; when exactly one owner matches the search the system MUST redirect directly to that owner's detail page; when no owners match the system MUST display a "not found" field error. *(Source: BR-OWNERS-004, BR-OWNERS-005, BR-OWNERS-006)*
- **FR-004**: System MUST treat a null or absent last-name search value as an empty string, returning all owners in the paginated results. *(Source: BR-OWNERS-004)*
- **FR-005**: System MUST redirect any paginated owner search request whose `page` parameter is out of range to page 1, preserving any active last-name filter. *(Source: BR-OWNERS-005)*
- **FR-006**: System MUST disallow binding of `id` and `*.id` fields on all Owner and Visit form submissions, preventing client-side manipulation of internal identifiers. *(Source: BR-OWNERS-001, BR-OWNERS-010)*
- **FR-007**: System MUST reject an owner update where the payload owner ID does not match the path `ownerId`, set an error flash attribute, and redirect back to the edit form for the path owner. *(Source: BR-OWNERS-007)*
- **FR-008**: System MUST display an owner's detail page showing first name, last name, address, city, telephone, and the owner's pets listed in ascending alphabetical order by pet name. *(Source: Owner entity `@OrderBy("name")` on pets collection)*
- **FR-009**: System MUST raise an error when any operation references an owner ID that does not exist in the repository. *(Source: BR-OWNERS-002)*
- **FR-010**: System MUST allow creation of a Visit for a Pet only when the Pet exists under the specified Owner and the visit date is at least one day in the future; visits MUST be appended to the pet's history and MUST NOT be deleted. *(Source: BR-OWNERS-008, BR-OWNERS-009, BR-OWNERS-011)*
- **FR-011**: System MUST enforce that Owner `telephone` is exactly ten digits, `address` is non-blank and at most 255 characters, and `city` is non-blank and at most 80 characters before persisting any owner record. *(Source: BR-OWNERS-012, BR-OWNERS-013)*

### Key Entities

- **Owner**: A registered clinic customer identified by first name, last name, address (max 255 chars), city (max 80 chars), and a ten-digit telephone number. Owns zero or more Pets. Internal identifier is system-assigned and cannot be set via form submission.
- **Pet**: An animal registered under exactly one Owner. Identified by a system-assigned integer ID and a name. Ordered alphabetically by name when displayed under an Owner. Carries a collection of Visits.
- **PetType**: A classification for a Pet (e.g., cat, dog). Selected from a fixed list surfaced by the repository; required at pet registration.
- **Visit**: A veterinary appointment record attached to a Pet. Has a date that must be at least tomorrow. Append-only; deletion is not permitted. Internal identifier is system-assigned and cannot be set via form submission.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A staff member can complete new owner registration — from opening the form to seeing the owner detail page — without encountering an unhandled error when all required fields are supplied correctly.
- **SC-002**: A staff member searching by last name reaches the correct owner detail page in two interactions or fewer when exactly one matching owner exists.
- **SC-003**: All owner form submissions that contain invalid or missing field values are rejected before any data is written, and the staff member receives a visible error message identifying the problem.
- **SC-004**: Owner search results are paginated and remain navigable; no out-of-range page request results in an error page — the user is always returned to a valid page.
- **SC-005**: An owner's contact information updated via the edit form is reflected on the detail page immediately after the successful submission, with no stale data displayed.
- **SC-006**: A visit cannot be scheduled for a date in the past or for today; any attempt to do so is blocked before the record is written.
- **SC-007**: The complete visit history for any pet is always retrievable and no visit record is ever removed from that history.

---

## Assumptions

- The owner search form is accessible to all authenticated clinic staff; no role-based restriction beyond general clinic access is modelled in the current codebase.
- The ten-digit telephone constraint (`\d{10}`) is applied without country-code prefix; international formats are out of scope for this feature as implemented.
- Pet registration and visit scheduling are downstream workflows that depend on the Owners feature but are not fully specified here; this specification covers only the Owner aggregate and the visit-creation entry point within `VisitController`.
- The `PetType` list is managed separately and is treated as a read-only reference from the Owners module's perspective.
- Owner PII (name, address, telephone) must not appear in application logs or error messages surfaced to the UI, per the project constitution Principle III.
- The H2 in-memory profile is used for unit and slice tests; MySQL and PostgreSQL profiles are used for database integration tests, per the project constitution.
- Billing, payment, and multi-location management are explicitly out of scope per the project constitution Principle V.