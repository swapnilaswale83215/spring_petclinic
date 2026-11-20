# Contract: Owner Search by Last Name

**Feature**: 005-test1-55e78248 — Case-Insensitive Owner Search
**Date**: 2026-11-17
**Status**: Baseline + behavioural change
**Contract type**: HTTP endpoint (server-side rendered, Spring MVC)

---

## Overview

This document records the full contract for the owner search endpoint and the Find Owners form. It states which behaviours are unchanged (must-not-break), which behaviour changes with this feature (case-insensitive matching), and which new guarantee is introduced (explicit result ordering).

All callers — the Thymeleaf form, browser navigation, and automated contract tests (E5-S1) — depend on this contract. Any deviation from the unchanged items is a regression.

---

## Endpoints

### `GET /owners/find`

Renders the Find Owners search form.

**Must-not-break (unchanged)**:

| Property | Value |
|---|---|
| URL path | `/owners/find` |
| HTTP method | `GET` |
| Response status | `200 OK` |
| Template rendered | `findOwners.html` (Thymeleaf; no diff permitted) |
| Form action | Submits to `GET /owners` with query parameter `lastName` |
| Page layout | Unchanged; no new HTML elements, no removed elements |

---

### `GET /owners`

Executes the owner search and returns results.

**Query parameters**:

| Parameter | Type | Required | Constraints |
|---|---|---|---|
| `lastName` | `string` | No | Any string. Empty string or absent returns all owners. `%` and `_` are treated as literals (escaped before the LIKE query). Single quotes and other SQL fragments are safe (bound parameter; no SQL injection possible). Trailing spaces are stripped by the controller (existing behaviour, unchanged). |
| `page` | `integer` | No | 1-based. Out-of-range values (less than 1 or greater than total pages) redirect to page 1. Absent defaults to page 1. |

**Response matrix (must-not-break)**:

| Condition | HTTP status | Body / action |
|---|---|---|
| Zero owners match | `200 OK` | Renders `findOwners.html` with the not-found error message. No redirect. |
| Exactly one owner matches | `302 Found` | Redirects to `GET /owners/{id}` where `{id}` is the matched owner's internal identifier. No list page rendered. |
| Two or more owners match | `200 OK` | Renders `ownersList.html` with a paginated list. |
| `page` out of range | `302 Found` | Redirects to `GET /owners?lastName=<term>&page=1`. |

**Page size (must-not-break)**:

- Fixed at **5 owners per page**.
- The 10-owner sample data produces exactly **2 pages**.
- Page 1 and page 2 have no overlap and no gap.

---

## Behavioural Changes Introduced by This Feature

### Changed: Case sensitivity of `lastName` matching

| Property | Before this feature | After this feature |
|---|---|---|
| Case sensitivity | Case-sensitive on PostgreSQL; case-insensitive on H2 (VARCHAR_IGNORECASE); unconfirmed on MySQL (see research item 3) | Case-insensitive on all three profiles (H2, MySQL, PostgreSQL) |
| Matching mechanism | Database default (collation-dependent) | `lower()` applied to both the stored column and the bound search term in the `@Query`; result does not depend on database collation |
| Example: sample data, 2 Davis owners | `davis` → 0 results on PostgreSQL (before fix); `Davis` → 2 results | `davis`, `DAVIS`, `Davis` → identical 2-owner result set on all profiles |

The `lastName` parameter continues to be a **prefix match** (owners whose last name *starts with* the supplied term). This behaviour is unchanged.

### New guarantee: Explicit result ordering

| Property | Before this feature | After this feature |
|---|---|---|
| Result order | Undefined; database-dependent; not guaranteed to be stable across profiles | `lower(lastName)` ascending, then `lower(firstName)` ascending, then `id` ascending |
| Cross-profile consistency | Not guaranteed | Guaranteed for ASCII-only names on H2, MySQL, and PostgreSQL |
| Tie-breaker | None | `id` ascending (ensures deterministic order when two owners share the same last name and first name) |
| Non-ASCII names | N/A (no prior guarantee) | Order may differ across profiles due to collation differences; accepted by product owner for current scope |

---

## Unchanged Items (Must-Not-Break)

The following are explicitly confirmed unchanged by this feature. Any diff in these items is a defect.

**URL structure**:

| Path | Purpose | Status |
|---|---|---|
| `/owners/find` | Find Owners form | Unchanged |
| `/owners` | Search endpoint | Unchanged |
| `/owners/{id}` | Owner detail page | Unchanged |

**Controller**:

- `OwnerController` — **no diff permitted**. The controller's `processFindForm` method, its `@GetMapping` annotations, its `Pageable` construction, its single-match redirect logic, its not-found error path, and its out-of-range page redirect are all unchanged.

**Templates**:

- `findOwners.html` — **no diff permitted**.
- `ownersList.html` — **no diff permitted**.

**Entity and schema**:

- `Owner` entity class — unchanged. No new fields projected or exposed.
- `Person` superclass — unchanged.
- Database schema (`owners` table, all columns) — unchanged. No migration.
- Data scripts (`data.sql` on all profiles) — unchanged.

**`pom.xml`**:

- No dependency additions, removals, or version changes.

**Returned data**:

- The search returns `Owner` entities only. No new fields are projected. Owner `name`, `address`, and `telephone` fields are not exposed in any new code path (constitution principle V).

**Pagination contract**:

- `Page<Owner>` return type — unchanged.
- Page size — 5, unchanged.
- Total element count and page boundary logic — unaffected by the case-insensitive change. The count query uses the same `lower()`-based `WHERE` clause.

**Single-match redirect**:

- A search term (in any case) that matches exactly one owner produces `302 Found` → `/owners/{id}`. This applies to lower-case terms as well as correctly-cased terms (FR-007, AC-9).

**Not-found behaviour**:

- A search term that matches no owner produces `200 OK` rendering `findOwners.html` with the existing not-found error message. No change to the message text or the template.

**Wildcard handling**:

- `%` and `_` in the search term are escaped and treated as literals. A search for `%` returns 0 owners, not all owners (FR-004, SPEC-APP-3, AC edge cases).

**SQL injection safety**:

- The search term is passed to the database as a bound parameter. No string concatenation into the query. No SQL injection is possible and no error is raised for terms containing single quotes or other SQL fragments (FR-004).

---

## `OwnerRepository` Internal Contract

This section documents the repository method contract that all callers (controller, tests, stubs) depend on. It is an internal contract; the HTTP contract above is the external one.

**Method signature (must-not-break)**:

```java
Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable);
```

| Property | Value |
|---|---|
| Method name | `findByLastNameStartingWith` — unchanged |
| Parameter 1 | `String lastName` — the raw search term as received from the controller (after the controller's existing `strip()` call); `@Param("lastName")` annotation added (no signature change) |
| Parameter 2 | `Pageable pageable` — unchanged |
| Return type | `Page<Owner>` — unchanged |
| Declared on | `OwnerRepository` (Spring Data JPA interface) — the only file modified in `src/main/` |

**Query behaviour (changed)**:

| Property | Before | After |
|---|---|---|
| `WHERE` clause | Derived by Spring Data JPA from method name: `last_name LIKE :lastName%` (case-sensitive on PostgreSQL) | `@Query`: `WHERE lower(o.lastName) LIKE lower(:lastName) ESCAPE :escapeChar` — case-insensitive prefix match on all profiles |
| Wildcard escaping | None | `%` and `_` in `:lastName` are escaped using `escape(#lastName)` / `escapeCharacter()` SpEL helpers (fallback: `default` method with manual escaping if spike E3-S1 shows helpers unavailable) |
| `ORDER BY` | None (database default) | `ORDER BY lower(o.lastName), lower(o.firstName), o.id` |
| Count query | Auto-derived | Explicit `countQuery` attribute: same `lower()`-based `WHERE` clause, no `ORDER BY` |
| Bound parameters | Derived | Named parameters only (`:lastName`, `:escapeChar`); no string concatenation |

**Stub contract for `OwnerControllerTests`**:

- Existing Mockito stubs for `findByLastNameStartingWith` are **not modified**. The method name, parameter types, and return type are unchanged, so all existing stubs remain valid.
- New test cases add stubs that supply a lower-case `lastName` argument; the stub behaviour (returning a configured `Page<Owner>`) is identical to existing stubs.

---

## Acceptance Criteria Traceability

| Acceptance criterion | Contract item verified |
|---|---|
| AC-1: `davis` returns 2 owners on H2 | Case-insensitive matching; `Page<Owner>` count |
| AC-2: `DAVIS` returns 2 owners on H2 | Case-insensitive matching |
| AC-3: `Davis` returns 2 owners (regression) | Must-not-break: existing exact-case result unchanged |
| AC-4: All three cases return same 2 owners on MySQL and PostgreSQL | Cross-profile case-insensitive matching |
| AC-6 / KPI-3: p95 within tolerance | Not a contract item; see `quickstart.md` scenario 8 |
| AC-7: `/owners/find` and `/owners?lastName=` unchanged | Must-not-break: URLs, templates, controller |
| AC-8: Empty search → 2 pages of 5 | Must-not-break: page size, pagination |
| AC-9: Lower-case single match → redirect to `/owners/{id}` | Must-not-break: single-match redirect |
| AC-10: `da` and `Da` return same prefix-matched owners | Case-insensitive prefix matching |
| AC-11: `Da` search → Betty Davis before Harold Davis, identical order on all profiles | New ordering guarantee |
| SPEC-APP-3: `%` and `_` treated as literals | Wildcard escaping |

---

## Out-of-Scope Items

The following are explicitly not covered by this contract and must not be introduced:

- Any new HTTP endpoint or URL path.
- Any change to the `Owner` entity fields or the `owners` table schema.
- Any new query parameter on `GET /owners` or `GET /owners/find`.
- Any change to the Thymeleaf template HTML structure.
- Any exposure of owner `name`, `address`, or `telephone` in a new code path.
- Trailing-space normalisation beyond the controller's existing `strip()` call (Jira PETC-8, out of scope).
- Non-ASCII collation normalisation across profiles (accepted limitation; product owner sign-off recorded in research item 12).
- A functional index on `lower(last_name)` unless the KPI-3 gate (E6-S2) fires and the product owner approves it as a separate schema-change decision.
