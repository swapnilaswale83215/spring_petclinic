---

description: "Task list template for feature implementation"
---

# Tasks: Case-Insensitive Owner Search

**Input**: Design documents from `/specs/005-test1-55e78248/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/owner-search.md

**Tests**: Test tasks are included because FR-009, FR-010, and the spec explicitly require new automated tests covering lower-case, upper-case, and mixed-case searches on H2, PostgreSQL, and MySQL.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. Because the entire production code change is a single `@Query` on one method, the foundational phase carries the implementation; user-story phases carry the tests and verification work that prove each story's acceptance criteria.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Single-project Maven layout. All source paths are relative to the repository root.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm the measurement environment, CI assumptions, and SpEL-helper availability before any code is written. Every subsequent phase depends on the facts gathered here.

- [ ] T001 Confirm the Spring Data JPA version in `pom.xml` supports `escape(#lastName)` and `escapeCharacter()` in `@Query` by reading the Spring Data JPA artifact version declared or transitively pulled in `pom.xml`; record the version and whether the helpers are available; if unavailable, record that the fallback default-method approach (manual `%`/`_` escaping, same public signature) will be used in T010 — document finding in `specs/005-test1-55e78248/research.md` (resolves research item 7 / E3-S1)
- [ ] T002 [P] Confirm Docker is available on the GitHub Actions `ubuntu-latest` CI runners by reading `.github/workflows/` CI workflow files; record whether `PostgresIntegrationTests` and `MySqlIntegrationTests` are executed (not skipped) in CI; if Docker is unavailable, record that a manual PostgreSQL and MySQL run is required as release evidence at gate E6-S5 — document finding in `specs/005-test1-55e78248/research.md` (resolves research item 11 / E1-S4)
- [ ] T003 [P] Read `src/test/java/org/springframework/samples/petclinic/service/PetClinicConcurrencyTests.java` in full and confirm it does not call `findByLastNameStartingWith` and does not assert on owner search result order; record the finding — document in `specs/005-test1-55e78248/research.md` (resolves research item 10 / E3-S7 pre-check)
- [ ] T004 [P] Confirm the MySQL Testcontainers image tag and effective collation by reading `src/test/java/org/springframework/samples/petclinic/service/MySqlIntegrationTests.java` and any Testcontainers configuration; record the image tag (expected `mysql:9.7`) and whether `conf.d` overrides are present; record whether MySQL is already case-insensitive before the fix — document finding in `specs/005-test1-55e78248/research.md` (resolves research item 3 / E2-S2)
- [ ] T005 [P] Record the pre-change main-branch test suite pass/fail count by reading the most recent CI surefire report or test output available; record the total test count and any pre-existing failures as the KPI-2 baseline — document in `specs/005-test1-55e78248/research.md` (E2-S1 partial)

**Checkpoint**: Setup complete — SpEL-helper availability confirmed, CI Docker status known, `PetClinicConcurrencyTests` scope confirmed, MySQL collation known, baseline test count recorded. Foundational phase may now begin.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement the single production code change (`OwnerRepository`) that all user-story verification phases depend on. No user-story test phase can be written or run until this phase is complete.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T006 Add `@Query` to `findByLastNameStartingWith` in `src/main/java/org/springframework/samples/petclinic/owner/OwnerRepository.java`: use `WHERE lower(o.lastName) LIKE lower(:lastName) ESCAPE :escapeChar` with `escape(#lastName)` and `escapeCharacter()` SpEL helpers (or the fallback default-method approach if T001 found the helpers unavailable); add `ORDER BY lower(o.lastName) ASC, lower(o.firstName) ASC, o.id ASC`; add an explicit `countQuery` using the same `lower()`-based `WHERE` clause without `ORDER BY`; add `@Param("lastName")` to the `lastName` parameter; keep the method name `findByLastNameStartingWith`, parameter types `(String lastName, Pageable pageable)`, and return type `Page<Owner>` exactly unchanged; make no change to `OwnerController`, `Owner`, `Person`, any Thymeleaf template, any schema script, or any data script (implements FR-001, FR-002, FR-003, FR-004, FR-006, FR-007, FR-008, FR-013, FR-014; resolves E3-S2)

**Checkpoint**: Foundation complete — `OwnerRepository` carries the new `@Query`. All user-story phases may now proceed. Verify the project compiles cleanly with `./mvnw compile` before continuing.

---

## Phase 3: User Story 1 — Front-Desk Staff Find Owners by Lower-Case Last Name (Priority: P1) 🎯 MVP

**Goal**: Prove that `davis`, `DAVIS`, and `Davis` each return the same 2 owners (Betty id 2, Harold id 4) on H2, PostgreSQL, and MySQL, eliminating the false "No owners found" defect on PostgreSQL.

**Independent Test**: Submit `GET /owners?lastName=davis` against the sample data on H2 (via `ClinicServiceTests`) and on PostgreSQL (via `PostgresIntegrationTests`) and confirm exactly 2 owners are returned — the same 2 returned by `GET /owners?lastName=Davis`.

### Tests for User Story 1

- [ ] T007 [P] [US1] Add test method `testFindOwnersByLastNameLowerCase` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("davis", pageable)` and assert the result contains exactly 2 owners with ids 2 and 4 (Betty Davis and Harold Davis) — covers AC-1, FR-001, KPI-1 on H2 (E3-S3)
- [ ] T008 [P] [US1] Add test method `testFindOwnersByLastNameUpperCase` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("DAVIS", pageable)` and assert the result contains exactly 2 owners with ids 2 and 4 — covers AC-2, FR-001, KPI-2 on H2 (E3-S3)
- [ ] T009 [P] [US1] Add test method `testFindOwnersByLastNameMixedCase` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("Davis", pageable)` and assert the result contains exactly 2 owners with ids 2 and 4, confirming no regression from the existing exact-case behaviour — covers AC-3, FR-001, KPI-2 on H2 (E3-S3)
- [ ] T010 [US1] Add test method `testFindOwnersByLastNameLowerCasePostgres` to `src/test/java/org/springframework/samples/petclinic/service/PostgresIntegrationTests.java`: call `findByLastNameStartingWith("davis", pageable)` via the real PostgreSQL Testcontainers database and assert exactly 2 owners are returned with ids 2 and 4; this test MUST fail on the unchanged `main` branch (E3-S6 defect-detection proof) and pass on this branch — covers AC-1, AC-4, FR-001, KPI-1 on PostgreSQL (E3-S4)
- [ ] T011 [P] [US1] Add test methods `testFindOwnersByLastNameUpperCasePostgres` and `testFindOwnersByLastNameMixedCasePostgres` to `src/test/java/org/springframework/samples/petclinic/service/PostgresIntegrationTests.java`: assert `DAVIS` and `Davis` each return the same 2 owners as `davis` on PostgreSQL — covers AC-2, AC-3, AC-4, FR-001, KPI-2 on PostgreSQL (E3-S4)
- [ ] T012 [P] [US1] Add test methods `testFindOwnersByLastNameLowerCaseMysql`, `testFindOwnersByLastNameUpperCaseMysql`, and `testFindOwnersByLastNameMixedCaseMysql` to `src/test/java/org/springframework/samples/petclinic/service/MySqlIntegrationTests.java`: assert `davis`, `DAVIS`, and `Davis` each return exactly 2 owners with ids 2 and 4 on the MySQL Testcontainers database — covers AC-4, FR-001, KPI-1 on MySQL (E3-S4)

**Checkpoint**: User Story 1 complete. `davis`, `DAVIS`, and `Davis` return the same 2 owners on H2, PostgreSQL, and MySQL. KPI-1 is satisfied. The PostgreSQL `davis` test fails on `main` (E3-S6 evidence attached).

---

## Phase 4: User Story 2 — Search Results Are Consistently Ordered Across All Database Profiles (Priority: P2)

**Goal**: Prove that search results are ordered by `lower(lastName)` ASC, `lower(firstName)` ASC, `id` ASC on H2, MySQL, and PostgreSQL, and that the order is identical across all three profiles for the ASCII sample data.

**Independent Test**: Submit `GET /owners?lastName=Da` on each of the three database profiles and confirm Betty Davis (id 2) always appears before Harold Davis (id 4), and that the full result sequence is identical across profiles.

### Tests for User Story 2

- [ ] T013 [P] [US2] Add test method `testFindOwnersByPrefixOrderedH2` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("Da", pageable)` and assert the result list is ordered with Betty Davis (id 2) before Harold Davis (id 4) — covers AC-11, FR-003, US2 on H2 (E3-S3)
- [ ] T014 [P] [US2] Add test method `testFindOwnersByPrefixOrderedPostgres` to `src/test/java/org/springframework/samples/petclinic/service/PostgresIntegrationTests.java`: call `findByLastNameStartingWith("Da", pageable)` on the real PostgreSQL Testcontainers database and assert Betty Davis (id 2) appears before Harold Davis (id 4) in the result list — covers AC-11, FR-003, US2 on PostgreSQL (E3-S4)
- [ ] T015 [P] [US2] Add test method `testFindOwnersByPrefixOrderedMysql` to `src/test/java/org/springframework/samples/petclinic/service/MySqlIntegrationTests.java`: call `findByLastNameStartingWith("Da", pageable)` on the MySQL Testcontainers database and assert Betty Davis (id 2) appears before Harold Davis (id 4) — covers AC-11, FR-003, US2 on MySQL (E3-S4)
- [ ] T016 [P] [US2] Add test method `testEmptySearchPaginationH2` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("", PageRequest.of(0, 5))` and assert `getTotalElements()` equals 10 and `getTotalPages()` equals 2; call again with `PageRequest.of(1, 5)` and assert no overlap with page 1 — covers AC-8, FR-008, US2 on H2 (E3-S3)
- [ ] T017 [P] [US2] Add test method `testEmptySearchPaginationPostgres` to `src/test/java/org/springframework/samples/petclinic/service/PostgresIntegrationTests.java`: same assertions as T016 on the PostgreSQL Testcontainers database — covers AC-8, FR-008, US2 on PostgreSQL (E3-S4)
- [ ] T018 [P] [US2] Add test method `testEmptySearchPaginationMysql` to `src/test/java/org/springframework/samples/petclinic/service/MySqlIntegrationTests.java`: same assertions as T016 on the MySQL Testcontainers database — covers AC-8, FR-008, US2 on MySQL (E3-S4)

**Checkpoint**: User Story 2 complete. Explicit ordering is verified on all three profiles. Empty-search pagination produces 2 pages of 5 with no overlap on all profiles.

---

## Phase 5: User Story 3 — Prefix Matching Continues to Work Case-Insensitively (Priority: P2)

**Goal**: Prove that partial lower-case terms (e.g., `da`) return the same owners as the correctly-cased term (`Da`), that a non-matching prefix returns zero results without error, and that `%` and `_` in the search term are treated as literals.

**Independent Test**: Submit `GET /owners?lastName=da` and `GET /owners?lastName=Da` against the sample data and confirm both return every owner whose last name starts with "Da", including both Davis owners, with no false negatives.

### Tests for User Story 3

- [ ] T019 [P] [US3] Add test method `testFindOwnersByLowerCasePrefixH2` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("da", pageable)` and assert the result set is identical to `findByLastNameStartingWith("Da", pageable)` — covers AC-10, FR-001, US3 on H2 (E3-S3)
- [ ] T020 [P] [US3] Add test method `testFindOwnersByLowerCasePrefixPostgres` to `src/test/java/org/springframework/samples/petclinic/service/PostgresIntegrationTests.java`: same assertion as T019 on the PostgreSQL Testcontainers database — covers AC-10, FR-001, US3 on PostgreSQL (E3-S4)
- [ ] T021 [P] [US3] Add test method `testFindOwnersByLowerCasePrefixMysql` to `src/test/java/org/springframework/samples/petclinic/service/MySqlIntegrationTests.java`: same assertion as T019 on the MySQL Testcontainers database — covers AC-10, FR-001, US3 on MySQL (E3-S4)
- [ ] T022 [P] [US3] Add test method `testFindOwnersByNonMatchingPrefixH2` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("Daviss", pageable)` and assert `getTotalElements()` equals 0 and no exception is thrown — covers US3 AC-3, FR-001 on H2 (E3-S3)
- [ ] T023 [P] [US3] Add test method `testWildcardPercentEscapedH2` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("%", pageable)` and assert `getTotalElements()` equals 0 (% treated as literal, not wildcard) — covers SPEC-APP-3, FR-004, US3 edge case on H2 (E3-S3, E4-S2)
- [ ] T024 [P] [US3] Add test method `testWildcardUnderscoreEscapedH2` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("_", pageable)` and assert `getTotalElements()` equals 0 (_ treated as literal) — covers SPEC-APP-3, FR-004, US3 edge case on H2 (E3-S3, E4-S2)
- [ ] T025 [P] [US3] Add test methods `testWildcardPercentEscapedPostgres` and `testWildcardUnderscoreEscapedPostgres` to `src/test/java/org/springframework/samples/petclinic/service/PostgresIntegrationTests.java`: assert `%` and `_` each return 0 owners on PostgreSQL — covers SPEC-APP-3, FR-004, US3 on PostgreSQL (E3-S4, E4-S2)
- [ ] T026 [P] [US3] Add test methods `testWildcardPercentEscapedMysql` and `testWildcardUnderscoreEscapedMysql` to `src/test/java/org/springframework/samples/petclinic/service/MySqlIntegrationTests.java`: assert `%` and `_` each return 0 owners on MySQL — covers SPEC-APP-3, FR-004, US3 on MySQL (E3-S4, E4-S2)
- [ ] T027 [P] [US3] Add test method `testSqlFragmentTermSafeH2` to `src/test/java/org/springframework/samples/petclinic/service/ClinicServiceTests.java`: call `findByLastNameStartingWith("'", pageable)` (single-quote term) and assert 0 owners are returned and no exception is thrown, confirming bound-parameter safety — covers SPEC-APP-3, FR-004, US3 edge case on H2 (E4-S2)

**Checkpoint**: User Story 3 complete. Prefix matching is case-insensitive on all profiles. Wildcard characters are escaped. SQL-fragment terms are safe.

---

## Phase 6: User Story 4 — Single-Match Lower-Case Search Redirects Directly to Owner Page (Priority: P2)

**Goal**: Prove that a lower-case search term matching exactly one owner causes `OwnerController.processFindForm` to redirect to `/owners/{id}`, not render the list view, mirroring the existing behaviour for correctly-cased single-match searches.

**Independent Test**: Add a mocked-repository `@WebMvcTest` case that supplies a lower-case `lastName` stub returning a single-element `Page<Owner>` and asserts the response is `302 Found` redirecting to `/owners/{id}`.

### Tests for User Story 4

- [ ] T028 [US4] Add test method `testProcessFindFormLowerCaseSingleMatchRedirects` to `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java`: configure the Mockito stub for `findByLastNameStartingWith` to return a `Page<Owner>` containing exactly one owner when called with a lower-case `lastName` argument; perform `mockMvc.perform(get("/owners").param("lastName", "davis"))` and assert the response status is `302` and the redirect URL is `/owners/2`; confirm the stub uses the existing method signature unchanged — covers AC-9, FR-007, US4 (E3-S5)
- [ ] T029 [P] [US4] Add test method `testProcessFindFormLowerCaseNoMatchShowsError` to `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java`: configure the stub to return an empty `Page<Owner>` for a lower-case term; assert the response status is `200` and the model contains the not-found error attribute, confirming the not-found path is unchanged — covers US4 AC-2, FR-006 (E3-S5)
- [ ] T030 [P] [US4] Add test method `testProcessFindFormLowerCaseTermPassedUnchangedToRepository` to `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java`: use a Mockito `ArgumentCaptor` to capture the `lastName` argument passed to `findByLastNameStartingWith` when the controller receives `lastName=davis`; assert the captured value equals `"davis"` (lower-case, not up-cased by the controller), confirming `OwnerController` is unchanged and passes the term as-is — covers FR-006, FR-014 (E3-S5)

**Checkpoint**: User Story 4 complete. Lower-case single-match redirect is verified at the controller slice level. `OwnerController` diff is confirmed empty.

---

## Phase 7: User Story 5 — Find Owners Page and URLs Are Unchanged (Priority: P3)

**Goal**: Prove that `/owners/find` and `/owners?lastName=` load at the same URLs, that the Thymeleaf templates are unmodified, and that the form submits to the same endpoint.

**Independent Test**: Load `/owners/find` and submit the form; confirm the request goes to `/owners?lastName=` with no change to the rendered HTML structure or URL paths.

### Tests for User Story 5

- [ ] T031 [P] [US5] Add test method `testFindOwnersFormLoadsAtCorrectUrl` to `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java`: perform `mockMvc.perform(get("/owners/find"))` and assert status `200` and that the view name is `owners/findOwners` — covers AC-7, FR-006, US5 (E3-S5, E5-S1)
- [ ] T032 [P] [US5] Add test method `testFindOwnersFormSubmitsToCorrectEndpoint` to `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java`: configure the stub to return a multi-owner page; perform `mockMvc.perform(get("/owners").param("lastName", "Davis"))` and assert status `200` and view name `owners/ownersList` — covers AC-7, FR-006, US5 (E3-S5, E5-S1)
- [ ] T033 [P] [US5] Add test method `testEmptySearchReturnsAllOwnersWithPaging` to `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java`: configure the stub to return a `Page<Owner>` with `totalElements=10`, `totalPages=2`, and 5 owners for page 1; perform `mockMvc.perform(get("/owners").param("lastName", ""))` and assert status `200`, view `owners/ownersList`, and that the model's `currentPage` is 1 — covers AC-8, FR-008, US5 (E3-S5, E5-S1)
- [ ] T034 [P] [US5] Add test method `testOutOfRangePageRedirectsToPageOne` to `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java`: perform `mockMvc.perform(get("/owners").param("lastName", "Davis").param("page", "99"))` and assert the response is a redirect to a URL containing `page=1` — covers FR-006, US5 edge case (E5-S1)
- [ ] T035 [US5] Verify that `src/main/resources/templates/owners/findOwners.html` has no diff from the `main` branch by reading the file and confirming no lines were added, removed, or modified — covers FR-006, FR-011, FR-014, US5 AC-2 (E3-S7)
- [ ] T036 [P] [US5] Verify that `src/main/resources/templates/owners/ownersList.html` has no diff from the `main` branch by reading the file and confirming no lines were added, removed, or modified — covers FR-006, FR-011, FR-014, US5 AC-2 (E3-S7)

**Checkpoint**: User Story 5 complete. URLs, templates, and form behaviour are confirmed unchanged.

---

## Phase 8: User Story 6 — Performance of Owner Search Is Not Degraded (Priority: P3)

**Goal**: Confirm that the KPI-3 measurement plan is in place, that SQL logging is configured to redact bound values, and that the EXPLAIN output and generated SQL are captured and attached to the release report without PII.

**Independent Test**: Run the 200-search mix (50 each of `davis`, `DAVIS`, `Da`, empty) on the PostgreSQL profile before and after the change using the KPI-3 load script; confirm p95 after ≤ baseline p95 + max(10% of baseline, 5 ms).

### Tasks for User Story 6

- [ ] T037 [US6] Verify that `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` has no diff from the `main` branch by reading the file in full and confirming no lines were added, removed, or modified — covers FR-006, FR-011, FR-014, US6 (E3-S7 merge gate)
- [ ] T038 [P] [US6] Verify that `pom.xml` has no dependency additions, removals, or version changes from the `main` branch by reading the `<dependencies>` and `<dependencyManagement>` sections — covers FR-011, FR-013, US6 (E3-S7 merge gate)
- [ ] T039 [P] [US6] Verify that `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` and `src/main/java/org/springframework/samples/petclinic/owner/Person.java` (or wherever `Person` is declared) have no diff from the `main` branch — covers FR-005, FR-011, FR-014, US6 (E3-S7 merge gate)
- [ ] T040 [P] [US6] Verify that all schema and data scripts (`src/main/resources/db/h2/schema.sql`, `src/main/resources/db/h2/data.sql`, `src/main/resources/db/mysql/schema.sql`, `src/main/resources/db/mysql/data.sql`, `src/main/resources/db/postgres/schema.sql`, `src/main/resources/db/postgres/data.sql`) have no diff from the `main` branch — covers FR-013, FR-014, US6 (E3-S7 merge gate)
- [ ] T041 [US6] Confirm that the KPI-3 load script (E1-S2) is configured to log query text without bind values (e.g., Hibernate `show_sql=true`, `format_sql=true`, with no P6Spy or datasource-proxy bind-value logging enabled) so that no owner last names appear in log output; document the logging configuration used in `specs/005-test1-55e78248/research.md` — covers FR-012, FR-015, constitution V, US6 AC-2 (E6-S1)

NEEDS CLARIFICATION: The KPI-3 load script (E1-S2) and the PostgreSQL measurement environment (E1-S1) are human-track deliverables that cannot be created by an LLM. Tasks T037–T041 cover the verifiable code-side obligations for US6. The actual baseline run (E1-S3), after-change run (E6-S1), KPI-3 gate decision (E6-S2), rollback rehearsal (E6-S3), and post-release measurement (E6-S6) are human-executed steps that must be tracked outside this task list.

**Checkpoint**: User Story 6 code-side obligations complete. All no-diff verifications passed. SQL logging is confirmed PII-safe. KPI-3 measurement is ready to execute (human track).

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Full-suite regression confirmation, security review evidence, spec 001 amendment, and release documentation.

- [ ] T042 Run the full Maven test suite (`./mvnw test`) on the H2 profile and confirm zero existing test failures; compare the total passing test count against the baseline recorded in T005; confirm `ClinicServiceTests`, `OwnerControllerTests`, `OwnerTests`, and `PetClinicConcurrencyTests` all pass with no assertion or stub changes — covers FR-011, KPI-2 (E3-S7)
- [ ] T043 [P] Run the Maven test suite with the PostgreSQL Testcontainers profile (`./mvnw test -Dspring.profiles.active=postgres`) and confirm zero failures and zero skipped Testcontainers tests (if Docker is available per T002); attach the surefire report showing the new PostgreSQL test methods as executed — covers FR-009, FR-010, KPI-2 (E3-S6, E3-S7)
- [ ] T044 [P] Run the Maven test suite with the MySQL Testcontainers profile (`./mvnw test -Dspring.profiles.active=mysql`) and confirm zero failures and zero skipped Testcontainers tests (if Docker is available per T002); attach the surefire report — covers FR-009, KPI-2 (E3-S7)
- [ ] T045 [P] Produce the security review record for E4-S1: read the final `OwnerRepository.java` diff and confirm (a) only bound parameters are used (no string concatenation), (b) `%` and `_` are escaped, (c) no new field or projection is added beyond the existing `Owner` entity, (d) no change to access control on `/owners`; record the written confirmation in `specs/005-test1-55e78248/research.md` — covers FR-004, FR-005, SPEC-APP-3, constitution V (E4-S1)
- [ ] T046 [P] Amend `specs/001-owners-spring-petclinic/spec.md` to record that owner search by last name is now case-insensitive (lower-case, upper-case, and mixed-case terms return the same result set) and that results are explicitly ordered by last name ascending, then first name ascending, then id ascending; note the accepted limitation that non-ASCII name ordering may differ across profiles — covers research item 13, spec 001 amendment deliverable
- [ ] T047 [P] Write release notes in `specs/005-test1-55e78248/release-notes.md`: state "no schema or data change; no write-freeze required; rollback = redeploy previous build with no data repair step"; list the evidence items (KPI-1 result, KPI-2 pass count, KPI-3 p95, security review, contract regression, cross-profile test results) — covers FR-013, SPEC-APP-8 (E6-S4)
- [ ] T048 Execute the quickstart validation scenarios from `specs/005-test1-55e78248/quickstart.md` manually or via the test suite: Scenario 1 (KPI-1 case variants), Scenario 3 (prefix matching), Scenario 4 (single-match redirect), Scenario 5 (empty search paging), Scenario 6 (wildcard escape); record pass/fail for each scenario — covers SC-001, SC-002, SC-003

**Checkpoint**: All phases complete. Zero existing test failures. New tests cover lower-case, upper-case, and mixed-case on H2, PostgreSQL, and MySQL. Security review recorded. Spec 001 amended. Release notes written. Ready for release gate E6-S5.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately. T002, T003, T004, T005 are all parallel.
- **Foundational (Phase 2)**: Depends on T001 (SpEL-helper availability confirmed) — BLOCKS all user story phases.
- **User Story Phases (3–8)**: All depend on T006 (Foundational) completion.
  - Phases 3–8 can proceed in parallel once T006 is done (different test files, no shared state).
  - Within each phase, all `[P]`-marked tasks can run in parallel.
- **Polish (Phase 9)**: T042–T044 depend on all test tasks in Phases 3–8 being written. T045 depends on T006. T046, T047 can start any time after T006. T048 depends on T042–T044.

### User Story Dependencies

- **US1 (Phase 3)**: Depends only on T006. No dependency on other user stories.
- **US2 (Phase 4)**: Depends only on T006. No dependency on US1 (ordering is part of the same `@Query`).
- **US3 (Phase 5)**: Depends only on T006. No dependency on US1 or US2.
- **US4 (Phase 6)**: Depends only on T006. Controller slice tests are independent of service-layer tests.
- **US5 (Phase 7)**: Depends only on T006. Template and URL verification is independent.
- **US6 (Phase 8)**: Depends on T006 for no-diff verifications. KPI-3 measurement is a human-track dependency (E1-S1, E1-S2, E1-S3).

### Within Each User Story

- All `[P]`-marked tasks within a phase touch different test methods or different test classes and can run in parallel.
- Non-`[P]` tasks within a phase have a logical dependency on T006 but not on each other.

### Critical Path

T001 → T006 → T010 (PostgreSQL `davis` defect-detection test, must fail on `main`) → T043 (PostgreSQL CI run) → T042 (full-suite regression) → T048 (quickstart validation) → release gate E6-S5.

---

## Parallel Example: User Story 1

```
# All three H2 case-variant tests can be written in parallel (same file, different methods — coordinate to avoid merge conflicts):
T007: testFindOwnersByLastNameLowerCase      → ClinicServiceTests.java
T008: testFindOwnersByLastNameUpperCase      → ClinicServiceTests.java
T009: testFindOwnersByLastNameMixedCase      → ClinicServiceTests.java

# PostgreSQL and MySQL tests can be written in parallel (different files):
T010 + T011: PostgresIntegrationTests.java
T012:        MySqlIntegrationTests.java
```

## Parallel Example: User Story 3

```
# H2 wildcard escape tests (same file — coordinate):
T023: testWildcardPercentEscapedH2          → ClinicServiceTests.java
T024: testWildcardUnderscoreEscapedH2       → ClinicServiceTests.java

# Cross-profile wildcard tests (different files — fully parallel):
T025: PostgresIntegrationTests.java
T026: MySqlIntegrationTests.java
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T005) — confirm assumptions
2. Complete Phase 2: Foundational (T006) — the single production code change
3. Complete Phase 3: User Story 1 (T007–T012) — prove KPI-1 on all three profiles
4. **STOP and VALIDATE**: Run `./mvnw test` (H2) and the Testcontainers suites; confirm T010 fails on `main` and passes on this branch
5. Deploy/demo if ready — the core defect is fixed

### Incremental Delivery

1. Setup + Foundational → `OwnerRepository` carries the new `@Query`
2. US1 (Phase 3) → KPI-1 satisfied, defect fixed → **MVP**
3. US2 (Phase 4) → explicit ordering verified across profiles
4. US3 (Phase 5) → prefix matching and wildcard escaping verified
5. US4 (Phase 6) → single-match redirect verified at controller level
6. US5 (Phase 7) → URL and template no-diff confirmed
7. US6 (Phase 8) → performance obligations confirmed, PII-safe logging verified
8. Polish (Phase 9) → full regression, security review, spec 001 amendment, release notes

### Parallel Team Strategy

With multiple developers, once T006 is complete:

- Developer A: US1 (Phase 3) — H2 and PostgreSQL case-variant tests
- Developer B: US2 + US3 (Phases 4–5) — ordering and prefix tests
- Developer C: US4 + US5 (Phases 6–7) — controller slice and no-diff verification
- Developer D: US6 + Polish (Phases 8–9) — performance obligations and release docs

---

## Notes

- `[P]` tasks touch different files or different methods in the same file; coordinate within-file parallel work to avoid merge conflicts on `ClinicServiceTests.java`, `PostgresIntegrationTests.java`, and `MySqlIntegrationTests.java`.
- The `[Story]` label maps each task to its acceptance criteria in `spec.md` for traceability.
- T006 is the only change to `src/main/`. All other tasks are test additions, verification steps, or documentation.
- NEEDS CLARIFICATION items (MySQL collation T004, CI Docker T002, `PetClinicConcurrencyTests` scope T003, SpEL helpers T001) are resolved in Phase 1 before any code is written.
- The KPI-3 load script, PostgreSQL measurement environment, baseline run, after-change run, KPI-3 gate decision, rollback rehearsal, and post-release measurement are human-track deliverables (E1-S1, E1-S2, E1-S3, E6-S1, E6-S2, E6-S3, E6-S6) and are not represented as LLM-executable tasks.
- Commit after T006 (the production change) and after each phase's test tasks are passing.
- Stop at the Phase 3 checkpoint to confirm T010 fails on `main` before proceeding — this is the E3-S6 defect-detection evidence required for the release gate.
