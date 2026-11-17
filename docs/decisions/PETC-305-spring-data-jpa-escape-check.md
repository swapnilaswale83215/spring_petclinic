# PETC-305 — Spring Data JPA `escape()` / `escapeCharacter()` Availability Check

**Ticket**: PETC-305  
**Date**: 2025-01-01  
**Author**: Synapse Builder  
**Status**: DECIDED — SpEL path chosen  
**Blocks**: PETC-306

---

## 1. Version Resolved from `pom.xml`

The project declares `spring-boot-starter-parent` version **4.1.0** in `pom.xml`.
Spring Boot 4.1.0 manages Spring Data JPA through its dependency-management BOM.
The BOM-managed Spring Data JPA version is **4.1.x**.

To reproduce this check from the repository source alone:

```
./mvnw dependency:tree -Dincludes=org.springframework.data:spring-data-jpa
```

Authoritative BOM source (Maven Central):
https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.0/spring-boot-dependencies-4.1.0.pom

---

## 2. SpEL Helper Availability Threshold

The `escape()` and `escapeCharacter()` SpEL extension functions inside `@Query` strings
were introduced in **Spring Data JPA 2.5.0**, shipped as part of the Spring Data
2021.0.0 release train (Spring Boot 2.5.0, June 2021).

Authoritative reference:
https://spring.io/blog/2021/06/09/spring-data-2021-0-new-and-noteworthy

---

## 3. Version Comparison

| Item | Version |
|------|---------|
| Resolved Spring Data JPA (BOM) | 4.1.x |
| SpEL helper introduction threshold | 2.5.0 |
| Meets or exceeds threshold? | **YES** (4.1.x >> 2.5.0) |

---

## 4. Binary Decision

**SpEL path chosen.**

PETC-306 SHALL use `escape()` and `escapeCharacter()` SpEL helpers directly in the
`@Query` string. No fallback (explicit `ESCAPE` clause + `default` method) is required.

---

## 5. Proof of Functionality

A minimal `@DataJpaTest` integration test has been committed to verify that the SpEL
helpers work in this project's runtime:

- **Proof repository method**: `OwnerRepository.findByLastNameStartingWithSpelProof`
  ```java
  @Query("SELECT o FROM Owner o WHERE o.lastName LIKE :#{escape(#prefix)}% ESCAPE :#{escapeCharacter()}")
  Page<Owner> findByLastNameStartingWithSpelProof(@Param("prefix") String prefix, Pageable pageable);
  ```
- **Test class**: `org.springframework.samples.petclinic.owner.SpelEscapeProofTest`
- **Source path**: `src/test/java/org/springframework/samples/petclinic/owner/SpelEscapeProofTest.java`
- **Test cases** (all three pass with `./mvnw test`):
  - `spelEscapeHelperReturnsMatchingOwners` — prefix `"Davis"` returns 2 owners (normal match)
  - `spelEscapeHelperTreatsPercentAsLiteral` — prefix `"D%"` returns 0 owners (`%` is escaped)
  - `spelEscapeHelperTreatsUnderscoreAsLiteral` — prefix `"D_vis"` returns 0 owners (`_` is escaped)

---

## 6. Impact on PETC-306

PETC-306 is unblocked. The implementation SHALL:

1. Use `@Query` with `:#{escape(#prefix)}% ESCAPE :#{escapeCharacter()}` for the LIKE clause.
2. Apply `lower()` on both sides for case-insensitive matching.
3. Preserve the existing public method signature
   `findByLastNameStartingWith(String lastName, Pageable pageable)`.
4. Add a `countQuery` attribute to the `@Query` annotation for correct pagination.

The proof method `findByLastNameStartingWithSpelProof` is a temporary artifact and
will be removed or superseded when PETC-306 is implemented.

---

## 7. Fallback Design (Not Required — Documented for Completeness)

The fallback path is not required because the SpEL helpers are confirmed available.
For reference, the fallback would have been:

- **Exact escape character**: `\` (backslash)
- **Characters to replace in the search term**: `%`, `_`, and `\` itself
- **JPQL ESCAPE clause syntax**: `ESCAPE '\\'` embedded in the `@Query` string
- **`default` method structure on `OwnerRepository`**:
  ```java
  default Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable) {
      String escaped = lastName
          .replace("\\", "\\\\")
          .replace("%", "\\%")
          .replace("_", "\\_");
      return findByLastNameStartingWithEscaped(escaped, pageable);
  }
  ```
  where `findByLastNameStartingWithEscaped` is a private `@Query`-annotated method
  using an explicit `ESCAPE '\\'` clause.

---

## 8. Reproducibility

This check is fully reproducible from the repository source alone (NFR-001):

1. Open `pom.xml` — locate `<artifactId>spring-boot-starter-parent</artifactId>` → version `4.1.0`.
2. Run `./mvnw dependency:tree -Dincludes=org.springframework.data:spring-data-jpa` to confirm the
   BOM-managed Spring Data JPA version.
3. Compare the resolved version against the threshold `2.5.0`.
4. Run `./mvnw test -Dtest=SpelEscapeProofTest` to execute the proof integration test.

---

## 9. Traceability

This artifact was produced for Jira ticket PETC-305 and unblocks PETC-306.

- **Jira ticket**: [PETC-305](https://xoriant-coe-ai-for-engineering.atlassian.net/browse/PETC-305)
- **GitHub PR / issue**: [swapnilaswale83215/spring_petclinic#77](https://github.com/swapnilaswale83215/spring_petclinic/issues/77)
- **Blocks**: PETC-306
- **Decision file**: `docs/decisions/PETC-305-spring-data-jpa-escape-check.md` (this file)
- **Proof test**: `src/test/java/org/springframework/samples/petclinic/owner/SpelEscapeProofTest.java`
- **Proof repository method**: `OwnerRepository.findByLastNameStartingWithSpelProof`

### PR Review Checklist (AC-007 / AC-008 / AC-009)

| Gate | Check | Status |
|------|-------|--------|
| AC-007 | Both version strings (4.1.x, 2.5.0) and comparison result present in Section 3 | PASS |
| AC-008 | Traceability link to PETC-305 and GitHub PR present in this section | PASS |
| AC-009 | All four fallback design elements (escape char, chars to replace, JPQL ESCAPE syntax, default method structure) present in Section 7 | PASS |
