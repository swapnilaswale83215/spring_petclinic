# Data Model: Case-Insensitive Owner Search

**Feature**: 005-test1-55e78248
**Date**: 2026-11-17
**Status**: Complete

---

## Overview

This feature makes no changes to any entity class, database schema, data script, or migration. The `Owner` entity and its underlying `owners` table are unchanged. All case-folding and ordering logic is applied entirely within a JPQL `@Query` annotation on `OwnerRepository`. This document records the entity as-is, identifies the fields the query touches, and documents the query-level transformations applied.

---

## Entities

### Owner

The pet's registered guardian. The sole entity read by the search path. No fields are added, removed, or modified.

**Inheritance**: `Owner` extends `Person`. The fields `firstName` and `lastName` are declared on `Person` and inherited by `Owner`.

| Field | Java type | DB column | DB type (H2) | DB type (MySQL / PostgreSQL) | Nullable | Role in this change |
|---|---|---|---|---|---|---|
| `id` | `Integer` | `id` | `INTEGER` (PK, auto) | `INT` / `SERIAL` (PK) | No | Tie-breaker in `ORDER BY`; not modified |
| `lastName` | `String` | `last_name` | `VARCHAR_IGNORECASE(80)` | `VARCHAR(80)` | No | Search target; `lower()` applied in `WHERE` and `ORDER BY` |
| `firstName` | `String` | `first_name` | `VARCHAR_IGNORECASE(30)` | `VARCHAR(30)` | No | Secondary sort key; `lower()` applied in `ORDER BY` |
| `address` | `String` | `address` | `VARCHAR(255)` | `VARCHAR(255)` | No | Sensitive; returned as part of `Owner` entity; not projected separately; not modified |
| `telephone` | `String` | `telephone` | `VARCHAR(20)` | `VARCHAR(20)` | No | Sensitive; returned as part of `Owner` entity; not projected separately; not modified |

**Sensitive fields**: `firstName`, `lastName`, `address`, `telephone` are designated sensitive zones under constitution principle V. No new exposure path is introduced. The query returns full `Owner` entities to the existing controller path; no new projection, DTO, or serialisation is added.

**Validation rules**: Unchanged. Jakarta Bean Validation annotations on `Person` and `Owner` are not modified by this feature. The existing constraints (non-blank name, valid telephone, non-blank address) remain in force.

**State transitions**: None. The search path is read-only. Owner records are not created, updated, or deleted by `findByLastNameStartingWith`.

---

### Pet, Visit, Veterinarian, PetType, Specialty

These entities are not touched by this feature. They are listed here for completeness and to confirm scope.

| Entity | Touched | Reason |
|---|---|---|
| `Pet` | No | Not involved in owner search |
| `Visit` | No | Not involved in owner search |
| `Veterinarian` | No | Not involved in owner search |
| `PetType` | No | Not involved in owner search |
| `Specialty` | No | Not involved in owner search |

---

## Relationships

No relationship changes. The `Owner` → `Pet` one-to-many association is unchanged. The search query operates on the `owners` table only; no join to `pets`, `visits`, or any other table is introduced.

---

## Schema Changes

**None.** No DDL is added, removed, or modified on any profile.

| Profile | Schema file | Status |
|---|---|---|
| H2 | `src/main/resources/db/h2/schema.sql` | UNCHANGED |
| MySQL | `src/main/resources/db/mysql/schema.sql` | UNCHANGED |
| PostgreSQL | `src/main/resources/db/postgres/schema.sql` | UNCHANGED |

The existing plain btree index on `last_name` (if present in the schema scripts) is not used by the new query on PostgreSQL or MySQL because `lower()` wrapping prevents index use. This is accepted for the 10-owner sample data. A functional index on `lower(last_name)` for PostgreSQL is held as a gated fallback, to be designed by the data track (E2-S3) and approved by the product owner only if the KPI-3 gate (E6-S2) fires.

---

## Data Script Changes

**None.** The sample data scripts are unchanged on all profiles.

| Profile | Data file | Status |
|---|---|---|
| H2 | `src/main/resources/db/h2/data.sql` | UNCHANGED |
| MySQL | `src/main/resources/db/mysql/data.sql` | UNCHANGED |
| PostgreSQL | `src/main/resources/db/postgres/data.sql` | UNCHANGED |

The sample data contains exactly 10 owners, including Betty Davis (id 2) and Harold Davis (id 4). These fixture counts are relied upon by KPI-1, KPI-3, and multiple acceptance criteria.

---

## Repository Change: `OwnerRepository.findByLastNameStartingWith`

The only production code change in this feature. The method signature, parameter types, and return type are fixed interfaces that all callers and test stubs depend on and are not modified.

### Method signature (unchanged)

```
Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable)
```

### Query transformation applied within `@Query`

The following transformations are applied entirely inside the JPQL annotation. The `Owner` entity class and its mapped fields are not modified.

**WHERE clause** (case-insensitive prefix match):

```
WHERE lower(o.lastName) LIKE lower(:lastName) ESCAPE :escapeChar
```

- `:lastName` receives the search term with `%` and `_` escaped via `escape(#lastName)` (SpEL helper) and a trailing `%` appended for prefix matching.
- `:escapeChar` receives the escape character from `escapeCharacter()` (SpEL helper).
- If the SpEL helpers are unavailable (confirmed by E3-S1 spike), a `default` method on `OwnerRepository` escapes `%` and `_` manually and delegates to a renamed internal `@Query` method. The public method signature is preserved in either case.
- The bound parameter approach ensures no SQL injection is possible (FR-004, SPEC-APP-3).

**ORDER BY clause** (stable cross-profile ordering):

```
ORDER BY lower(o.lastName) ASC, lower(o.firstName) ASC, o.id ASC
```

- `lower(o.lastName)` — primary sort key; case-insensitive on PostgreSQL (which would otherwise sort by code point).
- `lower(o.firstName)` — secondary sort key; case-insensitive.
- `o.id` — tie-breaker; ensures deterministic, stable pagination when two owners share the same last name and first name.
- This order is identical on H2, MySQL, and PostgreSQL for the ASCII-only sample data. Non-ASCII name ordering may differ across profiles due to collation differences; this limitation is accepted by the product owner (research item 12).

**Count query** (explicit, required for correct pagination):

```
SELECT count(o) FROM Owner o WHERE lower(o.lastName) LIKE lower(:lastName) ESCAPE :escapeChar
```

- Uses the same `lower()`-based `WHERE` clause as the main query.
- No `ORDER BY`; count queries do not require ordering.
- Provided explicitly to avoid framework-version-specific derivation failures with complex `@Query` expressions (research item 6).

**`@Param` annotation**:

- `@Param("lastName")` is added to the `lastName` parameter to ensure correct named-parameter binding regardless of whether the `-parameters` compiler flag is set (research item 7).

### Pagination

`Page<Owner>` return type is preserved. Page size (5) and page boundary logic in `OwnerController.processFindForm` are unaffected. The count query uses the same `WHERE` clause, so total element counts and page boundaries are correct on all profiles.

---

## What Changes vs. the Current Model

| Dimension | Before | After |
|---|---|---|
| `Owner` entity class | Unchanged | Unchanged |
| `Person` entity class | Unchanged | Unchanged |
| `owners` table schema | Unchanged | Unchanged |
| Data scripts | Unchanged | Unchanged |
| `OwnerRepository` interface | `findByLastNameStartingWith` uses derived query (case-sensitive on PostgreSQL and MySQL) | `findByLastNameStartingWith` uses explicit `@Query` with `lower()` on both sides, escaped prefix `LIKE`, explicit `ORDER BY`, and explicit `countQuery` |
| Search case sensitivity | Case-sensitive on PostgreSQL; case-sensitive on MySQL (collation-dependent — NEEDS CLARIFICATION, resolved by E2-S2); case-insensitive on H2 (VARCHAR_IGNORECASE) | Case-insensitive on all three profiles via `lower()` wrapping; result does not depend on database collation |
| Result ordering | Undefined (no explicit `ORDER BY`; order varies by profile and query plan) | Explicitly ordered by `lower(lastName)` ASC, `lower(firstName)` ASC, `id` ASC; identical on all three profiles for ASCII sample data |
| Wildcard handling | `%` and `_` in the search term expand the result set (LIKE wildcard behaviour) | `%` and `_` are escaped and treated as literals; they do not expand the result set |
| `OwnerController` | Unchanged | Unchanged |
| Thymeleaf templates | Unchanged | Unchanged |
| `pom.xml` dependencies | Unchanged | Unchanged |

---

## NEEDS CLARIFICATION Items Affecting the Data Model

| # | Item | Impact on data model | Resolved by |
|---|---|---|---|
| 3 | MySQL effective collation and pre-change `davis` result count | Determines whether MySQL already passes KPI-1 before the fix; does not change the data model design | E2-S2 (before E3-S4) |
| 7 | Whether `-parameters` compiler flag is set | Determines whether `@Param` is strictly required or merely defensive; does not change the data model design | E3-S1 / code inspection (mitigated by adding `@Param`) |
| 10 | `PetClinicConcurrencyTests` does not call `findByLastNameStartingWith` | If the test does assert on search result order, the new `ORDER BY` could change the sequence; does not change the data model design | E3-S7 (merge gate) |
| 11 | Docker available in CI for Testcontainers PostgreSQL and MySQL tests | Determines whether the new tests run in CI or require a recorded manual run; does not change the data model design | E1-S4 (release gate E6-S5) |

None of the open NEEDS CLARIFICATION items require a change to the entity model, schema, or data scripts. They affect implementation confidence and test evidence only.

---

## Gated Fallback: Functional Index on `lower(last_name)`

Not part of the default data model. Held as a conditional schema change, to be designed by the data track (E2-S3) and approved by the product owner only if the KPI-3 gate (E6-S2) fires.

If approved, the index would be:

| Profile | Index definition | Notes |
|---|---|---|
| PostgreSQL | `CREATE INDEX idx_owners_lower_last_name ON owners (lower(last_name))` | Functional index; allows the planner to use an index scan for `lower(last_name) LIKE lower(:lastName)` |
| MySQL | `ALTER TABLE owners ADD INDEX idx_owners_lower_last_name ((lower(last_name)))` | Functional index syntax available in MySQL 8.0+; confirm with E2-S2 |
| H2 | No change needed | `VARCHAR_IGNORECASE` already supports case-insensitive index use |

If this fallback is activated, it constitutes a schema migration and requires a versioned migration script, a rollback script, and product owner sign-off. It does not affect the `Owner` entity class or any data script.
