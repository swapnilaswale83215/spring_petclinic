# Change Specification: Java 17 to 21 and Spring Boot 4.1.0 to 4.2.x Upgrade

**Feature Branch**: `002-java-17-to-21-upgrade-5d8272c2`

**Created**: 2026-11-24

**Status**: Draft

**Input**: User description: "Run on Java 21 and Spring Boot 4.2.x."

## Current State *(mandatory)*

The Spring PetClinic application builds and runs on Java 17 and Spring Boot 4.1.0. Both build files pin these versions: `pom.xml` declares `spring-boot-starter-parent` at `4.1.0` and `java.version` at `17`; `build.gradle` declares the Spring Boot Gradle plugin at `4.1.0` and a Java toolchain at `17`. Maven (`./mvnw`) is the acceptance gate; Gradle (`./gradlew`) is kept in sync. Spring Boot 4.1.0 is not at end of support; the upgrade is motivated by adopting the current LTS Java (21) and the current supported Spring Boot line (4.2.x).

The CI pipeline runs two workflows: `maven-build.yml` runs `./mvnw -B verify` on a Java 17 matrix with the `adopt` distribution; `gradle-build.yml` runs `./gradlew build` on a Java 17 matrix with `cache: maven` (incorrectly set for a Gradle job). The `.devcontainer/Dockerfile` pins `VARIANT=17-bullseye` and `JAVA_VERSION=17.0.7-ms`; `devcontainer.json` already requests `21-oracle`, so the two files disagree today. Gitpod runs `./mvnw install` inside the Dockerfile image.

The application exposes the following interfaces that other systems and tests depend on:

- `POST /owners/{ownerId}/pets/new` and `POST /owners/{ownerId}/pets/{petId}/edit` — handled by `PetController`, which rejects duplicate pet names (via `DataIntegrityViolationException` whose message contains `unique_owner_pet_name`) and future birth dates (via `PetValidator` and Spring MVC binding), and redirects on a valid save.
- `PetValidator` enforces: name required, name length at most 30, birth date required, pet type required on new pets.
- Owner registration, edit, search by last name and detail — `OwnerController` and `OwnerRepository`.
- Visit booking and history — `VisitController` and `Visit`.
- Vet directory page and `/vets` JSON resource with a Caffeine-backed `vets` cache — `VetController`, `VetRepository`, `CacheConfiguration` (PETC-15, PETC-16, PETC-17).
- Persistence on three profiles: H2 (default, in-memory), MySQL and PostgreSQL, through Spring Data JPA and Hibernate. Schema is fixed; `ddl-auto` does not alter it.
- Actuator endpoints `/actuator/health` and `/actuator/metrics`, with `management.endpoints.web.exposure.include=*`.
- AOT processing and runtime hints in `PetClinicRuntimeHints` covering `db/*`, `db/*/*`, `messages/*`, `mysql-default-conf` and three serialization types.

The baseline test totals (B0: run, failures, errors, skipped) and the application start result on Java 17 + Boot 4.1.0 are not yet recorded; they must be captured before any change is made (SPEC-APP-4).
Sources: [SPEC-APP-4](design.md#spec-app-4)

## Target State *(mandatory)*

When the change is complete:

- `pom.xml` declares `spring-boot-starter-parent` at a single pinned GA `4.2.x` patch version (the newest available at implementation time, recorded in the PR description) and `java.version` at `21`, with `maven-enforcer` requiring at least Java 21. `./mvnw -B verify` exits 0 on a Java 21 JDK. (SPEC-APP-1, ADR-APP-1)
  Sources: [SPEC-APP-1](design.md#spec-app-1) · [ADR-APP-1](design.md#adr-app-1)
- `build.gradle` declares the Spring Boot Gradle plugin at the same `4.2.x` patch and a Java toolchain at `21`. `./gradlew build` exits 0 on a Java 21 JDK as a sync check only; Maven remains the acceptance gate. (SPEC-APP-2, ADR-APP-2)
  Sources: [SPEC-APP-2](design.md#spec-app-2) · [ADR-APP-2](design.md#adr-app-2)
- Both build files name Java 21 and the identical Spring Boot `4.2.x` patch, with zero mismatches. A version-sync check (`SPEC-APP-3`) enforces this on every CI run. (KPI-3, AC-2)
  Sources: [SPEC-APP-3](design.md#spec-app-3) · [KPI-3](brief.md#kpi-3) · [AC-2](brief.md#ac-2)
- `maven-build.yml` runs on Java 21 with the Temurin distribution. `gradle-build.yml` runs on Java 21 with Temurin and `cache: gradle`. (SPEC-PLAT-1, SPEC-PLAT-2, ADR-PLAT-1)
  Sources: [SPEC-PLAT-1](design.md#spec-plat-1) · [SPEC-PLAT-2](design.md#spec-plat-2) · [ADR-PLAT-1](design.md#adr-plat-1)
- `.devcontainer/Dockerfile` sets `VARIANT=21-bookworm` and `JAVA_VERSION` to the newest Java 21 Microsoft sdkman identifier available at implementation time. `devcontainer.json` remains at `21-oracle`. (SPEC-PLAT-6, ADR-PLAT-4)
  Sources: [SPEC-PLAT-6](design.md#spec-plat-6) · [ADR-PLAT-4](design.md#adr-plat-4)
- All existing behaviour listed in "Behaviour That Must Be Preserved" is unchanged. No controller, validator, template, messages file, schema file or database engine is modified. (ADR-APP-3)
  Sources: [ADR-APP-3](design.md#adr-app-3)
- Dependency versions managed by the Boot 4.2.x BOM (Hibernate, Spring Data, Thymeleaf, Jackson, Caffeine, Micrometer, JDBC drivers, Testcontainers) are not overridden unless a build or test failure requires it; any such override is recorded in the PR with its reason. Checkstyle remains at `12.3.1` and `spring-javaformat` at `0.0.47`; build plugins are bumped only if `./mvnw verify` or `./gradlew build` fails on them. (ADR-APP-5)
  Sources: [ADR-APP-5](design.md#adr-app-5)
- AOT processing exits 0 on Java 21 + Boot 4.2.x; `PetClinicRuntimeHints` is unchanged unless a specific hint failure requires a minimal documented fix. (SPEC-APP-12, ADR-APP-6)
  Sources: [SPEC-APP-12](design.md#spec-app-12) · [ADR-APP-6](design.md#adr-app-6)
- The full Maven test run has 0 failures, 0 errors, and an executed-test count at least equal to B (the count after the new tests of SPEC-APP-5 are added on Java 17 + Boot 4.1.0). No test is removed, disabled or newly skipped. (SPEC-APP-13, KPI-2)
  Sources: [SPEC-APP-5](design.md#spec-app-5) · [SPEC-APP-13](design.md#spec-app-13) · [KPI-2](brief.md#kpi-2)
- A new `@SpringBootTest` class `PetCreateEditRealDbTests` is added (on the Java 17 + Boot 4.1.0 baseline, before the version bump) to verify that the `unique_owner_pet_name` constraint and `PetController`'s duplicate-name handling survive the framework upgrade. The same message check is added to `MySqlIntegrationTests` and `PostgresIntegrationTests`. No existing test, `PetController`, `PetValidator` or `PetControllerTests` is changed. (SPEC-APP-5, ADR-APP-3, ADR-APP-4)
  Sources: [SPEC-APP-5](design.md#spec-app-5) · [ADR-APP-3](design.md#adr-app-3) · [ADR-APP-4](design.md#adr-app-4)
- Rollback is a `git revert` of the upgrade PR plus a redeploy of the previous build, rehearsed on a throw-away branch before merge, targeting a green `./mvnw -B verify` on Java 17 + Boot 4.1.0 within 30 minutes. (SPEC-PLAT-9, ADR-APP-7, ADR-PLAT-6)
  Sources: [SPEC-PLAT-9](design.md#spec-plat-9) · [ADR-APP-7](design.md#adr-app-7) · [ADR-PLAT-6](design.md#adr-plat-6)

What deliberately does not change: `PetController.java`, `PetValidator.java`, pet templates, messages files, `src/main/resources/db/**`, `docker-compose.yml`, `k8s/**`, `deploy-and-test-cluster.yml`, the Maven and Gradle wrapper binaries, and all owner, visit, vet and actuator behaviour.

## Behaviour That Must Be Preserved *(mandatory)*

- **Pet duplicate-name rejection on create**: submitting a new pet whose name already belongs to another pet of the same owner returns HTTP 200 with a `duplicate` error on the `name` field and saves no second pet — verified by `PetCreateEditRealDbTests` (H2) and the added method in `MySqlIntegrationTests` / `PostgresIntegrationTests` (AC-5, SPEC-APP-5, SPEC-APP-6)
  Sources: [AC-5](brief.md#ac-5) · [SPEC-APP-5](design.md#spec-app-5) · [SPEC-APP-6](design.md#spec-app-6)
- **Pet future-birth-date rejection on create and edit**: submitting a pet with a birth date in the future returns HTTP 200 with a validation error on the `birthDate` field and saves no pet — verified by `PetCreateEditRealDbTests` and `PetControllerTests` (AC-6, AC-7, SPEC-APP-5, SPEC-APP-6)
  Sources: [AC-6](brief.md#ac-6) · [AC-7](brief.md#ac-7) · [SPEC-APP-5](design.md#spec-app-5) · [SPEC-APP-6](design.md#spec-app-6)
- **Pet edit own-name redirect**: submitting the edit form with the pet's own current name redirects (3xx) and saves — verified by `PetCreateEditRealDbTests` and `PetControllerTests` (AC-7, SPEC-APP-6)
  Sources: [AC-7](brief.md#ac-7) · [SPEC-APP-6](design.md#spec-app-6)
- **Pet edit duplicate-name rejection**: submitting the edit form with another pet's name returns HTTP 200 with a `duplicate` error on the `name` field — verified by `PetCreateEditRealDbTests` and `PetControllerTests` (AC-7, SPEC-APP-5, SPEC-APP-6)
  Sources: [AC-7](brief.md#ac-7) · [SPEC-APP-5](design.md#spec-app-5) · [SPEC-APP-6](design.md#spec-app-6)
- **Valid pet create saves and redirects**: a pet with a unique name, a valid past birth date and a pet type is saved and the user is redirected to the owner page — verified by `PetControllerTests` (AC-8, SPEC-APP-6)
  Sources: [AC-8](brief.md#ac-8) · [SPEC-APP-6](design.md#spec-app-6)
- **PetValidator rules**: name required, name length at most 30, birth date required, type required on new pets — verified by `PetValidatorTests` unchanged (SPEC-APP-7)
  Sources: [SPEC-APP-7](design.md#spec-app-7)
- **Owner registration, edit, search by last name and detail**: each action returns the same HTTP status and visible result as the Java 17 baseline — verified by `OwnerControllerTests`, `OwnerTests`, `ClinicServiceTests` unchanged (AC-9, SPEC-APP-8)
  Sources: [AC-9](brief.md#ac-9) · [SPEC-APP-8](design.md#spec-app-8)
- **Visit booking and history**: booking a visit and viewing visit history return the same status and result as the baseline — verified by `VisitControllerTests` and `ClinicServiceTests` unchanged (AC-9, SPEC-APP-8)
  Sources: [AC-9](brief.md#ac-9) · [SPEC-APP-8](design.md#spec-app-8)
- **Vet directory page and `/vets` JSON**: two consecutive requests each return the 6 seeded vets with their specialties; the `vets` cache is active — verified by `VetControllerTests` and `VetTests` unchanged (AC-10, SPEC-APP-9, PETC-15, PETC-16, PETC-17)
  Sources: [AC-10](brief.md#ac-10) · [SPEC-APP-9](design.md#spec-app-9)
- **H2 default profile persistence**: `PetClinicIntegrationTests`, `SeedParityTests` and `PetClinicConcurrencyTests` pass on Java 21 + Boot 4.2.x — verified by `./mvnw test` (AC-11, SPEC-APP-10)
  Sources: [AC-11](brief.md#ac-11) · [SPEC-APP-10](design.md#spec-app-10)
- **MySQL and PostgreSQL profile persistence**: `MySqlIntegrationTests` and `PostgresIntegrationTests` pass where a container runtime is available, and are reported as "not run" otherwise — verified by the CI Testcontainers step (AC-11, SPEC-APP-10)
  Sources: [AC-11](brief.md#ac-11) · [SPEC-APP-10](design.md#spec-app-10)
- **Actuator endpoints**: `/actuator/health` returns HTTP 200 and `/actuator/metrics` responds; `management.endpoints.web.exposure.include=*` is unchanged — verified by the CI startup-check step (AC-3, SPEC-APP-11, SPEC-PLAT-4)
  Sources: [AC-3](brief.md#ac-3) · [SPEC-APP-11](design.md#spec-app-11) · [SPEC-PLAT-4](design.md#spec-plat-4)
- **No owner PII in startup log**: no owner name, address or telephone value appears in the startup log — verified by log inspection in CI (SPEC-APP-8)
  Sources: [SPEC-APP-8](design.md#spec-app-8)
- **CI pipeline green**: all Maven-based CI jobs finish green; Docker Compose and the Maven and Gradle wrappers still work — verified by the CI run on the upgrade PR (AC-12, SPEC-PLAT-1, SPEC-PLAT-10)
  Sources: [AC-12](brief.md#ac-12) · [SPEC-PLAT-1](design.md#spec-plat-1) · [SPEC-PLAT-10](design.md#spec-plat-10)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Build and Test on Java 21 (Priority: P1)

A developer or CI system running Java 21 can build, test and start the application using the existing Maven and Gradle commands without any change to the commands themselves. The upgrade is transparent: the same `./mvnw -B verify`, `./mvnw test` and `./mvnw spring-boot:run` invocations that work today on Java 17 continue to work on Java 21 with Spring Boot 4.2.x.

**Why this priority**: This is the primary goal of the change. Without a green build and test run on Java 21, no other scenario is meaningful. It is also the gate for KPI-1 and KPI-2.
Sources: [KPI-1](brief.md#kpi-1) · [KPI-2](brief.md#kpi-2)

**Independent Test**: Run `./mvnw -B verify` on a clean checkout on a Java 21 JDK and confirm exit code 0 with 0 failures. Run `./mvnw spring-boot:run` and confirm startup with 0 errors and `/actuator/health` returning HTTP 200.

**Acceptance Scenarios**:

1. **Given** the upgrade branch with `pom.xml` on Java 21 and Spring Boot 4.2.x and a clean checkout on a Java 21 JDK, **When** `./mvnw -B verify` runs, **Then** the build exits 0 with 0 failures (AC-1, KPI-1, SPEC-APP-1)
  Sources: [AC-1](brief.md#ac-1) · [KPI-1](brief.md#kpi-1) · [SPEC-APP-1](design.md#spec-app-1)
2. **Given** the upgrade branch on a Java 21 JDK with the default H2 profile, **When** the application starts with `./mvnw spring-boot:run` and `/actuator/health` is requested, **Then** startup completes with 0 errors in the log, the log shows Java 21 and Spring Boot 4.2.x, and the endpoint returns HTTP 200 (AC-3, KPI-1, SPEC-APP-11, SPEC-PLAT-4)
  Sources: [AC-3](brief.md#ac-3) · [KPI-1](brief.md#kpi-1) · [SPEC-APP-11](design.md#spec-app-11) · [SPEC-PLAT-4](design.md#spec-plat-4)
3. **Given** the upgrade branch on Java 21, **When** `./mvnw test` runs, **Then** 0 tests fail, the number of executed tests is at least B (the baseline count after new tests are added on Java 17 + Boot 4.1.0), and 0 tests are newly skipped or disabled (AC-4, KPI-2, SPEC-APP-13)
  Sources: [AC-4](brief.md#ac-4) · [KPI-2](brief.md#kpi-2) · [SPEC-APP-13](design.md#spec-app-13)
4. **Given** the upgrade branch on a Java 21 JDK, **When** `./gradlew build` runs as a sync check, **Then** the build exits 0 and uses the same Java and Spring Boot versions as `pom.xml` (AC-13, KPI-3, SPEC-APP-2)
  Sources: [AC-13](brief.md#ac-13) · [KPI-3](brief.md#kpi-3) · [SPEC-APP-2](design.md#spec-app-2)

---

### User Story 2 - Build File Version Synchronisation (Priority: P1)

Any developer or automated check reading the project's build files sees the same Java version (21) and the same Spring Boot 4.2.x patch in both `pom.xml` and `build.gradle`. A change to one file that is not reflected in the other is caught immediately by the version-sync check before it reaches the main branch.

**Why this priority**: Drift between the two build files is a stated risk (KPI-3). The sync check is the enforcement mechanism for ADR-APP-2 and is called by CI on every run.
Sources: [KPI-3](brief.md#kpi-3) · [ADR-APP-2](design.md#adr-app-2)

**Independent Test**: Read the Java version and Spring Boot version from both files and confirm zero mismatches. Then change only one file and confirm the sync check fails with a message naming the differing field.

**Acceptance Scenarios**:

1. **Given** the upgrade branch, **When** the Java version and the Spring Boot version are read from `pom.xml` and `build.gradle`, **Then** both files name Java 21 and the same Spring Boot 4.2.x version with 0 mismatches (AC-2, KPI-3, SPEC-APP-3)
  Sources: [AC-2](brief.md#ac-2) · [KPI-3](brief.md#kpi-3) · [SPEC-APP-3](design.md#spec-app-3)
2. **Given** the upgrade branch with only one build file changed to a different version, **When** the version-sync check runs, **Then** it fails with a message naming the field that differs (AC-2, SPEC-APP-3, SPEC-PLAT-5)
  Sources: [AC-2](brief.md#ac-2) · [SPEC-APP-3](design.md#spec-app-3) · [SPEC-PLAT-5](design.md#spec-plat-5)
3. **Given** the Gradle CI job on the upgrade branch, **When** the version-sync check runs before `./gradlew build`, **Then** the check passes and the exact Boot version appears in the CI summary and evidence artifact (KPI-3, SPEC-PLAT-5)
  Sources: [KPI-3](brief.md#kpi-3) · [SPEC-PLAT-5](design.md#spec-plat-5)

---

### User Story 3 - Pet Create and Edit Behaviour Unchanged (Priority: P1)

An owner using the pet create or edit form sees exactly the same validation behaviour on Java 21 + Spring Boot 4.2.x as on the Java 17 + Boot 4.1.0 baseline: duplicate names are rejected, future birth dates are rejected, and valid pets are saved and redirect to the owner page.

**Why this priority**: `PetController` behaviour is explicitly frozen by the product owner. The framework upgrade changes the underlying Spring MVC, validation and Hibernate exception-translation stack; any silent regression here would be a functional defect. This is the highest-risk area of the upgrade.

**Independent Test**: Run `PetCreateEditRealDbTests` (H2, `./mvnw test`) and `PetControllerTests` (unchanged). Confirm all pass on both Java 17 + Boot 4.1.0 (before the bump) and Java 21 + Boot 4.2.x (after).

**Acceptance Scenarios**:

1. **Given** an owner with an existing pet named 'Leo', **When** a new pet named 'Leo' is submitted for the same owner through the create form, **Then** the submission is rejected with a `duplicate` error on the `name` field, the form is redisplayed (HTTP 200) and no second pet is saved (AC-5, SPEC-APP-5, SPEC-APP-6)
  Sources: [AC-5](brief.md#ac-5) · [SPEC-APP-5](design.md#spec-app-5) · [SPEC-APP-6](design.md#spec-app-6)
2. **Given** an owner exists and the create pet form is open, **When** a pet with a valid unique name and a birth date in the future is submitted, **Then** the submission is rejected with a validation error on the `birthDate` field, the form is redisplayed (HTTP 200) and no pet is saved, as on the Java 17 baseline (AC-6, SPEC-APP-5, SPEC-APP-6)
  Sources: [AC-6](brief.md#ac-6) · [SPEC-APP-5](design.md#spec-app-5) · [SPEC-APP-6](design.md#spec-app-6)
3. **Given** an existing pet with another pet belonging to the same owner, **When** the edit form is submitted first with the pet's own current name, then with the name of the owner's other pet, then with a future birth date, **Then** the first submission redirects (3xx) and saves; the second is rejected with a `duplicate` error on the `name` field; the third is rejected with a validation error on the `birthDate` field (AC-7, SPEC-APP-5, SPEC-APP-6)
  Sources: [AC-7](brief.md#ac-7) · [SPEC-APP-5](design.md#spec-app-5) · [SPEC-APP-6](design.md#spec-app-6)
4. **Given** a new pet form, **When** a pet with a valid unique name, a valid past birth date and a pet type is submitted, **Then** the pet is saved, the user is redirected to the owner page, and the pet appears in the owner's pet list (AC-8, SPEC-APP-6, SPEC-APP-7)
  Sources: [AC-8](brief.md#ac-8) · [SPEC-APP-6](design.md#spec-app-6) · [SPEC-APP-7](design.md#spec-app-7)
5. **Given** the `PetCreateEditRealDbTests` class on H2, **When** two copies of the same owner are loaded before either saves and the second `saveAndFlush` adds a pet with the same name, **Then** a `DataIntegrityViolationException` is thrown whose `getMessage().toLowerCase()` contains `unique_owner_pet_name` (SPEC-APP-5)
  Sources: [SPEC-APP-5](design.md#spec-app-5)

---

### User Story 4 - Owner, Visit and Vet Flows Unchanged (Priority: P1)

An owner, receptionist or visitor using the owner registration, edit, search, visit booking or vet directory features sees exactly the same HTTP status and visible result on Java 21 + Boot 4.2.x as on the Java 17 baseline.

**Why this priority**: Owner records are a sensitive zone. Any regression in owner or visit flows is a functional defect. Vet caching is a known integration point (PETC-15, PETC-16, PETC-17).

**Independent Test**: Run `OwnerControllerTests`, `OwnerTests`, `VisitControllerTests`, `ClinicServiceTests`, `VetControllerTests` and `VetTests` unchanged. Confirm all pass.

**Acceptance Scenarios**:

1. **Given** the seeded sample data, **When** an owner is created, edited and found by last name, and a visit is booked for a pet, **Then** each action returns the same HTTP status and visible result as on the Java 17 baseline, and the owner and visit tests pass (AC-9, SPEC-APP-8)
  Sources: [AC-9](brief.md#ac-9) · [SPEC-APP-8](design.md#spec-app-8)
2. **Given** the seeded vet data, **When** the vet list page and the `/vets` JSON resource are requested twice, **Then** both return the 6 seeded vets with their specialties as on the baseline, and the vet tests pass (AC-10, SPEC-APP-9, PETC-15, PETC-16, PETC-17)
  Sources: [AC-10](brief.md#ac-10) · [SPEC-APP-9](design.md#spec-app-9)
3. **Given** the upgrade branch, **When** the MySQL and PostgreSQL integration tests run where a container runtime is available, **Then** they pass with 0 failures on Java 21, and the H2 profile still starts (AC-11, SPEC-APP-10)
  Sources: [AC-11](brief.md#ac-11) · [SPEC-APP-10](design.md#spec-app-10)

---

### User Story 5 - CI Pipeline and Developer Environment on Java 21 (Priority: P2)

A developer opening the repository in a devcontainer or Gitpod, or a CI run triggered by a pull request, uses Java 21 consistently across all platform files. The CI pipeline produces evidence for KPI-1 to KPI-3 on every run and can be rolled back to Java 17 within 30 minutes.
Sources: [KPI-1](brief.md#kpi-1) · [KPI-3](brief.md#kpi-3)

**Why this priority**: Platform consistency prevents "works on my machine" failures. Evidence retention is required for close-out. Rollback rehearsal is required before merge.

**Independent Test**: Inspect `maven-build.yml`, `gradle-build.yml`, `.devcontainer/Dockerfile` and `.gitpod.yml` for Java version settings. Run the CI pipeline on the upgrade PR and confirm all Maven jobs are green. Run the rollback rehearsal on a throw-away branch.

**Acceptance Scenarios**:

1. **Given** the upgrade branch with its pull request, **When** the CI pipeline runs on the pull request, **Then** all Maven-based CI jobs finish green on Java 21, and Docker Compose and the Maven and Gradle wrappers still work (AC-12, SPEC-PLAT-1, SPEC-PLAT-8, SPEC-PLAT-10)
  Sources: [AC-12](brief.md#ac-12) · [SPEC-PLAT-1](design.md#spec-plat-1) · [SPEC-PLAT-8](design.md#spec-plat-8) · [SPEC-PLAT-10](design.md#spec-plat-10)
2. **Given** the Gitpod image built from the updated Dockerfile, **When** `bash -lc 'java -version'` runs inside the image, **Then** it reports Java 21 (SPEC-PLAT-6, ADR-PLAT-4)
  Sources: [SPEC-PLAT-6](design.md#spec-plat-6) · [ADR-PLAT-4](design.md#adr-plat-4)
3. **Given** the upgrade branch, **When** the CI Maven job uploads the `java21-evidence` artifact with `if: always()`, **Then** the artifact holds Surefire and Failsafe reports, totals JSON, `startup.log`, HTTP codes and the Java and Boot versions, and is retained for 90 days (KPI-1, KPI-2, KPI-3, SPEC-PLAT-7)
  Sources: [KPI-1](brief.md#kpi-1) · [KPI-2](brief.md#kpi-2) · [KPI-3](brief.md#kpi-3) · [SPEC-PLAT-7](design.md#spec-plat-7)
4. **Given** a throw-away branch with the upgrade PR reverted, **When** `./mvnw -B verify` runs on Java 17 + Boot 4.1.0, **Then** the build exits 0 and the elapsed time from decision to green main is at most 30 minutes (KPI-1, SPEC-PLAT-9, ADR-APP-7)
  Sources: [KPI-1](brief.md#kpi-1) · [SPEC-PLAT-9](design.md#spec-plat-9) · [ADR-APP-7](design.md#adr-app-7)

---

### Edge Cases

- **Baseline not yet captured**: B0 (run, failures, errors, skipped on Java 17 + Boot 4.1.0) and the application start result must be recorded from `./mvnw -B verify` and `./mvnw spring-boot:run` before any code change. If the baseline is missing when the Java 21 CI run executes, the test-totals comparison step fails (SPEC-APP-4, SPEC-PLAT-3, ADR-APP-4, ADR-PLAT-3).
  Sources: [SPEC-APP-4](design.md#spec-app-4) · [SPEC-PLAT-3](design.md#spec-plat-3) · [ADR-APP-4](design.md#adr-app-4) · [ADR-PLAT-3](design.md#adr-plat-3)
- **Boot 4.2.x BOM moves a managed dependency**: if a library version change introduced by the Boot 4.2.x BOM causes a build or test failure, the dependency is pinned to the working version and the reason is recorded in the PR. No override is made pre-emptively (ADR-APP-5).
  Sources: [ADR-APP-5](design.md#adr-app-5)
- **AOT hint failure on Java 21**: if AOT processing fails on a specific hint, only that hint is changed in `PetClinicRuntimeHints`; the native image build is not part of the gate (SPEC-APP-12, ADR-APP-6).
  Sources: [SPEC-APP-12](design.md#spec-app-12) · [ADR-APP-6](design.md#adr-app-6)
- **`DataIntegrityViolationException` message changes in Boot 4.2.x**: if the exception message no longer contains `unique_owner_pet_name`, `PetController.isDuplicatePetNameViolation` silently stops working. `PetCreateEditRealDbTests` is designed to catch this before the bump (SPEC-APP-5, ADR-APP-3).
  Sources: [SPEC-APP-5](design.md#spec-app-5) · [ADR-APP-3](design.md#adr-app-3)
- **Testcontainers tests without Docker**: `MySqlIntegrationTests` and `PostgresIntegrationTests` are reported as "not run" (never as passes) when no container runtime is available. The totals summary shows them as separate lines (SPEC-PLAT-3, ADR-PLAT-7, SPEC-APP-10).
  Sources: [SPEC-PLAT-3](design.md#spec-plat-3) · [ADR-PLAT-7](design.md#adr-plat-7) · [SPEC-APP-10](design.md#spec-app-10)
- **`build (17)` required check becomes stale**: the matrix change renames the CI check from `build (17)` to `build (21)`. Branch protection must be updated in the same step to avoid a permanently pending required check (SPEC-PLAT-8, ADR-PLAT-5).
  Sources: [SPEC-PLAT-8](design.md#spec-plat-8) · [ADR-PLAT-5](design.md#adr-plat-5)
- **`devcontainer.json` already at 21**: `devcontainer.json` already requests `21-oracle` and is not changed. After the Dockerfile is updated to Java 21, the two files agree. The rollback rehearsal confirms that `devcontainer.json` staying at 21 does not break the Java 17 rollback build (SPEC-PLAT-9, ADR-PLAT-6).
  Sources: [SPEC-PLAT-9](design.md#spec-plat-9) · [ADR-PLAT-6](design.md#adr-plat-6)
- **Gradle test starters absent from `build.gradle`**: `build.gradle` does not include the extra test starters present in `pom.xml` (restclient, thymeleaf-test, validation-test, actuator-test, cache-test). This gap is recorded in the PR. Starters are added to `build.gradle` only if `./gradlew build` fails without them (ADR-APP-2).
  Sources: [ADR-APP-2](design.md#adr-app-2)
- **`ddl-auto` schema alteration**: if `ddl-auto` is set to `update` in any application properties file, Hibernate could alter the schema on startup. This must be confirmed as not set to `update` before the upgrade is merged (ADR-APP-7, SPEC-APP-10).
  Sources: [ADR-APP-7](design.md#adr-app-7) · [SPEC-APP-10](design.md#spec-app-10)
- **Staged commit order**: the Boot-4.2-on-17 commit must be tested on Java 17 before the Java 21 commit is made, so that framework breaks and JDK breaks are isolated. The Maven CI matrix must remain `['17']` until the Java 21 commit (SPEC-PLAT-1, ADR-APP-4, ADR-PLAT-3).
  Sources: [SPEC-PLAT-1](design.md#spec-plat-1) · [ADR-APP-4](design.md#adr-app-4) · [ADR-PLAT-3](design.md#adr-plat-3)
- **`adopt` distribution and Java 21 availability**: if `adopt` does not provide a Java 21 build on the GitHub Actions runner at implementation time, `temurin` is used as the replacement in both CI workflows. [NEEDS CLARIFICATION: confirm whether `adopt` has a Java 21 build available on the GitHub Actions runner at implementation time, or whether `temurin` is required] (ADR-PLAT-1)
  Sources: [ADR-PLAT-1](design.md#adr-plat-1)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001** (SPEC-APP-1): The system MUST build successfully with `./mvnw -B verify` on a Java 21 JDK after `pom.xml` is updated to declare `spring-boot-starter-parent` at the pinned Boot 4.2.x GA patch version and `java.version` at `21`, with `maven-enforcer` requiring at least Java 21; Checkstyle, javaformat and nohttp validation MUST pass without a rule change (AC-1, KPI-1, SPEC-APP-1, ADR-APP-1)
  Sources: [SPEC-APP-1](design.md#spec-app-1) · [AC-1](brief.md#ac-1) · [KPI-1](brief.md#kpi-1) · [ADR-APP-1](design.md#adr-app-1)
- **FR-002** (SPEC-APP-2): The system MUST build successfully with `./gradlew build` on a Java 21 JDK after `build.gradle` is updated to declare the Spring Boot Gradle plugin at the same pinned 4.2.x patch and a Java toolchain at `21`; this is a sync check only and does not replace Maven as the acceptance gate; any added Gradle dependency or plugin bump MUST be listed in the PR with its reason (AC-13, KPI-3, SPEC-APP-2, ADR-APP-2)
  Sources: [SPEC-APP-2](design.md#spec-app-2) · [AC-13](brief.md#ac-13) · [KPI-3](brief.md#kpi-3) · [ADR-APP-2](design.md#adr-app-2)
- **FR-003** (SPEC-APP-3): The system MUST provide a version-sync check that reads the Java version and the Spring Boot version from `pom.xml` and `build.gradle`, returns 0 mismatches when both files agree, and fails with a message naming the differing field when one file is changed alone; the check MUST run without the Maven test suite and add no tests to it (AC-2, KPI-3, SPEC-APP-3, ADR-APP-2)
  Sources: [SPEC-APP-3](design.md#spec-app-3) · [AC-2](brief.md#ac-2) · [KPI-3](brief.md#kpi-3) · [ADR-APP-2](design.md#adr-app-2)
- **FR-004** (SPEC-APP-4): The named baseline owner MUST capture B0 (run, failures, errors, skipped from Surefire XML only, with MySQL and PostgreSQL class rows as separate lines) and the application start result on Java 17 + Boot 4.1.0 before any other change; after the new tests of FR-005 are added on the same stack, the count B MUST be captured and used as the KPI-2 gate value; the order MUST be: B0 capture, then new tests, then B capture, then the Boot 4.2 commit [NEEDS CLARIFICATION: the PR description names `swapnilaswale83215` as the baseline owner to be confirmed at approval — confirm or substitute the correct owner before the first commit] (AC-4, KPI-1, KPI-2, SPEC-APP-4, ADR-APP-4)
  Sources: [SPEC-APP-4](design.md#spec-app-4) · [KPI-2](brief.md#kpi-2) · [AC-4](brief.md#ac-4) · [KPI-1](brief.md#kpi-1) · [ADR-APP-4](design.md#adr-app-4)
- **FR-005** (SPEC-APP-5): The system MUST add a new `@SpringBootTest` class `PetCreateEditRealDbTests` in `src/test/java/.../owner` that runs on the default H2 profile under `./mvnw test` and requires no Docker; it MUST verify: (a) a `DataIntegrityViolationException` whose `getMessage().toLowerCase()` contains `unique_owner_pet_name` is thrown at the repository level when a pet with a duplicate name is flushed; (b) `PetController` returns HTTP 200 with a `duplicate` field error on `name` on create and edit when the in-memory check passes but the constraint fires; (c) edit with a future birth date returns HTTP 200 with a `typeMismatch.birthDate` field error and leaves the stored pet unchanged; no existing test, `PetController.java`, `PetValidator.java` or `PetControllerTests.java` MUST be changed; `git diff` MUST show `PetControllerTests.java` unchanged (AC-5, AC-6, AC-7, SPEC-APP-5, ADR-APP-3, ADR-APP-4)
  Sources: [SPEC-APP-5](design.md#spec-app-5) · [AC-5](brief.md#ac-5) · [AC-6](brief.md#ac-6) · [AC-7](brief.md#ac-7) · [ADR-APP-3](design.md#adr-app-3) · [ADR-APP-4](design.md#adr-app-4)
- **FR-006** (SPEC-APP-5): The system MUST add one method each to `MySqlIntegrationTests` and `PostgresIntegrationTests` that verifies the `unique_owner_pet_name` message check; these methods MUST pass where Docker is available and be reported as "not run" otherwise (AC-11, SPEC-APP-5, ADR-PLAT-7)
  Sources: [SPEC-APP-5](design.md#spec-app-5) · [AC-11](brief.md#ac-11) · [ADR-PLAT-7](design.md#adr-plat-7)
- **FR-007** (SPEC-APP-6): `PetController.java`, `PetValidator.java`, the pet templates and the messages files MUST remain byte-for-byte identical to the baseline; `git diff` MUST show no change to these files (AC-5, AC-6, AC-7, AC-8, SPEC-APP-6, ADR-APP-3)
  Sources: [SPEC-APP-6](design.md#spec-app-6) · [AC-5](brief.md#ac-5) · [AC-6](brief.md#ac-6) · [AC-7](brief.md#ac-7) · [AC-8](brief.md#ac-8) · [ADR-APP-3](design.md#adr-app-3)
- **FR-008** (SPEC-APP-7): `PetValidator.java` and `PetValidatorTests` MUST remain unchanged; `PetValidatorTests` MUST pass on Java 21 + Boot 4.2.x (AC-8, SPEC-APP-7, ADR-APP-3)
  Sources: [SPEC-APP-7](design.md#spec-app-7) · [AC-8](brief.md#ac-8) · [ADR-APP-3](design.md#adr-app-3)
- **FR-009** (SPEC-APP-8): Owner registration, edit, search by last name and detail, and visit booking and history MUST return the same HTTP status and visible result as the Java 17 baseline on Boot 4.2.x; `OwnerControllerTests`, `OwnerTests`, `VisitControllerTests` and `ClinicServiceTests` MUST pass unchanged; no owner name, address or telephone value MUST appear in the startup log (AC-9, SPEC-APP-8)
  Sources: [SPEC-APP-8](design.md#spec-app-8) · [AC-9](brief.md#ac-9)
- **FR-010** (SPEC-APP-9): The vet list page and `/vets` JSON resource MUST return the 6 seeded vets with their specialties on two consecutive requests, with the `vets` cache active and created at startup with no error; `VetControllerTests` and `VetTests` MUST pass unchanged; if `CacheConfiguration` is changed, the change MUST be limited to what is needed to compile against Boot 4.2.x (AC-10, SPEC-APP-9, PETC-15, PETC-16, PETC-17)
  Sources: [SPEC-APP-9](design.md#spec-app-9) · [AC-10](brief.md#ac-10)
- **FR-011** (SPEC-APP-10): The H2 default profile MUST start and its tests (`PetClinicIntegrationTests`, `SeedParityTests`, `PetClinicConcurrencyTests`) MUST pass on Java 21 + Boot 4.2.x; `MySqlIntegrationTests` and `PostgresIntegrationTests` MUST pass where Docker is available and be reported as "not run" otherwise; `git diff` MUST show no change under `src/main/resources/db`; `ddl-auto` MUST NOT be set to `update` (AC-11, SPEC-APP-10, ADR-APP-7)
  Sources: [SPEC-APP-10](design.md#spec-app-10) · [AC-11](brief.md#ac-11) · [ADR-APP-7](design.md#adr-app-7)
- **FR-012** (SPEC-APP-11): With the default H2 profile on Java 21, `./mvnw spring-boot:run` MUST start with 0 ERROR lines in the log, log Java 21 and the pinned Boot 4.2.x version, and serve `/actuator/health` with HTTP 200 and `/actuator/metrics` with HTTP 200; `management.endpoints.web.exposure.include=*` MUST remain unchanged (AC-3, KPI-1, SPEC-APP-11, SPEC-PLAT-4, ADR-APP-10)
  Sources: [SPEC-APP-11](design.md#spec-app-11) · [AC-3](brief.md#ac-3) · [KPI-1](brief.md#kpi-1) · [SPEC-PLAT-4](design.md#spec-plat-4) · [ADR-APP-10](design.md#adr-app-10)
- **FR-013** (SPEC-APP-12): AOT processing MUST exit 0 on Java 21 + Boot 4.2.x; the generated hints MUST include entries for `db/*`, `db/*/*`, `messages/*`, `mysql-default-conf` and the three serialization types; `PetClinicRuntimeHints` MUST be unchanged unless a specific hint failure requires a minimal documented fix (KPI-1, SPEC-APP-12, ADR-APP-6)
  Sources: [SPEC-APP-12](design.md#spec-app-12) · [KPI-1](brief.md#kpi-1) · [ADR-APP-6](design.md#adr-app-6)
- **FR-014** (SPEC-APP-13): The full `./mvnw test` run on Java 21 MUST report 0 failures and 0 errors; the executed-test count MUST be at least B from FR-004; `git diff` MUST show no new `@Disabled` annotation, no deleted test file and no deleted test method; `I18nPropertiesSyncTest`, `WelcomeControllerTests`, `CrashController` tests, `ValidatorTests` and `PetTypeFormatterTests` MUST pass unchanged (AC-4, KPI-2, SPEC-APP-13, ADR-APP-9)
  Sources: [SPEC-APP-13](design.md#spec-app-13) · [AC-4](brief.md#ac-4) · [KPI-2](brief.md#kpi-2) · [ADR-APP-9](design.md#adr-app-9)
- **FR-015** (SPEC-PLAT-1): `maven-build.yml` MUST set the Java matrix to `['21']` with the `temurin` distribution, keep `cache: maven`, run `./mvnw -B verify`, print `java -version`, set `permissions: contents: read` and `timeout-minutes`, and hold no Spring Boot version; before the Java 21 commit the matrix MUST remain `['17']` so the Boot-4.2-on-17 commit is tested on Java 17 (AC-1, AC-12, KPI-1, KPI-2, SPEC-PLAT-1, ADR-PLAT-1, ADR-APP-8)
  Sources: [SPEC-PLAT-1](design.md#spec-plat-1) · [AC-1](brief.md#ac-1) · [AC-12](brief.md#ac-12) · [KPI-1](brief.md#kpi-1) · [KPI-2](brief.md#kpi-2) · [ADR-PLAT-1](design.md#adr-plat-1) · [ADR-APP-8](design.md#adr-app-8)
- **FR-016** (SPEC-PLAT-2): `gradle-build.yml` MUST set the Java matrix to `['21']` with the `temurin` distribution, replace `cache: maven` with `cache: gradle`, run `./gradlew build`, exit 0 on the upgrade branch, and hold no Spring Boot version (AC-13, KPI-3, SPEC-PLAT-2, ADR-PLAT-1, ADR-APP-8)
  Sources: [SPEC-PLAT-2](design.md#spec-plat-2) · [AC-13](brief.md#ac-13) · [KPI-3](brief.md#kpi-3) · [ADR-PLAT-1](design.md#adr-plat-1) · [ADR-APP-8](design.md#adr-app-8)
- **FR-017** (SPEC-PLAT-3): The Maven CI job MUST record test totals (run, failures, errors, skipped) with separate lines for `MySqlIntegrationTests` and `PostgresIntegrationTests` on every run; in compare mode it MUST fail when failures or errors > 0, executed < B, or skipped > baseline skipped; on a Java 21 run with no baseline file the step MUST fail; the step MUST add no test to the Maven suite (AC-4, AC-11, KPI-2, SPEC-PLAT-3, ADR-PLAT-3, ADR-APP-9)
  Sources: [SPEC-PLAT-3](design.md#spec-plat-3) · [AC-4](brief.md#ac-4) · [AC-11](brief.md#ac-11) · [KPI-2](brief.md#kpi-2) · [ADR-PLAT-3](design.md#adr-plat-3) · [ADR-APP-9](design.md#adr-app-9)
- **FR-018** (SPEC-PLAT-4): After `verify`, CI MUST start the application on Java 21 with the default H2 profile, reach HTTP 200 on `/actuator/health` within 120 seconds (otherwise the step fails), confirm `startup.log` has 0 ERROR lines and shows Java 21 and the Boot version equal to the `pom.xml` parent version, confirm `/actuator/metrics` returns HTTP 200 with a non-empty names list, and upload `startup.log` in every case (AC-3, KPI-1, SPEC-PLAT-4, ADR-PLAT-2, ADR-APP-10)
  Sources: [SPEC-PLAT-4](design.md#spec-plat-4) · [AC-3](brief.md#ac-3) · [KPI-1](brief.md#kpi-1) · [ADR-PLAT-2](design.md#adr-plat-2) · [ADR-APP-10](design.md#adr-app-10)
- **FR-019** (SPEC-PLAT-5): CI MUST call the version-sync check on every run before `./gradlew build`; the output MUST name the differing field; a PR that changes only one build file MUST fail this step; the exact Boot version MUST appear in the CI summary and evidence artifact (AC-2, KPI-3, SPEC-PLAT-5, ADR-APP-1, ADR-APP-2)
  Sources: [SPEC-PLAT-5](design.md#spec-plat-5) · [AC-2](brief.md#ac-2) · [KPI-3](brief.md#kpi-3) · [ADR-APP-1](design.md#adr-app-1) · [ADR-APP-2](design.md#adr-app-2)
- **FR-020** (SPEC-PLAT-6): `.devcontainer/Dockerfile` MUST set `VARIANT` and `JAVA_VERSION` to Java 21 values; `devcontainer.json` MUST remain at `21-oracle`; `.gitpod.yml` MUST hold no Java version; `devcontainer-build.yml` MUST build the Dockerfile and fail unless `java -version` reports 21; a grep of `.github`, `.devcontainer` and `.gitpod.yml` MUST find no Java 17 setting (AC-12, KPI-1, SPEC-PLAT-6, ADR-PLAT-4, ADR-APP-8)
  Sources: [SPEC-PLAT-6](design.md#spec-plat-6) · [AC-12](brief.md#ac-12) · [KPI-1](brief.md#kpi-1) · [ADR-PLAT-4](design.md#adr-plat-4) · [ADR-APP-8](design.md#adr-app-8)
- **FR-021** (SPEC-PLAT-7): Every Maven and Gradle run on the upgrade branch MUST upload artifact `java21-evidence` with `if: always()`, retained 90 days, containing Surefire and Failsafe reports, totals JSON, `startup.log`, HTTP codes and the Java and Boot versions; a failed job MUST still upload the artifact; the job summary MUST show build result, startup result, executed tests and the version pair (KPI-1, KPI-2, KPI-3, SPEC-PLAT-7, ADR-PLAT-2)
  Sources: [SPEC-PLAT-7](design.md#spec-plat-7) · [KPI-1](brief.md#kpi-1) · [KPI-2](brief.md#kpi-2) · [KPI-3](brief.md#kpi-3) · [ADR-PLAT-2](design.md#adr-plat-2)
- **FR-022** (SPEC-PLAT-8): Branch protection on `main` MUST require the Maven job and the Gradle job under the names they have after the matrix change; no required check MUST be left waiting for `build (17)`; the PR MUST require the product owner's approval; the PR description MUST list the pinned Boot patch, the baseline numbers and the `devcontainer-build` result; no workflow MUST deploy the build (AC-12, SPEC-PLAT-8, ADR-PLAT-5, KPI-1)
  Sources: [SPEC-PLAT-8](design.md#spec-plat-8) · [AC-12](brief.md#ac-12) · [ADR-PLAT-5](design.md#adr-plat-5) · [KPI-1](brief.md#kpi-1)
- **FR-023** (SPEC-PLAT-9): Before merge, a rollback rehearsal on a throw-away branch MUST give a green Maven job on Java 17 (matrix back to `['17']` with `adopt`); the revert MUST change both build files, both workflows and the Dockerfile in one commit and require no database step; the elapsed time from decision to green `main` MUST be recorded in the PR and MUST be at most 30 minutes (KPI-1, AC-12, SPEC-PLAT-9, ADR-PLAT-6, ADR-APP-7)
  Sources: [SPEC-PLAT-9](design.md#spec-plat-9) · [KPI-1](brief.md#kpi-1) · [AC-12](brief.md#ac-12) · [ADR-PLAT-6](design.md#adr-plat-6) · [ADR-APP-7](design.md#adr-app-7)
- **FR-024** (SPEC-PLAT-10): `git diff` MUST show no change to `docker-compose.yml`, `k8s/**` or `deploy-and-test-cluster.yml`; Docker Compose-based tests and the MySQL and PostgreSQL integration tests MUST run on `ubuntu-latest` or be shown as "not run"; `./mvnw` and `./gradlew` MUST run on JDK 21 in CI (AC-11, AC-12, SPEC-PLAT-10, ADR-PLAT-7)
  Sources: [SPEC-PLAT-10](design.md#spec-plat-10) · [AC-11](brief.md#ac-11) · [AC-12](brief.md#ac-12) · [ADR-PLAT-7](design.md#adr-plat-7)

### Key Entities *(include if the change touches data)*

- **`pom.xml`**: The Maven build descriptor and acceptance gate. Today: `spring-boot-starter-parent` at `4.1.0`, `java.version` at `17`. After: `spring-boot-starter-parent` at the pinned Boot 4.2.x GA patch, `java.version` at `21`, `maven-enforcer` requiring at least `21`. All BOM-managed dependency versions (Hibernate, Spring Data, Thymeleaf, Jackson, Caffeine, Micrometer, JDBC drivers, Testcontainers) follow the new BOM unless a failure requires an explicit pin. Checkstyle stays at `12.3.1`; `spring-javaformat` stays at `0.0.47`. (SPEC-APP-1, ADR-APP-1, ADR-APP-5)
  Sources: [SPEC-APP-1](design.md#spec-app-1) · [ADR-APP-1](design.md#adr-app-1) · [ADR-APP-5](design.md#adr-app-5)
- **`build.gradle`**: The Gradle build descriptor, kept in sync with `pom.xml` as a secondary check. Today: Boot plugin at `4.1.0`, Java toolchain at `17`. After: Boot plugin at the same 4.2.x patch as `pom.xml`, Java toolchain at `21`. The extra test starters present in `pom.xml` but absent from `build.gradle` are added only if `./gradlew build` fails without them; the gap is recorded in the PR. (SPEC-APP-2, ADR-APP-2)
  Sources: [SPEC-APP-2](design.md#spec-app-2) · [ADR-APP-2](design.md#adr-app-2)
- **`maven-build.yml`**: The Maven CI workflow. Today: matrix `['17']`, distribution `adopt`, `cache: maven`. After: matrix `['21']`, distribution `temurin`, `cache: maven` retained, `./mvnw -B verify` as the gate job, `java -version` printed, `permissions: contents: read` and `timeout-minutes` set, no Boot version hard-coded. (SPEC-PLAT-1, ADR-PLAT-1)
  Sources: [SPEC-PLAT-1](design.md#spec-plat-1) · [ADR-PLAT-1](design.md#adr-plat-1)
- **`gradle-build.yml`**: The Gradle CI workflow. Today: matrix `['17']`, `cache: maven` (incorrect). After: matrix `['21']`, distribution `temurin`, `cache: gradle`, `./gradlew build` as the sync job, no Boot version hard-coded. (SPEC-PLAT-2, ADR-PLAT-1)
  Sources: [SPEC-PLAT-2](design.md#spec-plat-2) · [ADR-PLAT-1](design.md#adr-plat-1)
- **`.devcontainer/Dockerfile`**: The devcontainer and Gitpod base image. Today: `VARIANT=17-bullseye`, `JAVA_VERSION=17.0.7-ms`. After: `VARIANT=21-bookworm`, `JAVA_VERSION` set to the newest Java 21 Microsoft sdkman identifier available at implementation time. (SPEC-PLAT-6, ADR-PLAT-4)
  Sources: [SPEC-PLAT-6](design.md#spec-plat-6) · [ADR-PLAT-4](design.md#adr-plat-4)
- **`ci/baseline-test-totals.json`**: A new file committed by the application track after B is captured. Holds run, failures, errors and skipped from the Surefire XML on Java 17 + Boot 4.1.0 after the new tests of FR-005 are added. Used by the CI test-totals comparison step in compare mode. (SPEC-APP-4, SPEC-PLAT-3, ADR-APP-4, ADR-PLAT-3)
  Sources: [SPEC-APP-4](design.md#spec-app-4) · [SPEC-PLAT-3](design.md#spec-plat-3) · [ADR-APP-4](design.md#adr-app-4) · [ADR-PLAT-3](design.md#adr-plat-3)
- **`PetCreateEditRealDbTests`**: A new `@SpringBootTest` class in `src/test/java/.../owner`. Runs on the default H2 profile under `./mvnw test`, requires no Docker. Verifies the `unique_owner_pet_name` constraint message, the `PetController` duplicate-name HTTP 200 response on create and edit, and the future-birth-date HTTP 200 response on edit. Added on Java 17 + Boot 4.1.0 before the version bump; must pass on both stacks. (SPEC-APP-5, ADR-APP-3, ADR-APP-4)
  Sources: [SPEC-APP-5](design.md#spec-app-5) · [ADR-APP-3](design.md#adr-app-3) · [ADR-APP-4](design.md#adr-app-4)
- **`unique_owner_pet_name` database constraint**: A schema-level unique constraint on the pet table (owner + name). Not changed by this upgrade. Its exception message, as translated by Hibernate and Spring Data into `DataIntegrityViolationException`, must still contain `unique_owner_pet_name` on Boot 4.2.x for `PetController.isDuplicatePetNameViolation` to work. (SPEC-APP-5, ADR-APP-3)
  Sources: [SPEC-APP-5](design.md#spec-app-5) · [ADR-APP-3](design.md#adr-app-3)
- **`ci/test-totals.sh`**: A new script created as part of this change. Reads `target/surefire-reports` only (no `maven-failsafe-plugin` is configured in `pom.xml`). Runs in capture mode on the Java 17 baseline commit and in compare mode from the Boot 4.2 commit onward. (SPEC-PLAT-3, ADR-APP-4, ADR-PLAT-3)
  Sources: [SPEC-PLAT-3](design.md#spec-plat-3) · [ADR-APP-4](design.md#adr-app-4) · [ADR-PLAT-3](design.md#adr-plat-3)

## Rollback *(mandatory)*

- **Trigger**: Any of the following on the upgrade branch: `./mvnw -B verify` exits non-zero on Java 21; a must-not-break behaviour is broken and cannot be fixed within the agreed window; the product owner withdraws approval.
- **Procedure**:
  1. On a throw-away branch, run `git revert` of the upgrade PR commits (or the merge commit). The revert restores `pom.xml`, `build.gradle`, `maven-build.yml`, `gradle-build.yml` and `.devcontainer/Dockerfile` to their Java 17 + Boot 4.1.0 state in one commit.
  2. Push the revert branch and confirm `./mvnw -B verify` exits 0 on Java 17 + Boot 4.1.0 in CI (matrix back to `['17']` with `adopt`).
  3. Merge the revert to `main` once CI is green.
  4. Redeploy the previous build artefact (the running JVM cannot change its own Java or Boot version; a redeploy is required).
  5. No database step is needed: H2 is in-memory and discarded on restart; the MySQL and PostgreSQL schema is unchanged; Hibernate does not alter the schema (confirmed by checking `ddl-auto` is not `update`).
  6. `devcontainer.json` stays at `21-oracle` after the revert and does not affect the Java 17 build.
- **Verification after rollback**: `./mvnw -B verify` exits 0 on Java 17 + Boot 4.1.0 with the same test totals as B0; the application starts with `./mvnw spring-boot:run` and `/actuator/health` returns HTTP 200; the startup log shows Java 17 and Boot 4.1.0. The elapsed time from rollback decision to green `main` must be at most 30 minutes (rehearsed before merge). (KPI-1, SPEC-PLAT-9, ADR-APP-7, ADR-PLAT-6)
  Sources: [KPI-1](brief.md#kpi-1) · [SPEC-PLAT-9](design.md#spec-plat-9) · [ADR-APP-7](design.md#adr-app-7) · [ADR-PLAT-6](design.md#adr-plat-6)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The application builds and starts on Java 21 and Spring Boot 4.2.x with zero build failures and zero startup failures; the startup log confirms Java 21 and Spring Boot 4.2.x; `/actuator/health` returns HTTP 200. Baseline (B0): to be captured from `./mvnw -B verify` and `./mvnw spring-boot:run` on Java 17 + Boot 4.1.0 before any change. Target: 0 build failures; 0 startup failures. (KPI-1, AC-1, AC-3, SPEC-APP-1, SPEC-APP-11, SPEC-PLAT-4)
  Sources: [KPI-1](brief.md#kpi-1) · [AC-1](brief.md#ac-1) · [AC-3](brief.md#ac-3) · [SPEC-APP-1](design.md#spec-app-1) · [SPEC-APP-11](design.md#spec-app-11) · [SPEC-PLAT-4](design.md#spec-plat-4)
- **SC-002**: `./mvnw test` passes with 0 failing tests on Java 21 and Spring Boot 4.2.x; the executed-test count is at least B (the count after new tests are added on Java 17 + Boot 4.1.0); 0 tests are newly skipped or disabled. Baseline (B): to be captured after FR-005 tests are added on Java 17 + Boot 4.1.0. Target: 0 failures; 0 errors; executed ≥ B; 0 newly skipped. (KPI-2, AC-4, SPEC-APP-13, SPEC-PLAT-3)
  Sources: [KPI-2](brief.md#kpi-2) · [AC-4](brief.md#ac-4) · [SPEC-APP-13](design.md#spec-app-13) · [SPEC-PLAT-3](design.md#spec-plat-3)
- **SC-003**: `pom.xml` and `build.gradle` name Java 21 and the identical Spring Boot 4.2.x patch with 0 mismatches; the version-sync check passes on the upgrade branch and fails when one file is changed alone. Baseline: 0 mismatches today (both pin Java 17 and Boot 4.1.0, confirmed by reading both files before any change). Target: 0 mismatches; both files name Java 21 and the same 4.2.x patch. (KPI-3, AC-2, SPEC-APP-3, SPEC-PLAT-5)
  Sources: [KPI-3](brief.md#kpi-3) · [AC-2](brief.md#ac-2) · [SPEC-APP-3](design.md#spec-app-3) · [SPEC-PLAT-5](design.md#spec-plat-5)

## Assumptions

- The pinned Spring Boot 4.2.x GA patch version is the newest available at implementation time; the exact patch is recorded in the PR description before the first code commit (ADR-APP-1).
  Sources: [ADR-APP-1](design.md#adr-app-1)
- `ddl-auto` is not set to `update` in any application properties file; this is confirmed by reading the properties files before the upgrade is merged (ADR-APP-7, SPEC-APP-10).
  Sources: [ADR-APP-7](design.md#adr-app-7) · [SPEC-APP-10](design.md#spec-app-10)
- The `adopt` distribution does not provide a Java 21 build on the GitHub Actions runner at implementation time; `temurin` is used as the replacement in both CI workflows. [NEEDS CLARIFICATION: confirm whether `adopt` has a Java 21 build available on the GitHub Actions runner at implementation time, or whether `temurin` is required] (ADR-PLAT-1)
  Sources: [ADR-PLAT-1](design.md#adr-plat-1)
- The baseline owner responsible for capturing B0 and B is `swapnilaswale83215`; this must be confirmed by the approver before the first commit (SPEC-APP-4, ADR-APP-4).
  Sources: [SPEC-APP-4](design.md#spec-app-4) · [ADR-APP-4](design.md#adr-app-4)
- The Gradle wrapper version (9.5.1) is compatible with Java 21 and the Boot 4.2.x Gradle plugin; if not, the wrapper is updated and the reason is recorded in the PR (AC-12, AC-13).
  Sources: [AC-12](brief.md#ac-12) · [AC-13](brief.md#ac-13)
- No deployment pipeline exists in the repository; release authorization is the product owner's approval and merge of the PR once the required checks are green (SPEC-PLAT-8, ADR-PLAT-5).
  Sources: [SPEC-PLAT-8](design.md#spec-plat-8) · [ADR-PLAT-5](design.md#adr-plat-5)
- `devcontainer-build.yml` is a new workflow created as part of this change; it is not a branch-protection requirement but must be green for this PR (SPEC-PLAT-6, ADR-PLAT-4, ADR-PLAT-5).
  Sources: [SPEC-PLAT-6](design.md#spec-plat-6) · [ADR-PLAT-4](design.md#adr-plat-4) · [ADR-PLAT-5](design.md#adr-plat-5)
- `ci/test-totals.sh` is a new script created as part of this change; it reads `target/surefire-reports` only, because `pom.xml` configures no `maven-failsafe-plugin` (SPEC-PLAT-3, ADR-APP-4, ADR-PLAT-3).
  Sources: [SPEC-PLAT-3](design.md#spec-plat-3) · [ADR-APP-4](design.md#adr-app-4) · [ADR-PLAT-3](design.md#adr-plat-3)
- Spring Boot 4.1.0 is not at end of support; the upgrade is motivated solely by adopting the current LTS Java (21) and the current supported Spring Boot line (4.2.x), not by a support deadline.
- The PR #144 Pets spec statement that future birth dates are accepted is incorrect and does not drive any test or code change in this upgrade (ADR-APP-3, SPEC-APP-6).
  Sources: [ADR-APP-3](design.md#adr-app-3) · [SPEC-APP-6](design.md#spec-app-6)
- No schema, SQL file, database engine or entity change is made; the MySQL and PostgreSQL schemas are identical before and after the upgrade, and the old build remains compatible with the existing data after a rollback (ADR-APP-7, SPEC-APP-10).
  Sources: [ADR-APP-7](design.md#adr-app-7) · [SPEC-APP-10](design.md#spec-app-10)
