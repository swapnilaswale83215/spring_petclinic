**Generated-at**: 2026-11-25T06:48:48Z
**Baseline-commit**: unknown
# Feature Specification: Veterinarians

**Feature Branch**: `003-veterinarians-spring-petclinic`

**Status**: Draft

**Input**: User description: "Veterinarians for spring_petclinic"

---

## Business Rules

### BR-VETERINARIANS-001 — Vet list wrapper must never return null

Rule:
The `Vets` wrapper class initialises its internal list as an empty `ArrayList` so that `getVetList()` always returns a valid (possibly empty) collection and never `null`. Downstream serialisers and view renderers can iterate the list unconditionally.

Source:
- `src/main/java/org/springframework/samples/petclinic/vet/Vets.java`
- Quoted evidence: `private List<Vet> vets;` initialised via `getVetList()` which contains `if (vets == null) { vets = new ArrayList<>(); }`

Status: Verified
---

### BR-VETERINARIANS-002 — Vet list is serialisable as both JSON and XML

Rule:
The `Vets` wrapper is annotated with `@XmlRootElement` and exposes its list via a method annotated `@XmlElement`, making the collection marshallable to XML (via Spring MVC `MarshallingView`) and to JSON (via the standard Spring MVC content-negotiation stack).

Source:
- `src/main/java/org/springframework/samples/petclinic/vet/Vets.java`
- Quoted evidence: class-level `@XmlRootElement` and `@XmlElement` on `getVetList()`

Status: Verified
---

### BR-VETERINARIANS-003 — Specialty assignment is optional (zero or many per vet)

Rule:
A veterinarian may be associated with zero or more specialties. The absence of any specialty is a valid state; such a vet must still appear in all list responses without error.

Source:
- `src/main/resources/db/h2/schema.sql` — `vet_specialties` join table (no NOT NULL / mandatory FK constraint forcing at least one row per vet)
- Constitution §II: "Veterinarian specialty assignments are optional (zero or many per vet); this MUST NOT be changed to a required constraint."
Status: Inferred

_Note: demoted from Verified — cited source was not quoted from the read context._
---

### BR-VETERINARIANS-004 — Vet list query results are cached

Rule:
The veterinarian list query is backed by a JCache-managed cache region configured in `CacheConfiguration`. The cache is intended to reduce repeated database reads for a dataset that changes infrequently.

Source:
- `src/main/java/org/springframework/samples/petclinic/system/CacheConfiguration.java` — `JCacheManagerCustomizer` bean configures the `vets` cache region.

Status: Verified
---

### BR-VETERINARIANS-005 — Cache statistics are exposed via JMX

Rule:
The JCache configuration enables statistics and management on the `vets` cache region so that operations teams can monitor hit/miss ratios via JMX without redeploying the application.

Source:
- `src/main/java/org/springframework/samples/petclinic/system/CacheConfiguration.java` — `MutableConfiguration` with `setStatisticsEnabled(true)` and `setManagementEnabled(true)`.

Status: Verified
---

### BR-VETERINARIANS-006 — Vet and Specialty share a many-to-many join table

Rule:
The `vets` and `specialties` tables are linked through the `vet_specialties` join table. A specialty record is independent of any individual vet; the same specialty may be held by multiple vets.

Source:
- `src/main/resources/db/h2/schema.sql` — `vet_specialties (vet_id, specialty_id)` join table definition.
Status: Inferred

_Note: demoted from Verified — cited source was not quoted from the read context._
---

## User Scenarios & Testing

### User Story 1 — View all veterinarians with their specialties (Priority: P1)

A clinic visitor or staff member navigates to the veterinarians listing page and sees every registered vet alongside the specialties they hold. Vets with no specialty show a clear "none" indicator rather than an error or blank.

**Why this priority**: This is the primary read path for the entire Veterinarians module. All other stories depend on this list being rendered correctly.

**Independent Test**: Navigate to `/vets` (or `/vets.html`) with the sample data loaded. Confirm every vet row appears and that specialty names are displayed. Delivers a usable vet directory with no further work required.

**Acceptance Scenarios**:

1. **Given** the application is running and veterinarian records exist in the database, **When** a user navigates to the veterinarians listing page, **Then** all veterinarians are displayed in a list with their associated specialty names visible.
2. **Given** a veterinarian exists with no specialties assigned, **When** the veterinarians list is rendered, **Then** that veterinarian appears in the list with an empty or "none" specialty indicator and no error is thrown.
3. **Given** the veterinarians list has been loaded at least once, **When** the same page is requested again without any data change, **Then** the response is served from the cache without an additional database query.

---

### User Story 2 — Retrieve veterinarian list as XML (Priority: P2)

An integrating system or administrative tool requests the veterinarian list in XML format. The response uses the `Vets` wrapper element as the root and lists each vet as a child element with their specialties.

**Why this priority**: The XML serialisation path is an explicitly modelled capability (`Vets` / `MarshallingView`) and must be verified independently of the HTML view.

**Independent Test**: Issue an HTTP request to `/vets` with `Accept: application/xml`. Confirm the response body is valid XML rooted at `<vets>` containing `<vet>` child elements. Delivers machine-readable vet data without requiring the HTML view.

**Acceptance Scenarios**:

1. **Given** the application is running and vet records exist, **When** a client requests the vet list with an XML content-type preference, **Then** the response body is well-formed XML with `<vets>` as the root element and each vet represented as a child element including their specialties.
2. **Given** no vets exist in the database, **When** a client requests the vet list as XML, **Then** the response is a valid XML document containing an empty `<vets>` element and no error is returned.

---

### User Story 3 — Regression: no unintended side-effects on existing modules (Priority: P3)

After any change to the Veterinarians module, the full test suite passes without modification to Owner, Person, schema, or data-script files.

**Why this priority**: The constitution mandates that the `owner`, `vet`, and `system` package boundaries are respected and that cross-package dependencies are introduced only through well-defined interfaces. This story guards that boundary.

**Independent Test**: Run the full Maven test suite (`./mvnw test`). Confirm `ClinicServiceTests` and `PetClinicConcurrencyTests` both pass and that no Owner, Person, schema, or data-script file has been altered.

**Acceptance Scenarios**:

1. **Given** the main baseline is green, **When** the full test suite is executed after any veterinarian-related change, **Then** zero previously passing tests fail.
2. **Given** a veterinarian-related change is proposed, **When** the diff is reviewed, **Then** no file under `owner/`, `Person.java`, `schema.sql`, or any seed-data script has been modified.

---

### Edge Cases

- What happens when the `vets` cache region is not yet warm and the database is unavailable? The system should surface a clear error rather than returning a null or partially populated list.
- What happens when a `Specialty` record exists in the `specialties` table but has no corresponding row in `vet_specialties`? The specialty must not appear on any vet's record and must not cause a query error.
- What happens when `getVetList()` is called on a freshly constructed `Vets` instance before any vets are added? Per BR-VETERINARIANS-001, it must return an empty `ArrayList`, never `null`.
- What happens when two concurrent requests hit the vet list endpoint simultaneously before the cache is populated? Only one database query should execute; the second request should receive the cached result.

---

## Requirements

### Functional Requirements

- **FR-001**: The system MUST expose a list of all registered veterinarians serialisable as both JSON and XML, using the `Vets` wrapper class as the root container.
  *Source: `src/main/java/org/springframework/samples/petclinic/vet/Vets.java` — `@XmlRootElement`, `@XmlElement` on `getVetList()`*

- **FR-002**: The system MUST ensure that `getVetList()` on the `Vets` wrapper never returns `null`; when no vets are present the method MUST return an empty collection.
  *Source: `src/main/java/org/springframework/samples/petclinic/vet/Vets.java` — null-guard initialisation in `getVetList()`*

- **FR-003**: The system MUST allow each veterinarian to be associated with zero or more specialties; a veterinarian with no specialty MUST be treated as valid and MUST appear in all list responses.
  *Source: `src/main/resources/db/h2/schema.sql` — `vet_specialties` join table; Constitution §II*

- **FR-004**: The system MUST cache veterinarian list query results using the configured JCache region so that repeated requests for the vet list do not each trigger a database query.
  *Source: `src/main/java/org/springframework/samples/petclinic/system/CacheConfiguration.java` — `JCacheManagerCustomizer`*

- **FR-005**: The system MUST expose cache statistics for the veterinarian cache region via JMX so that operations teams can monitor cache behaviour without redeployment.
  *Source: `src/main/java/org/springframework/samples/petclinic/system/CacheConfiguration.java` — `setStatisticsEnabled(true)`, `setManagementEnabled(true)`*

- **FR-006**: The system MUST render the veterinarian list in the HTML view with each vet's specialty names displayed; vets with no specialty MUST display a "none" or equivalent indicator rather than an error or blank cell.
  *Source: User Story 1 acceptance scenarios; BR-VETERINARIANS-003*

- **FR-007**: The system MUST NOT modify Owner, Person, schema, or data-script files as a side-effect of veterinarian module changes.
  *Source: Constitution §I — package boundaries; User Story 3*

### Key Entities

- **Vet**: A veterinarian registered at the clinic. Inherits `id` (Integer), `firstName` (String), `lastName` (String) from `Person`. Associated with zero or more `Specialty` records via the `vet_specialties` join table.
  *Source: `src/main/resources/db/h2/schema.sql`*

- **Specialty**: A medical area of expertise a vet may hold. Attributes: `id` (Integer, auto-generated), `name` (String, max 80 characters). Linked to `Vet` via the `vet_specialties` join table (many-to-many).
  *Source: `src/main/resources/db/h2/schema.sql`*

- **Vets**: A wrapper/container object aggregating a `List<Vet>`. Used as the XML root element for `MarshallingView` responses and as the JSON envelope. Not directly ORM-mapped.
  *Source: `src/main/java/org/springframework/samples/petclinic/vet/Vets.java`*

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: All registered veterinarians and their specialties are visible to a user on the veterinarians listing page in a single navigation step, with no additional clicks required.

- **SC-002**: A veterinarian with no assigned specialty appears in the listing without any error message or missing-data indicator beyond a clearly labelled "none" value.

- **SC-003**: Repeated requests for the veterinarian list after the first load do not each trigger a new database read; the cache serves subsequent requests within the same application lifecycle.

- **SC-004**: The veterinarian list is retrievable as well-formed XML by any client that requests it, with `<vets>` as the document root and each vet's specialties nested within their element.

- **SC-005**: The full automated test suite (including `ClinicServiceTests` and `PetClinicConcurrencyTests`) passes without modification after any change to the Veterinarians module.

- **SC-006**: Cache hit/miss statistics for the veterinarian cache region are observable via JMX without any application restart or redeployment.

---

## Assumptions

- The veterinarian listing is a read-only directory; no create, update, or delete operations for vet records are in scope for this feature. Vet management (add/edit/remove vets or specialties) is a separate concern.
- The HTML view for the vet list is rendered server-side via Thymeleaf, consistent with the application's modular monolith architecture and constitution §I.
- Seed data for vets and specialties is provided by the existing H2 and MySQL/PostgreSQL data scripts; this feature does not introduce new seed records.
- The `vets` cache region name used in `CacheConfiguration` matches the cache name referenced by the `VetRepository` or service layer; no rename is required.
- Performance targets (e.g., page load time) are not specified in the source evidence and are therefore not stated as numbered success criteria; standard web application responsiveness is assumed.
- The `ClinicServiceTests` suite exercises `VetRepository` directly and serves as the integration test baseline for this module; no new test class is required beyond what already exists unless new behaviour is added.
- Multi-location or multi-clinic vet directories are explicitly out of scope per Constitution §V.