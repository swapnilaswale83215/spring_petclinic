# Research: Case-Insensitive Owner Search
**Feature**: 005-test1-55e78248
**Date**: 2026-11-17
**Status**: Complete (with residual NEEDS CLARIFICATION items noted)

---

## 1. Spring Data JPA SpEL Escape Helpers

**Decision**: Treat `escape(#lastName)` and `escapeCharacter()` as available, but verify with a compile-and-run spike (E3-S1) before writing the main query.

**Rationale**: The spec explicitly assumes these helpers are present in Spring Boot 4.1.0 (Assumptions section). Spring Data JPA has shipped `escape()` and `escapeCharacter()` as `@Query` SpEL extensions since Spring Data JPA 2.x; Spring Boot 4.1.0 ships Spring Data JPA 3.x, so the helpers are expected to be present. However, the spec itself mandates a spike to confirm, and the plan item E3-S1 is the formal gate. The helpers allow the `@Query` to remain a single annotated string with no extra method, keeping the implementation minimal.

**Alternatives considered**:
- **Fallback: default method with manual escaping** — If the spike shows the helpers are absent or broken on any profile, a `default` method on `OwnerRepository` escapes `%` and `_` by prepending a chosen escape character (e.g., `\`), then delegates to a renamed `@Query` method. The public method signature (`findByLastNameStartingWith(String, Pageable)`) is preserved. This is the spec-mandated fallback.
- **Escaping in the controller** — Rejected. FR-014 prohibits changes to `OwnerController`, and escaping belongs at the query boundary, not the HTTP layer.
- **Database-side ESCAPE clause without SpEL** — Possible but verbose; the fallback default-method approach is cleaner and equally safe.

---

## 2. Case-Folding Strategy: `lower()` on Both Sides vs. Collation

**Decision**: Apply `lower()` to both the stored column and the bound search term in the JPQL/HQL `@Query`. Do not rely on database collation.

**Rationale**: FR-002 mandates this approach explicitly. It is the only strategy that produces identical results on H2 (VARCHAR_IGNORECASE, already case-insensitive but must not regress), MySQL (collation unconfirmed at research time — see item 3 below), and PostgreSQL (case-sensitive by default). Using `lower()` on both sides makes the query collation-agnostic and satisfies the cross-profile consistency requirement in FR-001 and AC-4.

**Alternatives considered**:
- **Rely on MySQL `utf8mb4_0900_ai_ci` collation** — The spec notes this collation is assumed but unconfirmed (E2-S2). Even if confirmed, relying on collation would mean the query behaves differently on PostgreSQL, violating FR-002. Rejected.
- **PostgreSQL `ILIKE`** — Case-insensitive on PostgreSQL but not portable to H2 or MySQL. Rejected on portability grounds.
- **`UPPER()` instead of `LOWER()`** — Functionally equivalent; `lower()` is the conventional choice and matches the spec's wording throughout. No material difference.
- **Functional index on `lower(last_name)` (PostgreSQL)** — Held as a gated fallback for KPI-3 only (FR-013, E2-S3). Not part of the default implementation. Requires product owner approval if the KPI-3 gate fires.

---

## 3. MySQL Effective Collation and Pre-Change Behaviour

**Decision**: NEEDS CLARIFICATION — the effective MySQL collation must be confirmed by E2-S2 before the MySQL Testcontainers test (E3-S4) is written or trusted.

**Rationale**: The spec assumes the MySQL Testcontainers image (`mysql:9.7`) defaults to `utf8mb4_0900_ai_ci`, which is case-insensitive, meaning MySQL may already pass the KPI-1 test before the fix. However, the spec explicitly flags this as unconfirmed and states the real KPI-1 proof profile is PostgreSQL. The effective collation can be overridden by `conf.d` settings in the Testcontainers image or by the `data.sql` / `schema.sql` DDL. Until E2-S2 reads the actual collation, the MySQL baseline result for `davis` is unknown.

**Alternatives considered**:
- **Assume `utf8mb4_0900_ai_ci` and proceed** — Rejected. If the assumption is wrong, the MySQL test could give a false pass on the unchanged branch, undermining E3-S6's defect-detection proof.
- **Skip MySQL baseline and rely on PostgreSQL only** — Rejected. FR-009 and the constitution (Section III) require MySQL integration tests to pass. AC-4 requires identical results on all three profiles.

---

## 4. H2 Behaviour with `lower()` Applied to a `VARCHAR_IGNORECASE` Column

**Decision**: The `lower()` wrapping is safe on H2 and will not alter its existing case-insensitive behaviour or break index use for the sample data size.

**Rationale**: H2's `VARCHAR_IGNORECASE` type stores values in a normalised form and its `LIKE` operator is already case-insensitive. Applying `lower()` to a `VARCHAR_IGNORECASE` column is a no-op in terms of result correctness. H2 does not use a btree index for a function-wrapped column in the same way PostgreSQL does, but at 10 owners the performance impact is zero. The spec accepts this (Assumptions section) and notes H2 tests serve as regression tests, not KPI-1 proof.

**Alternatives considered**:
- **Omit `lower()` on the H2 path via a profile-specific query** — Rejected. FR-014 confines the change to `OwnerRepository` and the spec requires a single `@Query` that works on all three profiles. Profile-branching in the repository would violate the modular monolith principle (constitution I) and add maintenance burden.
- **Replace `VARCHAR_IGNORECASE` with `VARCHAR` in H2 schema** — Out of scope. FR-013 and FR-014 prohibit schema changes unless the KPI-3 gate fires.

---

## 5. Explicit Result Ordering

**Decision**: Add `ORDER BY lower(o.lastName), lower(o.firstName), o.id` to the `@Query`. Apply the same ordering to the count query's `WHERE` clause (the count query does not need an `ORDER BY`).

**Rationale**: FR-003 mandates ordering by last name (case-insensitive), then first name (case-insensitive), then internal id as a tie-breaker. Without an explicit order, Spring Data JPA / Hibernate does not guarantee result sequence, and the sequence can differ between H2, MySQL, and PostgreSQL (AC-11, User Story 2). The `lower()` wrapping in the `ORDER BY` ensures the sort is case-insensitive on PostgreSQL, which would otherwise sort by code point. The id tie-breaker satisfies the edge case in User Story 2 (two owners with identical names) and ensures stable pagination.

**Alternatives considered**:
- **Pass a `Sort` via `Pageable`** — The caller (`OwnerController.processFindForm`) constructs the `Pageable` and the spec prohibits changes to `OwnerController` (FR-014). Injecting a sort from the controller is therefore not available without violating FR-014.
- **`ORDER BY o.lastName, o.firstName, o.id` without `lower()`** — Would sort case-sensitively on PostgreSQL (uppercase before lowercase in ASCII order), violating AC-11 for mixed-case data. Rejected.
- **Rely on database default order** — Explicitly rejected by FR-003 and User Story 2.

---

## 6. `@Query` Count Query Requirement

**Decision**: Provide an explicit `countQuery` attribute on the `@Query` annotation, using the same `lower()`-based `WHERE` clause but without the `ORDER BY`.

**Rationale**: Spring Data JPA derives a count query automatically from the main query when pagination is used, but the derivation can fail or produce incorrect SQL when the main query contains complex expressions such as `lower()` wrapping, `ESCAPE` clauses, or SpEL helpers. Providing an explicit `countQuery` ensures the total element count (used for page boundary calculation) is correct on all three profiles and avoids framework-version-specific derivation bugs. FR-008 requires the page count to be unaffected by the change.

**Alternatives considered**:
- **Rely on automatic count query derivation** — Risky with complex `@Query` expressions. The spec's assumption about SpEL helpers already flags framework-version sensitivity. Rejected in favour of explicit `countQuery`.
- **Named query in `orm.xml`** — Unnecessary complexity. The `@Query` annotation is the established pattern in this codebase (constitution, Technology Stack section).

---

## 7. `@Param` Annotation on the Method Parameter

**Decision**: Add `@Param("lastName")` to the `lastName` parameter of `findByLastNameStartingWith`.

**Rationale**: The spec's Assumptions section states `@Param` will be added to ensure correct binding regardless of compiler flags. Without `-parameters` compiler flag (not guaranteed in all build environments), Spring Data JPA cannot reliably infer parameter names from bytecode for `@Query` methods that use named parameters (`:lastName`). Adding `@Param` is a zero-risk, zero-signature-change annotation that eliminates this binding ambiguity on all profiles and CI environments.

**Alternatives considered**:
- **Positional parameters (`?1`)** — Works without `@Param` but is less readable and more fragile if the parameter list changes. The spec uses named parameters throughout. Rejected.
- **Rely on `-parameters` flag** — The Maven/Gradle build configuration was not read at research time; whether `-parameters` is set is NEEDS CLARIFICATION. Adding `@Param` removes the dependency on this flag entirely.

---

## 8. Wildcard Escaping Approach

**Decision**: Use the SpEL `escape(#lastName)` helper (with `escapeCharacter()` as the escape character) to escape `%` and `_` in the bound term before it is appended with `%` for the prefix `LIKE`. If the spike (E3-S1) shows the helpers are unavailable, use a `default` method that replaces `%` with `\%` and `_` with `\_` and appends `%`, then passes the result to a renamed internal `@Query` method.

**Rationale**: FR-004 requires `%` and `_` to be escaped so they match only themselves. SPEC-APP-3 and the edge cases section confirm this. The SpEL helpers are the idiomatic Spring Data JPA approach and keep the `@Query` self-contained. The fallback preserves the public method signature (FR-001, FR-011).

**Alternatives considered**:
- **No escaping** — A search for `%` would return all owners, a security and correctness defect. Rejected.
- **Escaping in `OwnerController`** — FR-014 prohibits changes to `OwnerController`. Rejected.
- **Database-side `REPLACE()` in the query** — Verbose and non-portable. Rejected.

---

## 9. Index Strategy and KPI-3 Performance

**Decision**: No index change by default. Accept that `lower()` wrapping prevents use of the existing plain btree index on `last_name` for PostgreSQL and MySQL. Gate a functional index on `lower(last_name)` (PostgreSQL) behind the KPI-3 approval gate (E6-S2).

**Rationale**: FR-013 prohibits schema changes unless the KPI-3 gate fires. The sample data has only 10 owners; a sequential scan at that size is faster than an index lookup. The spec acknowledges the index-bypass effect and accepts it for the sample data (Assumptions section). The KPI-3 tolerance is generous (baseline p95 + max(10%, 5 ms)). A supplementary run on ~10,000 owners is required as report-only evidence (KPI-3 note) so the approver can make an informed no-index decision.

**Alternatives considered**:
- **Add functional index immediately** — Requires a schema migration, violates FR-013 unless the gate fires, and adds rollback complexity. Rejected as default path.
- **Use a covering index** — Out of scope at this stage; would require product owner approval and a data-track design (E2-S3).
- **Switch to a full-text search index** — Entirely out of scope for a prefix `LIKE` change. Rejected.

---

## 10. `PetClinicConcurrencyTests` Dependency on Search

**Decision**: NEEDS CLARIFICATION — the test class must be read (E3-S7) to confirm it does not call `findByLastNameStartingWith` or assert on search result order before the change is merged.

**Rationale**: The spec's Assumptions section states `PetClinicConcurrencyTests` uses only `findById` and the pet-add path, but explicitly flags this as an assumption to be confirmed by reading the test class. If the test does call the search method or assert on result order, the new `ORDER BY` clause could change the result sequence and cause an assertion failure, threatening FR-011 (zero existing test failures).

**Alternatives considered**:
- **Assume the test is safe and proceed** — Rejected. The spec itself mandates confirmation. A false assumption here would cause a CI failure at the worst possible time (wave 3).

---

## 11. CI Docker Availability for Testcontainers

**Decision**: NEEDS CLARIFICATION — CI Docker availability must be confirmed by E1-S4 before the Testcontainers-based PostgreSQL and MySQL tests are relied upon as release evidence.

**Rationale**: FR-009 requires the new case-variant tests to run against real PostgreSQL and MySQL repositories in CI without being skipped. The constitution (Section III) requires MySQL and PostgreSQL integration tests to pass in CI before any persistence-layer change is merged. If Docker is unavailable on the GitHub Actions `ubuntu-latest` runners (e.g., due to runner configuration or resource limits), the tests will be skipped and cannot serve as release evidence. The spec mandates a recorded manual run as the fallback.

**Alternatives considered**:
- **Use the H2 profile as the sole CI gate** — Rejected. H2 is already case-insensitive and cannot detect the defect. The constitution explicitly requires MySQL and PostgreSQL integration tests in CI for persistence-layer changes.
- **Use a hosted PostgreSQL/MySQL service in CI (GitHub Actions service containers)** — A viable alternative to Testcontainers if Docker-in-Docker is unavailable, but requires changes to the CI workflow. This is a decision for E1-S4 to resolve; it is not within the scope of the repository change itself.

---

## 12. Non-ASCII and Accent-Folded Name Ordering

**Decision**: Accept that `ORDER BY lower(o.lastName), lower(o.firstName), o.id` guarantees consistent order only for the ASCII-only sample data. Non-ASCII names may sort differently across profiles due to collation differences (H2 code-point order, PostgreSQL database locale, MySQL `ai_ci` accent folding).

**Rationale**: The spec's Assumptions section records this limitation and states the product owner accepts it for the current scope. The acceptance criteria (AC-11) are written against the sample data only. Attempting to normalise collation across all three profiles for non-ASCII data would require profile-specific queries or schema changes, both of which are out of scope.

**Alternatives considered**:
- **Binary collation on all profiles** — Would require schema changes and profile-specific DDL. Out of scope; flagged as a data-track decision if the product owner later requires it.
- **Application-side sorting after retrieval** — Would break pagination correctness (page boundaries depend on database-side ordering). Rejected.

---

## 13. Spec 001 Amendment

**Decision**: A minor amendment to spec `001-owners-spring-petclinic` is required to record that owner search by last name is now case-insensitive and that results are explicitly ordered by last name, then first name, then id. This amendment is a deliverable of this feature.

**Rationale**: The spec's Assumptions section identifies this amendment explicitly. Without it, spec 001 would describe a case-sensitive search, creating a conflict between the constitution's conflict-resolution rule (constitution, Conflict Resolution section) and the new behaviour.

**Alternatives considered**:
- **Leave spec 001 unchanged** — Creates a documented inconsistency. Rejected per the constitution's conflict-resolution rule.

---

## 14. SQL Logging and PII Handling During Performance Measurement

**Decision**: Configure SQL logging to capture query text and execution plans without bind values during KPI-3 baseline and post-change measurement runs. No owner last names or other PII appear in log files or report attachments.

**Rationale**: FR-015 and constitution principle V mandate this. The EXPLAIN output and generated SQL are required as release evidence (FR-012), but bound values (owner last names) are PII and must be redacted. Hibernate's `show_sql` / `format_sql` properties log query structure without bind values by default; P6Spy or datasource-proxy can be used if more detail is needed, configured to mask bind values.

**Alternatives considered**:
- **Log full queries including bind values** — Violates FR-015 and constitution V. Rejected.
- **Omit SQL and EXPLAIN from the release report** — Violates FR-012. Rejected.

---

## Summary of NEEDS CLARIFICATION Items

| # | Item | Blocking | Resolved by |
|---|---|---|---|
| 3 | MySQL effective collation and pre-change `davis` result count | E3-S4, E3-S6 | E2-S2 |
| 7 | Whether `-parameters` compiler flag is set (mitigated by adding `@Param`) | No (mitigated) | E3-S1 / code inspection |
| 10 | `PetClinicConcurrencyTests` does not call `findByLastNameStartingWith` | E3-S7 merge gate | E3-S7 |
| 11 | Docker available in CI for Testcontainers PostgreSQL and MySQL tests | Release gate E6-S5 | E1-S4 |
