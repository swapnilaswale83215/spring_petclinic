# Implementation Plan: Case-Insensitive Owner Search

**Branch**: `005-test1-55e78248` | **Date**: 2026-11-17 | **Spec**: `/specs/005-test1-55e78248/spec.md`

**Input**: Feature specification from `/specs/005-test1-55e78248/spec.md`

## Summary

Owner search by last name is currently case-sensitive on PostgreSQL (and possibly MySQL), causing front-desk staff who type lower-case names to receive false "No owners found" responses and create duplicate records. The fix applies `lower()` to both the stored `last_name` column and the bound search term inside a single `@Query` on `OwnerRepository.findByLastNameStartingWith`, making the prefix `LIKE` case-insensitive on all three database profiles (H2, MySQL, PostgreSQL) without relying on collation. An explicit `ORDER BY lower(o.lastName), lower(o.firstName), o.id` is added to guarantee stable, cross-profile ordering. Wildcard characters `%` and `_` in the search term are escaped via the Spring Data JPA SpEL helpers `escape(#lastName)` / `escapeCharacter()` (with a `default`-method fallback if the spike shows the helpers are absent). The method name, parameter types, `Page<Owner>` return type, `OwnerController`, Thymeleaf templates, entity classes, and schema are all unchanged. New case-variant tests are added to `ClinicServiceTests`, `PostgresIntegrationTests`, and `MySqlIntegrationTests`. No dependency changes and no schema migration are required by default; a functional index on `lower(last_name)` is held as a gated fallback pending the KPI-3 measurement.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 4.1.0)

**Primary Dependencies**: Spring MVC, Spring Data JPA 3.x (Hibernate ORM), Thymeleaf, Jakarta Bean Validation, Testcontainers (PostgreSQL, MySQL), JUnit 5, Micrometer

**Storage**: H2 (default/test profile), MySQL (mysql:9.7 via Testcontainers), PostgreSQL (via Testcontainers and dedicated KPI-3 environment). `owners.last_name` is `VARCHAR_IGNORECASE` on H2; plain `VARCHAR` with collation-dependent case sensitivity on MySQL and PostgreSQL.

**Testing**: JUnit 5 via Maven Surefire; `@WebMvcTest` for controller slice tests; `ClinicServiceTests` for H2 service-layer integration; `PostgresIntegrationTests` and `MySqlIntegrationTests` via Testcontainers; `PetClinicConcurrencyTests` for shared-state regression; `I18nPropertiesSyncTest` for i18n completeness.

**Target Platform**: Linux server (GitHub Actions `ubuntu-latest`); Docker required for Testcontainers CI runs.

**Project Type**: Web application — modular monolith, server-side rendering via Thymeleaf, Spring MVC controllers, Spring Data JPA repositories.

**Performance Goals**: Owner search p95 response time on PostgreSQL with the 200-search mix (50 each of "davis", "DAVIS", "Da", empty) must be at most baseline p95 + max(10% of baseline p95, 5 ms) (KPI-3). A supplementary report-only run on ~10,000 owners with EXPLAIN ANALYZE is required as evidence for the no-index decision.

**Constraints**: No new library dependencies; no schema change by default (functional index gated behind KPI-3 approval); no change to `OwnerController`, Thymeleaf templates, `Owner`/`Person` entity classes, or any schema/data script; `lower()` wrapping prevents use of the existing plain btree index on `last_name` for PostgreSQL and MySQL (accepted for sample-data size); non-ASCII name ordering is consistent only for ASCII-only sample data (product owner accepted).

**Scale/Scope**: 10-owner sample data for all acceptance criteria and KPI measurements; up to ~10,000 owners per spec 001 SC-002 (supplementary EXPLAIN run required). Change is confined to `OwnerRepository` (one interface file) plus new test methods in three existing test classes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Gate 1 — Modular Monolith Architecture (Principle I)

**Status**: PASS

The change is confined to `OwnerRepository`, a Spring Data JPA interface in the `owner` package. No new package dependencies are introduced. `OwnerController` and the Thymeleaf templates are unchanged. The application remains a modular monolith with server-side rendering. No REST API refactoring occurs.

### Gate 2 — Domain Integrity and Business Rule Enforcement (Principle II)

**Status**: PASS

No business rules are modified. The `Owner` entity class, its Bean Validation annotations, and the `Person` superclass are unchanged. The change does not affect pet ownership, visit immutability, pet type requirements, or any other domain invariant. No new standalone validator class is introduced.

### Gate 3 — Profile-Aware Persistence (Principle III)

**Status**: PASS (with residual NEEDS CLARIFICATION items tracked)

The `@Query` uses JPQL with `lower()`, which is portable across H2, MySQL, and PostgreSQL without hardcoding a dialect. The change does not assume H2 semantics in production paths. New integration tests are added to `PostgresIntegrationTests` and `MySqlIntegrationTests` and must pass in CI before merge, satisfying the constitution's explicit requirement. Two residual items remain open and are tracked:

- MySQL effective collation (resolved by E2-S2 before E3-S4 is written).
- CI Docker availability for Testcontainers (resolved by E1-S4; recorded manual run is the fallback for the release gate).

### Gate 4 — Service-Layer Caching Contract (Principle IV)

**Status**: PASS

The change touches only `OwnerRepository.findByLastNameStartingWith`. Veterinarian lookups and the `@Cacheable` contract on `VetRepository` are unaffected. No new uncached query path is introduced for data covered by the cache contract.

### Gate 5 — Sensitive Data Handling (Principle V)

**Status**: PASS (with mandatory controls)

The `@Query` returns `Owner` entities only; no new fields are projected or exposed (FR-005). SQL logging during KPI-3 measurement is configured to capture query text and EXPLAIN output without bind values, so no owner last names (PII) appear in log files or report attachments (FR-015). The EXPLAIN output and generated SQL are attached to the release report in redacted form. No new exposure path for owner name, address, or telephone is introduced.

### Gate 6 — Bounded Scope Enforcement (Principle VI)

**Status**: PASS

The change implements case-insensitive owner search. It does not introduce billing, payment, inventory, insurance, or multi-clinic functionality.

### Gate 7 — Internationalisation Completeness (Principle VII)

**Status**: PASS

No new user-facing strings are introduced. No Thymeleaf template is modified. `I18nPropertiesSyncTest` is unaffected.

### Gate 8 — Automated Testing Requirements (Development Workflow)

**Status**: PASS (with mandatory new tests)

The change requires:
- New `@WebMvcTest` cases in `OwnerControllerTests` covering lower-case term pass-through, single-match redirect, empty search, and not-found (E3-S5).
- New methods in `ClinicServiceTests` covering lower-case, upper-case, mixed-case, prefix, empty, no-match, and wildcard-escape scenarios on H2 (E3-S3).
- New methods in `PostgresIntegrationTests` and `MySqlIntegrationTests` covering the same cases on real databases via Testcontainers (E3-S4).
- All existing tests (`OwnerControllerTests`, `OwnerTests`, `ClinicServiceTests`, `PetClinicConcurrencyTests`, `PetClinicIntegrationTests`, `I18nPropertiesSyncTest`) must continue to pass with no assertion or stub changes (FR-011).
- The new PostgreSQL `davis` test must fail on unchanged main (E3-S6), proving defect detection.

### Gate 9 — Observability Standards (Development Workflow)

**Status**: PASS

The owner search is not a bulk query or cache-bypassing lookup of the kind the constitution requires to be instrumented with a new Micrometer timer. The existing Actuator and Micrometer instrumentation is unaffected. No new instrumentation is required for this change.

### Post-Phase-1 Re-check

All gates remain PASS after Phase 1 design. The data model confirms no entity or schema change. The contracts confirm no HTTP contract change. No new violation has been introduced by the design artifacts.

## Project Structure

### Documentation (this feature)

```text
specs/005-test1-55e78248/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── owner-search.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
src/
├── main/
│   └── java/
│       └── org/springframework/samples/petclinic/
│           └── owner/
│               ├── Owner.java                  # UNCHANGED
│               ├── OwnerController.java         # UNCHANGED
│               ├── OwnerRepository.java         # MODIFIED: @Query on findByLastNameStartingWith
│               └── [other owner-package files]  # UNCHANGED
└── test/
    └── java/
        └── org/springframework/samples/petclinic/
            ├── owner/
            │   ├── OwnerControllerTests.java    # MODIFIED: new mocked-repo cases added
            │   └── [other owner test files]     # UNCHANGED
            └── service/
                ├── ClinicServiceTests.java      # MODIFIED: new H2 case-variant methods added
                ├── PostgresIntegrationTests.java # MODIFIED: new case-variant methods added
                └── MySqlIntegrationTests.java   # MODIFIED: new case-variant methods added

src/main/resources/
├── db/
│   ├── h2/                                     # UNCHANGED (schema, data)
│   ├── mysql/                                  # UNCHANGED (schema, data)
│   └── postgres/                               # UNCHANGED (schema, data)
└── templates/
    └── owners/
        ├── findOwners.html                     # UNCHANGED
        └── ownersList.html                     # UNCHANGED

specs/
└── 001-owners-spring-petclinic/
    └── spec.md                                 # AMENDED: record case-insensitive search + explicit order
```

**Structure Decision**: Single-project Maven layout. The only production source change is `OwnerRepository.java` in `src/main/java/.../owner/`. Test changes are additions to three existing test classes in `src/test/java/.../`. No new source files are created in `src/main/`. The spec 001 amendment is a documentation change only.

## Complexity Tracking

No constitution violations requiring justification. All gates pass without exception.

## Phases

### Phase 0: Research

Research is complete. See `research.md`. All NEEDS CLARIFICATION items are tracked with their resolution gates:

| # | Item | Blocking gate | Resolved by |
|---|---|---|---|
| 3 | MySQL effective collation and pre-change `davis` result count | E3-S4, E3-S6 | E2-S2 |
| 7 | Whether `-parameters` compiler flag is set | No (mitigated by `@Param`) | E3-S1 / code inspection |
| 10 | `PetClinicConcurrencyTests` does not call `findByLastNameStartingWith` | E3-S7 merge gate | E3-S7 |
| 11 | Docker available in CI for Testcontainers PostgreSQL and MySQL tests | Release gate E6-S5 | E1-S4 |

No Phase 0 items remain open that block Phase 1 design. Items 3, 10, and 11 are resolved during implementation waves (W1 and W3) before their respective blocking gates.

### Phase 1: Design & Contracts

#### Data Model (`data-model.md`)

The `Owner` entity and its schema are unchanged by this feature. The data model document records the entity as-is, identifies the fields relevant to the search change, and documents the query-level transformation applied.

**Entity: Owner**

| Field | Type (JPA) | DB column | Role in this change |
|---|---|---|---|
| `id` | `Integer` (PK) | `id` | Tie-breaker in `ORDER BY` |
| `lastName` | `String` | `last_name` | Search target; `lower()` applied in query |
| `firstName` | `String` | `first_name` | Secondary sort key; `lower()` applied in `ORDER BY` |
| `address` | `String` | `address` | Sensitive; unchanged, not projected |
| `telephone` | `String` | `telephone` | Sensitive; unchanged, not projected |

**No new fields, no schema change, no migration.**

**Query transformation** (applied entirely within `@Query`; entity class is not modified):

- `WHERE lower(o.lastName) LIKE lower(:lastName) ESCAPE :escapeChar` — case-insensitive prefix match
- `ORDER BY lower(o.lastName), lower(o.firstName), o.id` — stable cross-profile ordering
- Bound parameter `:lastName` receives the escaped, `%`-appended term; `:escapeChar` receives the escape character from `escapeCharacter()`

**State transitions**: None. Owner records are not created, updated, or deleted by the search path.

**Validation rules**: Unchanged. Jakarta Bean Validation annotations on `Owner` and `Person` are not modified.

**Pagination**: `Page<Owner>` return type is preserved. Page size (5) and page boundary logic are unaffected. The count query uses the same `lower()`-based `WHERE` clause without `ORDER BY`.

---

#### Interface Contracts (`contracts/owner-search.md`)

The owner search exposes one HTTP endpoint consumed by the Find Owners form. The contract is unchanged by this feature; the document records the existing contract as the baseline and notes the single behavioural change (case-insensitive matching) and the new ordering guarantee.

**Endpoint**: `GET /owners`

| Parameter | Type | Required | Description |
|---|---|---|---|
| `lastName` | `string` | No | Last-name prefix to search. Empty string returns all owners. `%` and `_` are treated as literals. |
| `page` | `integer` | No | 1-based page number. Out-of-range values redirect to page 1. |

**Responses**:

| Condition | HTTP status | Outcome |
|---|---|---|
| Zero matches | `200 OK` | Renders `findOwners.html` with not-found error message |
| Exactly one match | `302 Found` | Redirects to `/owners/{id}` |
| Two or more matches | `200 OK` | Renders `ownersList.html` with paginated list |

**Ordering** (new guarantee, previously undefined):
Results are ordered by `lower(lastName)` ascending, then `lower(firstName)` ascending, then `id` ascending. This order is identical on H2, MySQL, and PostgreSQL for ASCII-only names.

**Case sensitivity** (changed behaviour):
The `lastName` parameter is now matched case-insensitively. `davis`, `DAVIS`, and `Davis` return the same result set. Previously, only exact-case matches were returned on PostgreSQL (and possibly MySQL).

**Unchanged**:
- URL paths: `/owners/find` (form), `/owners` (search endpoint), `/owners/{id}` (detail)
- Form submission method: `GET`
- Page size: 5
- Thymeleaf templates: `findOwners.html`, `ownersList.html` — no diff
- `OwnerController` — no diff
- `pom.xml` — no dependency changes

**Related endpoint**: `GET /owners/find` — renders the Find Owners form. Unchanged.

---

#### Quickstart Validation Guide (`quickstart.md`)

**Prerequisites**

- Java 21 and Maven wrapper available (`./mvnw`)
- Docker available (for Testcontainers PostgreSQL and MySQL tests)
- A PostgreSQL environment with the 10-owner sample data loaded (for KPI-3 runs; see E1-S1)
- The KPI-3 load script available (see E1-S2)

**Setup**

```bash
# Start the application on the default H2 profile
./mvnw spring-boot:run

# Or start on PostgreSQL profile (requires a running PostgreSQL instance)
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres

# Or start on MySQL profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

**Validation Scenario 1 — KPI-1: Case-insensitive search returns correct owners (AC-1, AC-2, AC-3)**

With the application running and the sample data loaded (Betty Davis id 2, Harold Davis id 4):

```
GET /owners?lastName=davis   → expect: 2 owners (Betty Davis, Harold Davis)
GET /owners?lastName=DAVIS   → expect: 2 owners (Betty Davis, Harold Davis)
GET /owners?lastName=Davis   → expect: 2 owners (Betty Davis, Harold Davis) [regression check]
```

All three responses must render `ownersList.html` with the same two owners in the same order (Betty before Harold, ordered by firstName after lastName tie).

**Validation Scenario 2 — Cross-profile consistency (AC-4, AC-11)**

Repeat Scenario 1 on H2, MySQL, and PostgreSQL profiles. The result set and order must be identical on all three profiles for the ASCII sample data.

**Validation Scenario 3 — Prefix matching (AC-10)**

```
GET /owners?lastName=Da   → expect: all owners whose last name starts with "Da" (includes both Davis owners)
GET /owners?lastName=da   → expect: same result as "Da"
GET /owners?lastName=Daviss → expect: 0 owners, no error
```

**Validation Scenario 4 — Single-match redirect (AC-9)**

Search a lower-case last name that is unique in the sample data. The application must respond with `302 Found` redirecting to `/owners/{id}`, not `200 OK` with a list.

**Validation Scenario 5 — Empty search and paging (AC-8)**

```
GET /owners?lastName=   → expect: page 1 of 2, 5 owners, stable order
GET /owners?lastName=&page=2 → expect: page 2 of 2, 5 owners, no overlap with page 1
```

Order must be identical on all three profiles.

**Validation Scenario 6 — Wildcard escape (SPEC-APP-3)**

```
GET /owners?lastName=%   → expect: 0 owners (% treated as literal, not wildcard)
GET /owners?lastName=_   → expect: 0 owners (_ treated as literal, not wildcard)
```

**Validation Scenario 7 — Automated test suite**

```bash
# Run full suite (H2 profile)
./mvnw test

# Run with PostgreSQL Testcontainers (requires Docker)
./mvnw test -Dspring.profiles.active=postgres

# Run with MySQL Testcontainers (requires Docker)
./mvnw test -Dspring.profiles.active=mysql
```

Expected: 0 failures, 0 skipped Testcontainers tests (if Docker is available). The new PostgreSQL `davis` test must fail on the unchanged `main` branch (E3-S6 evidence).

**Validation Scenario 8 — KPI-3 performance (AC-6)**

```bash
# Run baseline (on unchanged main, PostgreSQL environment from E1-S1)
./kpi3-load-script.sh --profile postgres --mix 200 --output baseline-p95.txt

# Run after change (same environment)
./kpi3-load-script.sh --profile postgres --mix 200 --output after-p95.txt
```

Compare `after-p95.txt` against `baseline-p95.txt + max(10% of baseline, 5 ms)`. Capture EXPLAIN output without bind values and attach to release report.

**References**

- Data model: `specs/005-test1-55e78248/data-model.md`
- HTTP contract: `specs/005-test1-55e78248/contracts/owner-search.md`
- Work plan and wave structure: `specs/005-test1-55e78248/spec.md` (Context from caller section)
- Research decisions: `specs/005-test1-55e78248/research.md`
