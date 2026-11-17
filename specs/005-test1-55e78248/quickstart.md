# Quickstart Validation Guide: Case-Insensitive Owner Search

**Feature**: 005-test1-55e78248
**Branch**: `005-test1-55e78248`
**Date**: 2026-11-17
**Spec**: `specs/005-test1-55e78248/spec.md`

---

## Purpose

This guide describes how to verify the case-insensitive owner search end to end. Each scenario is mapped to the acceptance criteria and success criteria it proves. Run the scenarios in order: baseline measurements (Scenarios 1–2) must be recorded on the unchanged `main` branch before any code change is made.

---

## Prerequisites

- Java 21 and Maven wrapper available (`./mvnw --version` succeeds)
- Docker available and running (`docker info` succeeds) — required for Testcontainers PostgreSQL and MySQL tests
- A dedicated PostgreSQL environment with the 10-owner sample data loaded and reachable, reserved for KPI-3 runs (see E1-S1); this environment must not be modified between the baseline run and the after-change run
- The KPI-3 load script available and producing p95 output (see E1-S2); the script sends `GET /owners?lastName=<term>` 200 times in the mix: 50 × "davis", 50 × "DAVIS", 50 × "Da", 50 × empty
- Sample data fixture counts confirmed: exactly 10 owners, with Betty Davis (id 2) and Harold Davis (id 4) present on all three profiles

---

## Setup

### Start the application on H2 (default profile)

```
./mvnw spring-boot:run
```

Application starts at `http://localhost:8080`.

### Start the application on PostgreSQL

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Requires a running PostgreSQL instance configured for the `postgres` Spring profile.

### Start the application on MySQL

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

Requires a running MySQL instance configured for the `mysql` Spring profile.

---

## ⚠ Baseline Scenarios — Run on Unchanged `main` Before Any Code Change

Scenarios 1 and 2 must be executed and their results recorded before the branch change is applied. The recorded numbers become the reference for KPI-1 and KPI-3 gates.

---

## Scenario 1 — KPI-1 Pre-Change Baseline: Confirm Case-Sensitive Defect on PostgreSQL

**Run on**: unchanged `main` branch, PostgreSQL profile
**Proves**: KPI-1 baseline exists; defect is real on PostgreSQL before the fix
**Acceptance criteria proved**: KPI-1 (baseline half)

### Steps

1. Start the application on the PostgreSQL profile (see Setup).
2. Submit the following requests and record the owner count returned in each response:

```
GET /owners?lastName=Davis   → record count (expected: 2 on all profiles)
GET /owners?lastName=davis   → record count (expected: 0 on PostgreSQL before fix; may be 2 on H2 and possibly MySQL)
GET /owners?lastName=DAVIS   → record count (expected: 0 on PostgreSQL before fix; may be 2 on H2 and possibly MySQL)
```

3. Repeat on H2 and MySQL profiles. Record counts per profile and per term in a table.
4. Record the generated SQL for each search (query text only, no bind values).

### Expected outcome

On PostgreSQL: "davis" and "DAVIS" return 0 owners; "Davis" returns 2. This confirms the defect.
On H2: all three terms likely return 2 (VARCHAR_IGNORECASE). H2 does not prove the defect.
On MySQL: result depends on effective collation (NEEDS CLARIFICATION — resolved by E2-S2). Record the actual count.

### Pass condition

A table of result counts per profile and per term is recorded and attached as baseline evidence before any code change is committed to the branch.

---

## Scenario 2 — KPI-3 Pre-Change Baseline: Measure p95 on PostgreSQL

**Run on**: unchanged `main` branch, dedicated PostgreSQL environment (E1-S1)
**Proves**: KPI-3 baseline p95 exists for comparison after the change
**Acceptance criteria proved**: KPI-3 (baseline half), FR-012, AC-6

### Steps

1. Confirm the PostgreSQL environment has the 10-owner sample data loaded and will not be modified.
2. Run a warm-up pass (discard results):

```
./kpi3-load-script.sh --profile postgres --mix 200
```

3. Run at least 3 baseline measurement passes and record p95 per pass:

```
./kpi3-load-script.sh --profile postgres --mix 200 --output baseline-run-1.txt
./kpi3-load-script.sh --profile postgres --mix 200 --output baseline-run-2.txt
./kpi3-load-script.sh --profile postgres --mix 200 --output baseline-run-3.txt
```

4. Record p95 per search term ("davis", "DAVIS", "Da", empty) and in aggregate for each run. Use the median or worst run as the reference baseline p95.
5. Capture the generated SQL (query text, no bind values) and the EXPLAIN output for one representative search. Confirm no owner last names appear in the captured output.

### Expected outcome

A recorded baseline p95 (in milliseconds), per-term breakdown, generated SQL, and EXPLAIN output are attached as evidence. No owner name values appear in any log or attachment.

### Pass condition

Baseline p95 is recorded. The KPI-3 gate threshold is: after-change p95 ≤ baseline p95 + max(10% of baseline p95, 5 ms).

---

## Post-Change Scenarios — Run After the Code Change Is Applied

Apply the branch change (`005-test1-55e78248`) before running Scenarios 3 onwards.

---

## Scenario 3 — KPI-1: Case-Insensitive Search Returns Correct Owners on All Profiles

**Run on**: changed branch, all three profiles (H2, PostgreSQL, MySQL)
**Proves**: KPI-1 (post-change), SC-001, SC-003
**Acceptance criteria proved**: AC-1, AC-2, AC-3, AC-4

### Steps

1. Start the application on the H2 profile.
2. Submit the following requests and record the owners returned (names and ids):

```
GET /owners?lastName=davis   → expect: Betty Davis (id 2), Harold Davis (id 4)
GET /owners?lastName=DAVIS   → expect: Betty Davis (id 2), Harold Davis (id 4)
GET /owners?lastName=Davis   → expect: Betty Davis (id 2), Harold Davis (id 4)
```

3. Confirm the three result sets are identical (same owners, same order).
4. Repeat steps 2–3 on the PostgreSQL profile.
5. Repeat steps 2–3 on the MySQL profile.
6. Confirm the result sets and order are identical across all three profiles.

### Expected outcome

All three search terms return exactly 2 owners (Betty Davis id 2, Harold Davis id 4) on every profile. The order is Betty before Harold (ordered by firstName after lastName tie). The result sets are identical across H2, PostgreSQL, and MySQL.

### Pass condition

- AC-1: "davis" on H2 returns the same 2 owners as "Davis". ✓
- AC-2: "DAVIS" on H2 returns the same 2 owners as "Davis". ✓
- AC-3: "Davis" returns the same 2 owners as before the change (no regression). ✓
- AC-4: All three terms return the same 2 owners in the same order on H2, MySQL, and PostgreSQL. ✓

---

## Scenario 4 — Stable Cross-Profile Ordering

**Run on**: changed branch, all three profiles
**Proves**: FR-003, User Story 2
**Acceptance criteria proved**: AC-11

### Steps

1. On each profile (H2, PostgreSQL, MySQL), submit:

```
GET /owners?lastName=Da
```

2. Record the full list of owners returned and their order.
3. Confirm Betty Davis appears before Harold Davis in the results.
4. Confirm the full result sequence is identical on all three profiles.

### Expected outcome

All owners whose last name starts with "Da" are returned. Betty Davis appears before Harold Davis. The sequence is identical on H2, PostgreSQL, and MySQL.

### Pass condition

- AC-11: "Da" returns owners ordered by last name then first name, Betty Davis before Harold Davis, identically on all three profiles. ✓

---

## Scenario 5 — Empty Search Returns All Owners with Stable Paging

**Run on**: changed branch, all three profiles
**Proves**: FR-008, User Story 2
**Acceptance criteria proved**: AC-8

### Steps

1. On each profile, submit:

```
GET /owners?lastName=
GET /owners?lastName=&page=2
```

2. Record the owners on page 1 and page 2.
3. Confirm page 1 contains exactly 5 owners and page 2 contains exactly 5 owners.
4. Confirm there is no overlap between page 1 and page 2.
5. Confirm the combined list covers all 10 owners.
6. Confirm the page 1 and page 2 sequences are identical across all three profiles.

### Expected outcome

Page 1: 5 owners. Page 2: 5 owners. No overlap. All 10 owners covered. Order identical on H2, PostgreSQL, and MySQL.

### Pass condition

- AC-8: Empty search returns all owners across 2 pages of 5, with no overlap or gap, in the same order on all three profiles. ✓

---

## Scenario 6 — Prefix Matching Works Case-Insensitively

**Run on**: changed branch, H2 profile (regression check); PostgreSQL and MySQL (cross-profile)
**Proves**: FR-001, User Story 3
**Acceptance criteria proved**: AC-10

### Steps

1. Submit the following requests on each profile:

```
GET /owners?lastName=Da    → expect: all owners whose last name starts with "Da"
GET /owners?lastName=da    → expect: same result as "Da"
GET /owners?lastName=Daviss → expect: 0 owners, no error, renders findOwners.html with not-found message
```

2. Confirm the result sets for "Da" and "da" are identical.
3. Confirm "Daviss" returns 0 owners and the application renders the not-found message without an error.

### Expected outcome

"Da" and "da" return the same owners (including both Davis owners) on all profiles. "Daviss" returns 0 owners with no error.

### Pass condition

- AC-10: "Da" and "da" return every owner whose last name starts with "Da", including both Davis owners. ✓

---

## Scenario 7 — Single-Match Lower-Case Search Redirects to Owner Detail Page

**Run on**: changed branch, H2 profile (and PostgreSQL for cross-profile confirmation)
**Proves**: FR-007, User Story 4
**Acceptance criteria proved**: AC-9

### Steps

1. Identify a last name in the sample data that belongs to exactly one owner (NEEDS CLARIFICATION: the specific unique last name must be confirmed from the sample data fixture; the spec does not name one explicitly).
2. Submit:

```
GET /owners?lastName=<unique-last-name-in-lower-case>
```

3. Confirm the response is `302 Found` with `Location: /owners/{id}` (the id of the matching owner).
4. Confirm the application does not render `ownersList.html`.
5. Submit a lower-case term that matches no owner:

```
GET /owners?lastName=zzznomatch
```

6. Confirm the response is `200 OK` rendering `findOwners.html` with the not-found error message.

### Expected outcome

A unique lower-case match redirects directly to `/owners/{id}`. A no-match lower-case term renders the not-found form. No intermediate list page is shown for the single-match case.

### Pass condition

- AC-9: Lower-case single-match search redirects to `/owners/{id}`, not to the list view. ✓

---

## Scenario 8 — Wildcard Characters Are Treated as Literals

**Run on**: changed branch, H2 profile (and PostgreSQL for cross-profile confirmation)
**Proves**: FR-004, SPEC-APP-3
**Acceptance criteria proved**: SPEC-APP-3 wildcard escape edge case

### Steps

1. Submit the following requests:

```
GET /owners?lastName=%    → expect: 0 owners (% is a literal, not a wildcard)
GET /owners?lastName=_    → expect: 0 owners (_ is a literal, not a wildcard)
```

2. Confirm neither request returns all owners or any owners.
3. Confirm neither request raises an error or exception.
4. Submit a term containing a single quote:

```
GET /owners?lastName=O'Brien
```

5. Confirm the application returns 0 owners (no owner named O'Brien in sample data) and no SQL error is raised.

### Expected outcome

"%" returns 0 owners. "_" returns 0 owners. "O'Brien" returns 0 owners with no error. No SQL injection is possible.

### Pass condition

- SPEC-APP-3: "%" and "_" do not expand the result set; they match only themselves (returning 0 owners in the sample data). SQL fragments do not cause errors. ✓

---

## Scenario 9 — Find Owners Page and URLs Are Unchanged

**Run on**: changed branch, H2 profile
**Proves**: FR-006, User Story 5
**Acceptance criteria proved**: AC-7, SC-002

### Steps

1. Navigate to `http://localhost:8080/owners/find`.
2. Confirm the page loads with `200 OK` and renders `findOwners.html`.
3. Fill in the search form with "Davis" and submit.
4. Confirm the browser navigates to `/owners?lastName=Davis` (not any other URL).
5. Confirm the response renders `ownersList.html` with the 2 Davis owners.
6. Confirm the page layout is visually unchanged from the `main` branch.
7. Confirm no diff exists on `findOwners.html` or `ownersList.html` by inspecting the branch diff.

### Expected outcome

`/owners/find` loads. Form submits to `/owners?lastName=`. Templates are unmodified. Page layout is unchanged.

### Pass condition

- AC-7: The Find Owners page and search endpoint load at the same URLs with no change to page layout or form behaviour. ✓
- SC-002: `/owners/find` and `/owners?lastName=` function correctly after the change. ✓

---

## Scenario 10 — Automated Test Suite: All Existing Tests Pass, New Tests Present and Passing

**Run on**: changed branch, all profiles
**Proves**: FR-009, FR-010, FR-011, KPI-2
**Acceptance criteria proved**: KPI-2

### Steps

1. Run the full test suite on H2:

```
./mvnw test
```

2. Confirm 0 test failures and 0 regressions against the main-branch baseline count recorded in Scenario 1.
3. Confirm at least 3 new case-variant test methods are present in `ClinicServiceTests` (lower-case "davis", upper-case "DAVIS", mixed-case "Davis" returning Betty id 2 and Harold id 4).
4. Run the PostgreSQL Testcontainers tests (requires Docker):

```
./mvnw test -Dspring.profiles.active=postgres
```

5. Confirm the new PostgreSQL case-variant test methods execute and pass. Confirm 0 skipped Testcontainers tests (if Docker is available).
6. Run the MySQL Testcontainers tests (requires Docker):

```
./mvnw test -Dspring.profiles.active=mysql
```

7. Confirm the new MySQL case-variant test methods execute and pass.
8. Confirm `OwnerControllerTests`, `OwnerTests`, `ClinicServiceTests`, `PetClinicConcurrencyTests`, `PetClinicIntegrationTests`, and `I18nPropertiesSyncTest` all pass with no assertion or stub changes.

### Expected outcome

0 existing test failures. At least 3 new case-variant tests present and passing on H2, PostgreSQL, and MySQL. No Testcontainers tests skipped (if Docker is available).

### Pass condition

- KPI-2: Zero existing tests fail. At least 3 new case-variant tests pass on H2, PostgreSQL, and MySQL. ✓
- FR-011: All named existing test classes pass with unmodified assertions. ✓

---

## Scenario 11 — Defect Detection Proof: New PostgreSQL Test Fails on Unchanged `main`

**Run on**: unchanged `main` branch, PostgreSQL profile (Testcontainers)
**Proves**: FR-010
**Acceptance criteria proved**: FR-010 (E3-S6 evidence)

### Steps

1. Check out the unchanged `main` branch.
2. Run the PostgreSQL Testcontainers tests:

```
./mvnw test -Dspring.profiles.active=postgres
```

3. Confirm the new PostgreSQL "davis" case-variant test method fails on `main`.
4. Record the failure output and attach as evidence.
5. Check out the feature branch `005-test1-55e78248`.
6. Run the same test and confirm it passes.

### Expected outcome

The new PostgreSQL "davis" test fails on `main` and passes on the feature branch. This proves the test detects the defect before the fix is applied.

### Pass condition

- FR-010: The new PostgreSQL "davis" test fails on unchanged `main` and passes on the change branch. Evidence attached. ✓

---

## Scenario 12 — KPI-3 After-Change Performance Measurement

**Run on**: changed branch, dedicated PostgreSQL environment (same as Scenario 2)
**Proves**: KPI-3 (post-change), FR-012, AC-6
**Acceptance criteria proved**: AC-6, KPI-3

### Steps

1. Confirm the PostgreSQL environment is the same instance used for the Scenario 2 baseline (E1-S1). It must not have been modified.
2. Run a warm-up pass (discard results):

```
./kpi3-load-script.sh --profile postgres --mix 200
```

3. Run the after-change measurement:

```
./kpi3-load-script.sh --profile postgres --mix 200 --output after-change-p95.txt
```

4. Record p95 per search term ("davis", "DAVIS", "Da", empty) and in aggregate.
5. Compare against the baseline p95 from Scenario 2. Confirm: after-change p95 ≤ baseline p95 + max(10% of baseline p95, 5 ms).
6. Capture the generated SQL (query text, no bind values) and the EXPLAIN output. Confirm no owner last names appear in the captured output.
7. Run the supplementary report-only measurement on a scaled dataset of approximately 10,000 owners and capture EXPLAIN ANALYZE output. Attach as evidence for the no-index decision. (NEEDS CLARIFICATION: the 10,000-owner dataset and the environment for the supplementary run must be prepared separately; the spec does not specify how to generate or load this dataset.)

### Expected outcome

After-change p95 is within the KPI-3 threshold. Generated SQL and EXPLAIN output are captured without bind values. Supplementary EXPLAIN ANALYZE on ~10,000 owners is attached.

### Pass condition

- AC-6: p95 ≤ baseline p95 + max(10% of baseline p95, 5 ms). ✓
- FR-012: EXPLAIN output and generated SQL (without bound owner-name values) attached to release report. ✓
- KPI-3: Gate passes, or the product owner approves the functional-index fallback (E6-S2). ✓

If the gate fails, stop and initiate the functional-index decision (E2-S3, E6-S2) before proceeding to Scenario 13.

---

## Scenario 13 — Rollback Rehearsal

**Run on**: changed branch deployed, then previous build redeployed
**Proves**: FR-013
**Acceptance criteria proved**: FR-013 (E6-S3 evidence)

### Steps

1. With the changed build deployed and running, confirm the application is serving case-insensitive search results (spot-check: `GET /owners?lastName=davis` returns 2 owners).
2. Redeploy the previous build (the last build from `main` before the change).
3. Confirm the application starts without a migration step, schema change, or data repair.
4. Confirm the `owners` table is unchanged (same 10 owners, same data).
5. Confirm the application serves the previous (case-sensitive) behaviour: `GET /owners?lastName=davis` returns 0 owners on PostgreSQL.

### Expected outcome

Rollback is achieved by redeploying the previous build. No data migration or schema change is required. The application starts cleanly.

### Pass condition

- FR-013: Rollback achieved by redeploying the previous build with no data repair step. ✓

---

## Scenario 14 — Post-Release Measured Outcome

**Run on**: released build, production or production-equivalent environment
**Proves**: SC-001, KPI-1 (post-release), KPI-3 (post-release)
**Acceptance criteria proved**: SC-001

### Steps

1. After the release redeploy (E6-S5), submit the three KPI-1 searches on the PostgreSQL profile:

```
GET /owners?lastName=davis   → record count and owner ids
GET /owners?lastName=DAVIS   → record count and owner ids
GET /owners?lastName=Davis   → record count and owner ids
```

2. Confirm all three return exactly 2 owners (Betty Davis id 2, Harold Davis id 4) and the result sets are identical.
3. Run the KPI-3 load script on the same PostgreSQL environment used for Scenarios 2 and 12:

```
./kpi3-load-script.sh --profile postgres --mix 200 --output post-release-p95.txt
```

4. Confirm post-release p95 ≤ baseline p95 + max(10% of baseline p95, 5 ms).
5. Record both results as release evidence.

### Expected outcome

"davis", "DAVIS", and "Davis" each return the same 2 owners post-release. Post-release p95 is within the KPI-3 threshold. Both results are recorded as release evidence.

### Pass condition

- SC-001: KPI-1 searches and KPI-3 measurement repeated post-release and results recorded. ✓

---

## Success Criteria Coverage Summary

| Scenario | Success criteria / KPIs proved |
|---|---|
| 1 | KPI-1 baseline (pre-change defect confirmed on PostgreSQL) |
| 2 | KPI-3 baseline p95 recorded; FR-012 |
| 3 | KPI-1 post-change; AC-1, AC-2, AC-3, AC-4; SC-001, SC-003 |
| 4 | FR-003; AC-11 |
| 5 | FR-008; AC-8 |
| 6 | FR-001; AC-10 |
| 7 | FR-007; AC-9 |
| 8 | FR-004; SPEC-APP-3 wildcard escape |
| 9 | FR-006; AC-7; SC-002 |
| 10 | FR-009, FR-011; KPI-2 |
| 11 | FR-010 (defect detection proof) |
| 12 | KPI-3 post-change; FR-012; AC-6 |
| 13 | FR-013 (rollback) |
| 14 | SC-001; KPI-1 and KPI-3 post-release evidence |

---

## NEEDS CLARIFICATION

- **Scenario 7**: The specific unique last name in the sample data that belongs to exactly one owner is not named in the spec. The sample data fixture must be read to identify a suitable term before this scenario can be executed.
- **Scenario 12 (supplementary run)**: The method for generating or loading a ~10,000-owner dataset for the supplementary EXPLAIN ANALYZE run is not specified in the spec or plan. The environment and dataset preparation for this run must be agreed before the KPI-3 gate (E6-S2).
- **Scenario 11 / Scenario 10 (Docker in CI)**: Whether Docker is available on GitHub Actions `ubuntu-latest` runners is unconfirmed (resolved by E1-S4). If Docker is unavailable, a recorded manual run on PostgreSQL and MySQL is required as release evidence at gate E6-S5. A skipped Testcontainers run does not satisfy FR-009 or the constitution Section III requirement.
- **Scenario 3 / MySQL baseline**: The effective MySQL collation (and therefore whether MySQL already passes the KPI-1 test before the fix) is unconfirmed until E2-S2 reads the Testcontainers image configuration and any `conf.d` overrides.

---

## References

- Data model: `specs/005-test1-55e78248/data-model.md`
- HTTP contract: `specs/005-test1-55e78248/contracts/owner-search.md`
- Research decisions: `specs/005-test1-55e78248/research.md`
- Feature specification: `specs/005-test1-55e78248/spec.md`
- Work plan and wave structure: `specs/005-test1-55e78248/spec.md` (Context from caller section)
- KPI-3 environment setup: E1-S1, E1-S2 (work plan items)
- Spec 001 amendment: `specs/001-owners-spring-petclinic/spec.md` (to be updated as a deliverable of this feature)
