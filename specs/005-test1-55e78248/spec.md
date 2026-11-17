# Feature Specification: Case-Insensitive Owner Search

**Feature Branch**: `005-test1-55e78248`

**Created**: 2026-11-13

**Status**: Draft

**Input**: User description: "Make the owner search case-insensitive, so 'davis' finds 'Davis'."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Front-Desk Staff Find Owners by Lower-Case Last Name (Priority: P1)

A front-desk staff member types a last name in lower case (e.g., "davis") into the Find Owners search form and submits it. The application returns the same owners that a correctly-cased search (e.g., "Davis") would return, eliminating the "No owners found" false negative that currently causes duplicate record creation.

**Why this priority**: This is the core defect driving the change. Every other story depends on the search being case-insensitive. Fixing this single path directly prevents duplicate owner records and is the entire business justification.

**Independent Test**: Can be fully tested by submitting `/owners?lastName=davis` against the sample data (which contains 2 owners with last name Davis) and confirming that exactly 2 owners are returned — the same 2 returned by `/owners?lastName=Davis`.

**Acceptance Scenarios**:

1. **Given** the sample data holds 2 owners with last name Davis (Betty id 2, Harold id 4) and the application runs on H2, **When** a user searches `/owners?lastName=davis`, **Then** the result lists exactly the same 2 owners as the search for "Davis". *(AC-1)*
2. **Given** the same sample data, **When** a user searches `/owners?lastName=DAVIS`, **Then** the result lists exactly the same 2 owners as the search for "Davis". *(AC-2)*
3. **Given** the same sample data, **When** a user searches `/owners?lastName=Davis`, **Then** the result lists the same 2 owners as before the change, confirming no regression. *(AC-3)*
4. **Given** the same sample data on MySQL and on PostgreSQL, **When** a user searches "davis", "DAVIS" and "Davis", **Then** each search returns the same 2 owners as on H2, in the same order. *(AC-4)*

---

### User Story 2 - Search Results Are Consistently Ordered Across All Database Profiles (Priority: P2)

A front-desk staff member performs an owner search and sees results listed in a predictable, stable order — last name, then first name — regardless of which database the application is running against (H2, MySQL, PostgreSQL). Previously the query had no explicit sort, so order was undefined and could differ between profiles.

**Why this priority**: The explicit order is a product-owner-confirmed requirement and a must-not-break item. Without it, the same search can return owners in different sequences on different profiles, making the behaviour unpredictable and untestable. It must be delivered together with the case-insensitive fix.

**Independent Test**: Can be tested independently by searching "Da" (which matches multiple owners including Betty Davis and Harold Davis) on each of the three database profiles and confirming that Betty Davis always appears before Harold Davis, and that the full result sequence is identical across profiles.

**Acceptance Scenarios**:

1. **Given** the sample data and the application running on H2, MySQL, and PostgreSQL, **When** a user searches "Da", **Then** the owners are returned ordered by last name then first name (Betty Davis before Harold Davis), and the order is identical on all three profiles. *(AC-11)*
2. **Given** the sample data holds more owners than one page shows, **When** a user submits an empty search, **Then** all owners are listed across 2 pages of 5, with no overlap or gap between pages, and the page order is the same on all three profiles. *(AC-8)*
3. **Given** two owners with identical last name and first name, **When** a search returns both, **Then** they are further ordered by their internal identifier so the sequence is deterministic.

---

### User Story 3 - Prefix Matching Continues to Work Case-Insensitively (Priority: P2)

A front-desk staff member types a partial last name in any case (e.g., "da" or "Da") and receives all owners whose last name begins with those letters, regardless of the capitalisation used. The existing prefix-matching behaviour is preserved and extended to be case-insensitive.

**Why this priority**: Prefix matching is a must-not-break item and is the primary way staff narrow a long list. Losing it or making it case-sensitive for partial terms would be a regression as serious as the original defect.

**Independent Test**: Can be tested by submitting `/owners?lastName=da` and `/owners?lastName=Da` against the sample data and confirming both return every owner whose last name starts with "Da", including the 2 Davis owners, with no false negatives.

**Acceptance Scenarios**:

1. **Given** the sample data, **When** a user searches "Da", **Then** every owner whose last name starts with "Da" (including both Davis owners) is returned. *(AC-10)*
2. **Given** the same sample data, **When** a user searches "da", **Then** the same owners are returned as for "Da". *(AC-10)*
3. **Given** the sample data, **When** a user searches "Daviss" (a term that matches no owner), **Then** an empty result is returned and no error occurs.
4. **Given** a search term containing a wildcard character such as "%" or "_", **When** the search runs, **Then** the term is treated as a literal string and does not match all owners. *(SPEC-APP-3)*

---

### User Story 4 - Single-Match Lower-Case Search Redirects Directly to Owner Page (Priority: P2)

A front-desk staff member types a lower-case last name that matches exactly one owner and is taken directly to that owner's detail page, without seeing an intermediate list. This mirrors the existing behaviour for correctly-cased single-match searches.

**Why this priority**: The single-match redirect is a must-not-break item. If a lower-case search now returns one result but lands on a list page instead of redirecting, the user experience regresses for the most common lookup pattern.

**Independent Test**: Can be tested by searching a lower-case last name that is unique in the sample data and confirming the application redirects to `/owners/{id}` rather than rendering the list view.

**Acceptance Scenarios**:

1. **Given** a last name with exactly one owner in the data, searched in lower case, **When** a user submits the search, **Then** the application opens that owner's page directly, with no list page shown. *(AC-9)*
2. **Given** a search term in lower case that matches no owner, **When** a user submits the search, **Then** the application returns to the Find Owners form with the not-found error message, unchanged from current behaviour.

---

### User Story 5 - Find Owners Page and URLs Are Unchanged (Priority: P3)

A front-desk staff member opens the Find Owners page, fills in the search form, and submits it. The page loads at the same URL, the form submits to the same endpoint, and the page layout is visually identical to before the change. No template or URL has been modified.

**Why this priority**: This is a must-not-break constraint rather than new value. It is lower priority because it is a regression guard; the page already works and must simply continue to work.

**Independent Test**: Can be tested by loading `/owners/find`, submitting the form, and confirming the request goes to `/owners?lastName=` with no change to the rendered HTML structure or URL paths.

**Acceptance Scenarios**:

1. **Given** the application is running, **When** a user opens `/owners/find` and submits the form, **Then** the page and the form load at the same URLs, with no change to the page layout, and the search goes to `/owners?lastName=`. *(AC-7)*
2. **Given** the application is running, **When** a user navigates to `/owners/find`, **Then** the Thymeleaf templates `findOwners.html` and `ownersList.html` are rendered without modification.

---

### User Story 6 - Performance of Owner Search Is Not Degraded (Priority: P3)

A front-desk staff member performs owner searches after the change and experiences response times that are no worse than before the change, within the agreed tolerance. The case-folding added to the query does not introduce a measurable slowdown on the production-representative database profile (PostgreSQL).

**Why this priority**: Performance is a must-not-break item but is lower priority than correctness. The tolerance is generous (10% or 5 ms, whichever is larger) and the sample data is small, so regression is unlikely. It is included to satisfy the product owner's KPI-3 commitment.

**Independent Test**: Can be tested by running the agreed 200-search mix (50 each of "davis", "DAVIS", "Da", and empty) on PostgreSQL before and after the change, using the same script and environment, and comparing p95 response times.

**Acceptance Scenarios**:

1. **Given** the baseline p95 was measured on the unchanged codebase, on the PostgreSQL profile with the sample data, with 200 searches (50 each of "davis", "DAVIS", "Da" and empty search) run by a simple load script, **When** the same script and the same 200-search mix run against the changed search in the same environment, **Then** p95 is at most baseline p95 + max(10% of baseline p95, 5 ms). *(AC-6, KPI-3)*
2. **Given** the EXPLAIN output of the new query is captured on PostgreSQL, **When** the p95 target is met, **Then** the EXPLAIN and generated SQL are attached to the release report, with no owner names or bound values included in the attachment. *(SPEC-APP-7, constitution V)*

---

### Edge Cases

- What happens when the search term is empty? An empty search must still list all owners with paging (page size 5, 2 pages for the 10-owner sample data). *(AC-8)*
- What happens when the search term contains SQL wildcard characters "%" or "_"? They must be escaped and treated as literals, returning no owners rather than all owners. *(SPEC-APP-3)*
- What happens when the search term contains a single quote or other SQL fragment? The term is passed as a bound parameter; no SQL injection is possible and no error is raised. *(SPEC-APP-3)*
- What happens when the requested page number is out of range? The application redirects to page 1, unchanged from current behaviour. *(SPEC-APP-4)*
- What happens when two owners share the same last name and first name? They are further ordered by their internal identifier so the sequence is deterministic and stable across pages. *(SPEC-APP-2)*
- What happens on H2, where the column is already stored as VARCHAR_IGNORECASE? The change must not alter H2's existing case-insensitive behaviour or break its index use. The new query must produce the same correct results on H2 as on PostgreSQL and MySQL. *(risk: behaviour differs between profiles)*
- What happens on MySQL, where the collation is not explicitly set in the repository? The case-folding approach must not depend on the server collation; lower() on both sides of the comparison must produce consistent results regardless of the MySQL collation in effect. *(risk: behaviour differs between profiles)*
- What happens when trailing spaces are present in the search term? Trailing-space handling (Jira PETC-8) is out of scope. The controller's existing strip() behaviour on the main branch is preserved and not modified by this change.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001** *(SPEC-APP-1)*: The owner search by last name MUST be case-insensitive, so that "davis", "DAVIS" and "Davis" each return the same set of owners on H2, MySQL and PostgreSQL. The method name `findByLastNameStartingWith`, its parameters, and its `Page<Owner>` return type MUST remain unchanged.
- **FR-002** *(SPEC-APP-1)*: The case-insensitive comparison MUST be implemented by folding both the stored last name and the search term to lower case within the database query, so that the result does not depend on the database collation of any profile.
- **FR-003** *(SPEC-APP-2)*: Search results MUST be ordered by last name (case-insensitive), then first name (case-insensitive), then by internal owner identifier as a tie-breaker. This order MUST be identical on H2, MySQL and PostgreSQL for the sample data.
- **FR-004** *(SPEC-APP-3)*: The search term MUST be passed to the database as a bound parameter. Any LIKE wildcard characters ("%" and "_") present in the term MUST be escaped so they match only themselves and do not expand the result set.
- **FR-005** *(SPEC-APP-3)*: The query MUST return `Owner` entities only, with no new fields projected or exposed. Owner name, address and telephone data MUST be unchanged after any search operation.
- **FR-006** *(SPEC-APP-4)*: `OwnerController.processFindForm` MUST behave as before: the same URLs (`/owners/find`, `/owners?lastName=`), the same page size of 5, the out-of-range page redirect, the not-found error, the single-match redirect to `/owners/{id}`, and the list view. No template changes are permitted.
- **FR-007** *(SPEC-APP-4)*: A lower-case search term that matches exactly one owner MUST cause the application to redirect directly to that owner's detail page, with no intermediate list page.
- **FR-008** *(SPEC-APP-4)*: An empty search term MUST return all owners with paging, using the same page size and producing the same page count as before the change.
- **FR-009** *(SPEC-APP-5)*: New automated tests MUST cover lower-case, upper-case and mixed-case owner searches. At least 3 new case-variant tests MUST be present and passing. Tests MUST run against real repositories on H2 (via `ClinicServiceTests`) and on PostgreSQL and MySQL (via Testcontainers integration tests) in CI without being skipped.
- **FR-010** *(SPEC-APP-5)*: The new PostgreSQL Testcontainers test for "davis" MUST fail on the unchanged main branch, demonstrating that the test detects the defect before the fix is applied.
- **FR-011** *(SPEC-APP-6)*: All existing tests MUST continue to pass with no modifications to their assertions or stubs. `OwnerControllerTests`, `OwnerTests`, `ClinicServiceTests` and `PetClinicConcurrencyTests` MUST all pass. The `pom.xml` MUST have no dependency changes. `OwnerController` and the Thymeleaf templates MUST have no diff.
- **FR-012** *(SPEC-APP-7)*: The owner search p95 response time MUST be measured on the unchanged codebase (baseline) and again after the change, using the same 200-search mix (50 each of "davis", "DAVIS", "Da" and empty search) on the PostgreSQL profile with the sample data. The EXPLAIN output and generated SQL (without bound owner-name values) MUST be captured and attached to the release report.
- **FR-013** *(SPEC-APP-8)*: The change MUST be releasable as a normal redeploy with no write-freeze, no data migration and no schema change (unless the KPI-3 gate defined in ADR-APP-3 fires and the product owner approves a functional index as a separate decision). Rollback MUST be achievable by redeploying the previous build with no data repair step.
- **FR-014**: The search change MUST be confined to `OwnerRepository`. No change is permitted to `OwnerController`, the Thymeleaf templates, the `Owner` or `Person` entity classes, or any schema or data script, unless the KPI-3 performance gate requires a functional index.
- **FR-015**: SQL logging used during baseline and post-change measurement MUST NOT capture or retain bound owner-name values in any log file or report attachment, in compliance with the sensitive-data handling principle. *(constitution V)*

### Key Entities

- **Owner**: The pet's registered guardian. Relevant attributes for this change: `lastName` (the field searched), `firstName` (used in result ordering). Sensitive fields — name, address, telephone number — are a designated sensitive zone and MUST NOT be exposed in new code paths. The `Owner` entity class and its schema are unchanged by this feature.
- **OwnerRepository**: The Spring Data JPA repository interface through which all owner searches are performed. The single method `findByLastNameStartingWith(String lastName, Pageable pageable)` is the target of this change. Its method name, parameter types and `Page<Owner>` return type are fixed interfaces that all callers and test stubs depend on.
- **Page\<Owner\>**: The paginated result returned by the search. Page size (5), total element count, and page boundaries must be unaffected by the case-insensitive change.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **KPI-1**: On the sample data (10 owners, 2 with last name Davis: Betty id 2 and Harold id 4), searching "davis", "DAVIS" and "Davis" each returns exactly 2 owners, and the three result sets are identical. Baseline: before the change, "Davis" returns 2 owners; "davis" and "DAVIS" return 0 on PostgreSQL (the KPI-1 proof profile). The baseline MUST be confirmed by running the three searches on each profile before any code change is made, because H2 is already case-insensitive and MySQL behaviour depends on an unconfirmed collation.
- **KPI-2**: Zero existing tests fail after the change. At least 3 new case-variant tests (lower-case, upper-case, mixed-case) are present and passing on H2, PostgreSQL and MySQL. Baseline: the current pass/fail count of the full test suite on main, captured before the change.
- **KPI-3**: Owner search p95 response time on the PostgreSQL profile with the sample data, measured over 200 searches (50 each of "davis", "DAVIS", "Da" and empty search), is at most baseline p95 + max(10% of baseline p95, 5 ms). Baseline: measured on the unchanged main branch using the same script, the same 200-search mix and the same PostgreSQL environment before any code change. The measurement MUST include a warm-up run, at least 3 baseline runs (using the median or worst as the reference), and p95 reported per search term as well as in aggregate. Note: because the sample data contains only 10 owners, this measurement cannot detect an index regression at production scale (up to 10,000 owners per SC-002 of spec 001). A supplementary, report-only run on a scaled dataset of approximately 10,000 owners MUST be performed and its EXPLAIN ANALYZE output attached, so the approver can accept the no-index decision with full evidence.
- **SC-001**: After release, the three KPI-1 searches ("davis", "DAVIS", "Davis") and the KPI-3 measurement are repeated on the sample data and the results are recorded as release evidence, confirming the measured outcome the product owner selected.
- **SC-002**: The Find Owners page (`/owners/find`) and the search endpoint (`/owners?lastName=`) load and function correctly after the change, with no change to page layout, URL structure or form behaviour, as confirmed by the AC-7 regression check.
- **SC-003**: Front-desk staff can find an existing owner by typing their last name in any case without receiving a false "No owners found" response, eliminating the trigger for duplicate owner record creation.

## Assumptions

- The Spring Data JPA SpEL escape helpers (`escape(#lastName)` and `escapeCharacter()`) are available and function correctly in the version of Spring Boot used by this project (Spring Boot 4.1.0). A compile-and-run spike on all three database profiles is the first build task to confirm this before the main query change is written. If the helpers are unavailable, a fallback using a default method that escapes the term manually will be used instead. `@Param` will be added to the method parameter to ensure correct binding regardless of compiler flags.
- The MySQL Testcontainers image in use (mysql:9.7) has a default collation of `utf8mb4_0900_ai_ci`, which is case-insensitive. This means MySQL may already pass the KPI-1 case-insensitivity test before the change. The real KPI-1 proof profile is PostgreSQL. The per-profile baseline run before the change will confirm the actual MySQL behaviour.
- H2 uses `VARCHAR_IGNORECASE` for `owners.last_name` and is already case-insensitive. The new query must not alter H2's existing behaviour. H2 tests serve as regression tests, not as KPI-1 proof.
- The sample data contains exactly 10 owners, with 2 owners named Davis (Betty id 2, Harold id 4). An empty search produces exactly 2 pages of 5. These fixture counts are relied upon by KPI-1, KPI-3 and multiple acceptance criteria.
- `PetClinicConcurrencyTests` uses only `findById` and the pet-add path; it does not call `findByLastNameStartingWith`. It will pass without assertion changes. This assumption must be confirmed by reading the test class before the change is merged.
- The CI pipeline (GitHub Actions, ubuntu-latest runners) has Docker available, so the Testcontainers-based PostgreSQL and MySQL integration tests will execute and not be skipped. If Docker is unavailable in CI, a recorded manual run on PostgreSQL and MySQL is required as release evidence before merge. The CI surefire report must show the new PostgreSQL and MySQL test methods as executed with 0 skipped; a skipped run does not count as proof.
- No schema migration is required by default. A functional index on `lower(last_name)` for PostgreSQL is held as a gated fallback, to be designed by the data track and approved by the product owner only if the KPI-3 gate fires. The H2 and MySQL plain indexes on `last_name` will no longer serve the case-folded search; this index-bypass effect is accepted for those profiles given the sample data size.
- The "same order on all three profiles" acceptance criterion (AC-11) is valid for the ASCII-only sample data. For non-ASCII owner names, sort order may differ between profiles due to collation differences (H2 code-point order, PostgreSQL database locale, MySQL ai_ci accent folding). This limitation is accepted by the product owner for the current scope and is recorded as a known constraint.
- Trailing-space handling (Jira PETC-8) is out of scope. The controller's existing `strip()` call on the main branch is preserved and not modified.
- The change does not require an amendment to the project constitution. It operates within the existing Spring Data JPA repository pattern, adds no new library, makes no schema change by default, and does not alter the HTTP contract or Thymeleaf templates.
- Spec 001 (`001-owners-spring-petclinic`) will require a minor amendment to record that owner search by last name is now case-insensitive and that results are explicitly ordered by last name, then first name, then id. This amendment is part of the deliverables for this change.
- The SQL logging used during baseline and post-change performance measurement will be configured to capture query text without bind values, so that no owner last names (PII) appear in log files or report attachments, in compliance with constitution principle V.
