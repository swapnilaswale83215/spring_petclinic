**Status:** Approved: architecture-approval, 2026-11-25

# Change Specification: Java 17 → 21 Runtime Upgrade

**Feature Branch**: `005-transformation-request-java-17-5d6e9213`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "Run the application and its build on a supported JDK: move the Java level from 17 to 21. No feature work. Spring Boot stays at the declared 4.1.0."

## Current State *(mandatory)*

The Spring PetClinic application is a modular monolith built on Spring Boot 4.1.0 with server-side Thymeleaf rendering. It supports three database profiles: H2 in-memory (default), MySQL and PostgreSQL (both Testcontainers-backed for integration tests). Health and metrics are exposed via Spring Boot Actuator and Micrometer. AOT/native runtime hints are registered in `PetClinicRuntimeHints`, covering `BaseEntity`, `Person` and `Vet`.

All Java pins are currently at 17:

- `pom.xml`: `<java.version>17</java.version>`; Maven enforcer rule `requireJavaVersion` and `build-info.properties` report 17
- `build.gradle`: Gradle toolchain `JavaLanguageVersion.of(17)`; Spring Boot plugin version `4.1.0`
- `.github/workflows/maven-build.yml`: matrix `java: [ '17' ]`, distribution `adopt`, cache `maven`, command `./mvnw -B verify`
- `.github/workflows/gradle-build.yml`: matrix `java: [ '17' ]`, distribution `adopt`, cache `maven`
- `.devcontainer/Dockerfile`: `ARG VARIANT=17-bullseye`, `FROM java:0-${VARIANT}`, `ARG JAVA_VERSION=17.0.7-ms`
- `.coderig/harness.yaml` and `.coderig/sensors.json`: sensor images tagged `maven:3.9-eclipse-temurin-17` (4 occurrences across both files)

Java 17 is past or near the end of the support window relied upon by the project. The build, CI and harness still pin 17 while Spring Boot is already on 4.1.0, which requires Java 17 as a minimum and supports Java 21.

The `coderig-gate.yml` workflow enforces a `blocked-scope` gate that treats edits to CI workflow files as blocked unless a named one-PR exception is recorded in the PR body. The repository variable `CODERIG_PIP_SPEC` must be set for `coderig-gate.yml` to exit 0.

JDK 17 baseline test counts (executed, passed, failed, skipped) are not yet recorded; they must be captured after `git pull --ff-only origin main` (sha recorded) on a Docker-capable host before work starts, so that KPI-4 has a concrete baseline.
Sources: [KPI-4](brief.md#kpi-4)

## Target State *(mandatory)*

When the change is complete, every in-scope Java pin reads 21 and the application builds, tests and starts on JDK 21. Spring Boot remains at 4.1.0. No source code, schema, entity, repository, dependency or plugin version is changed unless a proven JDK 21 failure forces it, one file at a time with the failing log cited as evidence.

Specific target values:

- `pom.xml`: `<java.version>21</java.version>`; Maven enforcer and `build-info.properties` report `build.java.source=21` and `build.java.target=21`; Spring Boot parent version remains `4.1.0`
- `build.gradle`: Gradle toolchain `JavaLanguageVersion.of(21)`; Spring Boot plugin version remains `4.1.0`; diff is one line
- `.github/workflows/maven-build.yml`: matrix `java: [ '21' ]`; `distribution: 'adopt'`, `cache: maven` and `./mvnw -B verify` unchanged; diff is one line
- `.github/workflows/gradle-build.yml`: matrix `java: [ '21' ]`; `cache: maven` and `distribution: 'adopt'` unchanged; diff is one line
- `.devcontainer/Dockerfile`: `ARG VARIANT=21-bullseye`, `FROM java:1-${VARIANT}`, `ARG JAVA_VERSION=<sdkman id verified from sdk list java in the 1-21-bullseye image and recorded before editing>`; no other line in that file changes; `.gitpod.yml` and `devcontainer.json` are untouched
- `.coderig/harness.yaml` and `.coderig/sensors.json`: all 4 sensor image tags are `maven:3.9-eclipse-temurin-21`, set through the Launchpad editor; any other line the editor regenerates is listed in the PR body and accepted by the approver
- `PetClinicRuntimeHints.java`: not in the diff; still registers `BaseEntity`, `Person` and `Vet`
- No file under `src/main/java/**/owner`, `src/main/java/**/vet`, `src/main/resources/templates`, `src/main/resources/db`, `deploy/k8s/compose`, `coderig-gate.yml` or `README` appears in the diff
- `.gitpod.yml` and `.devcontainer/devcontainer.json` are not in the diff

The change is delivered as one atomic PR, squash-merged into one commit on `main`, so that a single `git revert` returns every pin to 17.

## Behaviour That Must Be Preserved *(mandatory)*

- **Pet create and edit, including duplicate-name and future-birth-date validation** — verified by existing `PetControllerTests`, `PetValidatorTests` and `ClinicServiceTests` passing on JDK 21 with 0 failures and 0 errors, and the duplicate-name and future-birth-date cases appearing as passed in the surefire report; no `Pet`, `PetController`, `PetValidator` or `PetTypeFormatter` file and no new test file in the diff (AC-9, SPEC-PLAT-16, KPI-4)
  Sources: [AC-9](brief.md#ac-9) · [SPEC-PLAT-16](design.md#spec-plat-16) · [KPI-4](brief.md#kpi-4)
- **Owner register, edit and search, including telephone validation** — verified by existing `OwnerControllerTests`, `OwnerTests` and `PetClinicConcurrencyTests` passing on JDK 21 with 0 failures and 0 errors; the `@Pattern(\d{10})` rule additionally verified by a recorded, non-committed manual check on JDK 21, or the approver explicitly accepts the gap in the PR body; no `Owner`, `OwnerController` or `OwnerRepository` file and no new test file in the diff (AC-11, SPEC-PLAT-17, KPI-4)
  Sources: [AC-11](brief.md#ac-11) · [SPEC-PLAT-17](design.md#spec-plat-17) · [KPI-4](brief.md#kpi-4)
- **Visit booking, including the visit-date boundary rule** — verified by existing `VisitControllerTests` passing on JDK 21 with 0 failures and 0 errors; the boundary (a date not after today is rejected; a later date is accepted) is read from the current `VisitController` code and not changed; no `Visit` or `VisitController` file in the diff (AC-12, SPEC-PLAT-18, KPI-4)
  Sources: [AC-12](brief.md#ac-12) · [SPEC-PLAT-18](design.md#spec-plat-18) · [KPI-4](brief.md#kpi-4)
- **Vet list page and JSON resource** — verified by existing `VetControllerTests` and `VetTests` passing on JDK 21 with 0 failures and 0 errors; no `Vet`, `VetController`, `VetRepository` or `CacheConfiguration` file and no new test file in the diff (AC-13, SPEC-PLAT-19, KPI-4)
  Sources: [AC-13](brief.md#ac-13) · [SPEC-PLAT-19](design.md#spec-plat-19) · [KPI-4](brief.md#kpi-4)
- **Vets cache** — verified by a recorded, non-committed SQL-log check run separately for GET /vets.html and GET /vets: on each endpoint the first call issues N vet-related SQL statements and the second call issues 0, with N on JDK 21 equalling N on the JDK 17 baseline; or the approver explicitly accepts 'vets cache not verified' in the PR body (AC-13, SPEC-PLAT-19)
  Sources: [AC-13](brief.md#ac-13) · [SPEC-PLAT-19](design.md#spec-plat-19)
- **H2 default profile startup** — verified by GET / returning 200 and `/actuator/health` reporting UP on JDK 21 after one cold start; startup log line showing Java 21 captured in the evidence (AC-7, SPEC-PLAT-9, KPI-7)
  Sources: [AC-7](brief.md#ac-7) · [SPEC-PLAT-9](design.md#spec-plat-9) · [KPI-7](brief.md#kpi-7)
- **MySQL and PostgreSQL profiles with no schema or data change** — verified by `MySqlIntegrationTests` and `PostgresIntegrationTests` passing on a Docker-capable host on JDK 21; without Docker they are reported as 'not run (no Docker)' and the criteria are not claimed as verified; no file under `src/main/resources/db` or profile-specific properties in the diff (AC-10, SPEC-PLAT-20, KPI-4)
  Sources: [AC-10](brief.md#ac-10) · [SPEC-PLAT-20](design.md#spec-plat-20) · [KPI-4](brief.md#kpi-4)
- **Existing test suite: 0 failures, 0 errors, executed count not below the JDK 17 baseline** — verified by the full surefire/failsafe report on JDK 21 on a Docker-capable host; executed (run minus skipped) on JDK 21 >= executed on the JDK 17 baseline; skipped on JDK 21 <= skipped on the JDK 17 baseline (AC-4, SPEC-PLAT-7, KPI-4)
  Sources: [AC-4](brief.md#ac-4) · [SPEC-PLAT-7](design.md#spec-plat-7) · [KPI-4](brief.md#kpi-4)
- **AOT/native runtime hints** — verified by `PetClinicRuntimeHints.java` being absent from the PR diff and still registering `BaseEntity`, `Person` and `Vet`; the diff check is the acceptance criterion (AC-14, SPEC-PLAT-21)
  Sources: [AC-14](brief.md#ac-14) · [SPEC-PLAT-21](design.md#spec-plat-21)
- **Owner records (name, address, telephone) not changed or exposed differently** — verified by no owner entity, repository, controller or template file appearing in the diff; owner tests of SPEC-PLAT-17 passing on JDK 21 with the same expected values as on the JDK 17 baseline; a `secret-scan` run and a `pii-exposure` Launchpad review both recorded in the PR body (AC-15, SPEC-PLAT-22)
  Sources: [SPEC-PLAT-17](design.md#spec-plat-17) · [AC-15](brief.md#ac-15) · [SPEC-PLAT-22](design.md#spec-plat-22)
- **Spring Boot version remains 4.1.0** — verified by the parent version in `pom.xml` and the plugin version in `build.gradle` reading `4.1.0` on the check date and again on the merge commit (AC-6, SPEC-PLAT-8, KPI-6)
  Sources: [AC-6](brief.md#ac-6) · [SPEC-PLAT-8](design.md#spec-plat-8) · [KPI-6](brief.md#kpi-6)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Build and CI Run on a Supported JDK (Priority: P1)

The development team and CI pipeline move from JDK 17 to JDK 21 so that the project runs on a supported runtime. Developers building locally and CI runners both use JDK 21. The change is invisible to clinic end-users: no UI, data or behaviour changes.

**Why this priority**: Java 17 is past or near its support window. Running on an unsupported JDK exposes the project to unpatched security vulnerabilities and blocks future dependency upgrades that require Java 21. This is the foundational prerequisite for continued maintenance.

**Independent Test**: Run `./mvnw clean verify` on JDK 21 on a Docker-capable host after applying only the pin changes. Exit code must be 0, surefire/failsafe must show 0 failures and 0 errors, and executed count must be at or above the JDK 17 baseline.

**Acceptance Scenarios**:

1. **Given** the change branch with the `pom.xml` edit applied, **When** `./mvnw validate` runs on JDK 21, **Then** `<java.version>` reads 21, the enforcer passes and `build-info.properties` reports 21 (AC-1, SPEC-PLAT-1, KPI-1)
  Sources: [AC-1](brief.md#ac-1) · [SPEC-PLAT-1](design.md#spec-plat-1) · [KPI-1](brief.md#kpi-1)
2. **Given** the change branch checked out on a JDK 21 runtime, **When** `./mvnw clean verify` runs, **Then** the exit code is 0 (AC-2, SPEC-PLAT-1, KPI-2)
  Sources: [AC-2](brief.md#ac-2) · [SPEC-PLAT-1](design.md#spec-plat-1) · [KPI-2](brief.md#kpi-2)
3. **Given** the change branch on a CI runner with JDK 21, **When** `gradle-build.yml` runs, **Then** the Gradle build result (exit code and run URL) is recorded in the PR evidence, and a failure of it does not block the merge unless the repo admin has confirmed in writing that `gradle-build` is a required check (AC-5, SPEC-PLAT-3, SPEC-PLAT-15, KPI-5)
  Sources: [AC-5](brief.md#ac-5) · [SPEC-PLAT-3](design.md#spec-plat-3) · [SPEC-PLAT-15](design.md#spec-plat-15) · [KPI-5](brief.md#kpi-5)
4. **Given** the application started on JDK 21 with the default H2 profile, **When** GET / and GET /actuator/health are called, **Then** GET / returns 200 and health status is UP; the startup log line showing Java 21 is captured in the evidence (AC-7, SPEC-PLAT-9, KPI-7)
  Sources: [AC-7](brief.md#ac-7) · [SPEC-PLAT-9](design.md#spec-plat-9) · [KPI-7](brief.md#kpi-7)

---

### User Story 2 - Every Java Pin Moves Atomically in One Change (Priority: P1)

All in-scope Java pins (pom, Gradle toolchain, both CI workflows, Gitpod Dockerfile, harness sensor images) move from 17 to 21 in a single PR so that `main` is never left in a mixed-version state and the change can be reverted with one commit.

**Why this priority**: A partial pin move leaves `main` red and makes rollback ambiguous. Atomicity is a hard constraint from the brief and from the coderig `blocked-scope` gate, which requires a named exception for the CI workflow edits.

**Independent Test**: Run the anchored pin check on `main` as a positive control (all pins at 17), then run the same check on the branch (0 in-scope pins at 17). Verify the PR file list matches the allow-list exactly.

**Acceptance Scenarios**:

1. **Given** an anchored pin check that passes on `main` as a positive control finding all pins at 17, **When** the same check runs on the change branch, **Then** 0 in-scope pins read 17; in `.devcontainer/Dockerfile`, `ARG VARIANT` is the 21 bullseye value, the FROM tag prefix is `1-`, and `ARG JAVA_VERSION` is the sdkman JDK 21 identifier recorded from `sdk list java` in the `1-21-bullseye` image; the PR diff touches only the allowed files (`pom.xml`, `build.gradle`, `maven-build.yml`, `gradle-build.yml`, `.devcontainer/Dockerfile`, `.coderig/harness.yaml`, `.coderig/sensors.json`); `.gitpod.yml`, `devcontainer.json` and `README` are unchanged (AC-3, SPEC-PLAT-6, SPEC-PLAT-23, KPI-3)
  Sources: [AC-3](brief.md#ac-3) · [SPEC-PLAT-6](design.md#spec-plat-6) · [SPEC-PLAT-23](design.md#spec-plat-23) · [KPI-3](brief.md#kpi-3)
2. **Given** the PR body with the 'Gate evidence / exception' section naming the scope (pin-only edits to `maven-build.yml` and `gradle-build.yml`, this PR only), exception author `swapnilaswale83215` and approver Sandeep Paithankar, and a local dry run of `coderig gate blocked-scope` completed before the PR is opened with the result and accepted PR-body wording recorded, **When** the `coderig-gate` workflow runs on the PR head, **Then** the `gate` job ends successfully including `coderig gate blocked-scope`, and its run URL is recorded in the PR body (AC-3, SPEC-PLAT-14, KPI-3)
  Sources: [AC-3](brief.md#ac-3) · [SPEC-PLAT-14](design.md#spec-plat-14) · [KPI-3](brief.md#kpi-3)
3. **Given** the sensor images set to `maven:3.9-eclipse-temurin-21` in the Launchpad editor by a person with editor rights (name recorded in the PR body), and the regenerated files committed to the PR branch, **When** an anchored check runs on the committed `.coderig/harness.yaml` and `.coderig/sensors.json`, **Then** 0 `temurin-17` occurrences and 4 `temurin-21` occurrences are found across both files, and the `format` and `tests` sensors pass on the 21 image (SPEC-PLAT-5, KPI-3)
  Sources: [SPEC-PLAT-5](design.md#spec-plat-5) · [KPI-3](brief.md#kpi-3)
4. **Given** a repo-wide search for Java 17 pins on `main` after `git pull --ff-only origin main` (sha recorded), **When** the search results are reviewed, **Then** every hit is listed as in-scope, excluded with a reason, or raised as a conflict to the brief owner; no pin outside the 7 allowed files is found to be in-scope and unaddressed (SPEC-PLAT-23, KPI-3)
  Sources: [SPEC-PLAT-23](design.md#spec-plat-23) · [KPI-3](brief.md#kpi-3)

---

### User Story 3 - Existing Application Behaviour Is Unchanged on JDK 21 (Priority: P1)

Clinic staff and end-users see no change in behaviour: pet management, owner management, visit booking, vet directory and all three database profiles work exactly as they did on JDK 17.

**Why this priority**: This is a runtime-only upgrade. Any regression in application behaviour is a defect introduced by the change, not an accepted trade-off.

**Independent Test**: Run the full test suite on JDK 21 on a Docker-capable host and compare surefire/failsafe totals against the JDK 17 baseline. Supplement with the recorded manual checks for telephone `@Pattern` validation and the vets cache where existing tests do not cover those paths.

**Acceptance Scenarios**:

1. **Given** the JDK 17 baseline test counts recorded from `main` after `git pull --ff-only origin main` (sha recorded) on a Docker-capable host (ubuntu-latest runner of `maven-build.yml` preferred; local Docker Desktop acceptable with the host recorded), **When** the full test suite runs on JDK 21 on a Docker-capable environment, **Then** 0 failures, 0 errors, and executed tests >= the baseline count, with `MySqlIntegrationTests` and `PostgresIntegrationTests` executed; without Docker they are reported as 'not run (no Docker)' and not counted as passed (AC-4, SPEC-PLAT-7, KPI-4)
  Sources: [AC-4](brief.md#ac-4) · [SPEC-PLAT-7](design.md#spec-plat-7) · [KPI-4](brief.md#kpi-4)
2. **Given** an owner with a pet named 'Rex' exists, running on JDK 21, **When** a second pet named 'Rex' is submitted for the same owner, and a pet with a future birth date is submitted, **Then** both submissions are rejected with validation errors (duplicate name, birth date in the future); a valid pet is created and an existing pet is edited (AC-9, SPEC-PLAT-16, KPI-4)
  Sources: [AC-9](brief.md#ac-9) · [SPEC-PLAT-16](design.md#spec-plat-16) · [KPI-4](brief.md#kpi-4)
3. **Given** the application started with the MySQL profile and with the PostgreSQL profile on JDK 21 on a Docker-capable environment (ubuntu-latest runner of `maven-build.yml` preferred; local Docker Desktop acceptable with the host recorded), **When** the profile's integration tests run, **Then** each passes with no schema or data change in the diff; if Docker is unavailable, `MySqlIntegrationTests` and `PostgresIntegrationTests` are reported as 'not run (no Docker)' and this criterion is not claimed as verified (AC-10, SPEC-PLAT-20, KPI-4)
  Sources: [AC-10](brief.md#ac-10) · [SPEC-PLAT-20](design.md#spec-plat-20) · [KPI-4](brief.md#kpi-4)
4. **Given** an owner is registered on JDK 21, **When** an owner is registered or edited with an invalid telephone number, then searched by last name, **Then** the invalid telephone is rejected; a valid owner is saved and returned by the last-name search; the `@Pattern(\d{10})` rule is verified by a recorded, non-committed manual check (submitting '12345', 'abcdefghij' and '1316761638' on JDK 21 and the JDK 17 baseline, with status codes, messages and the person who ran it recorded in the PR evidence), or the approver explicitly accepts 'telephone @Pattern rule not verified' in the PR body (AC-11, SPEC-PLAT-17, KPI-4)
  Sources: [AC-11](brief.md#ac-11) · [SPEC-PLAT-17](design.md#spec-plat-17) · [KPI-4](brief.md#kpi-4)
5. **Given** an existing pet on JDK 21, **When** a visit dated today and a visit dated tomorrow are submitted, **Then** the visit dated today is rejected and the visit dated tomorrow is accepted and listed on the owner page; the boundary is read from the current `VisitController` code and not changed (AC-12, SPEC-PLAT-18, KPI-4)
  Sources: [AC-12](brief.md#ac-12) · [SPEC-PLAT-18](design.md#spec-plat-18) · [KPI-4](brief.md#kpi-4)
6. **Given** the application started on JDK 21 with vet data loaded, **When** the vet list page (GET /vets.html) and the JSON vets resource (GET /vets) are each requested twice using a fresh cold start per endpoint with SQL logging enabled at run time only, **Then** both return the vets with their specialties; for each endpoint the first call issues N vet-related SQL statements and the second call issues 0, with N on JDK 21 equalling N on the JDK 17 baseline (N read from the baseline and recorded); or the approver explicitly accepts 'vets cache not verified' in the PR body (AC-13, SPEC-PLAT-19, KPI-4)
  Sources: [AC-13](brief.md#ac-13) · [SPEC-PLAT-19](design.md#spec-plat-19) · [KPI-4](brief.md#kpi-4)
7. **Given** an owner with name, address and telephone exists on JDK 21, **When** the owner is read on the owner page and in the search results, and the PR diff is checked, **Then** the same name, address and telephone values appear as on the JDK 17 baseline, no new field or endpoint exposes them, and no owner entity, repository or template file is in the diff (AC-15, SPEC-PLAT-22)
  Sources: [AC-15](brief.md#ac-15) · [SPEC-PLAT-22](design.md#spec-plat-22)

---

### User Story 4 - Spring Boot 4.1.0 Remains in Open-Source Support (Priority: P1)

The release manager confirms that Spring Boot 4.1.0 is still in open-source support on the date the final gate evidence is recorded, and again on the merge commit, so that the project is not silently left on an unsupported framework version.

**Why this priority**: A silent move to an unsupported Boot version would introduce the same risk the JDK upgrade is meant to eliminate. The brief requires an explicit check and a separate upgrade track if support has ended.

**Independent Test**: Read the Spring Boot support policy page on the check date and record the open-source support end date for 4.1.0. Repeat on the merge commit date.

**Acceptance Scenarios**:

1. **Given** the change branch and the final gate evidence date recorded in the PR body, **When** the Spring Boot parent version and the support policy are read on the check date and again on the merge commit, **Then** the version is 4.1.0 and its open-source support end date is after the check date, with the date repeated on the merge commit; if the end date is on or before the check date, KPI-6 fails and a separate Boot-upgrade track is raised with no silent bump (AC-6, SPEC-PLAT-8, KPI-6)
  Sources: [KPI-6](brief.md#kpi-6) · [AC-6](brief.md#ac-6) · [SPEC-PLAT-8](design.md#spec-plat-8)

---

### User Story 5 - Change Is Reversible by One Revert (Priority: P1)

If the JDK 21 upgrade causes an unforeseen failure after merge, the team can return to JDK 17 by reverting one commit, rebuilding and redeploying, with no data restore required.

**Why this priority**: The no-data-change constraint means rollback is purely a build and deploy operation. Confirming this before merge reduces incident response time.

**Independent Test**: On a branch, run `git revert` of the squash commit, then run `./mvnw clean verify` on JDK 17 and confirm exit code 0. Record elapsed time. Confirm 0 data restores needed.

**Acceptance Scenarios**:

1. **Given** the merged change on a branch and the one change reverted, **When** the build and deploy run on JDK 17, **Then** the build passes, 0 data restores are needed, and the revert is a single commit or merge (AC-8, SPEC-PLAT-10, KPI-8)
  Sources: [AC-8](brief.md#ac-8) · [SPEC-PLAT-10](design.md#spec-plat-10) · [KPI-8](brief.md#kpi-8)

---

### Edge Cases

- **Mixed-version state during the change**: Because the change is one atomic PR squash-merged into one commit, `main` is never in a mixed-version state. The only mixed-version window is the PR branch itself, which is never deployed to production.
- **Docker unavailable on the verification host**: `MySqlIntegrationTests` and `PostgresIntegrationTests` are reported as 'not run (no Docker)' and never counted as passed. The MySQL and PostgreSQL acceptance criteria (AC-10) are stated as not verified in the gate evidence. (SPEC-PLAT-7, KPI-4)
  Sources: [AC-10](brief.md#ac-10) · [SPEC-PLAT-7](design.md#spec-plat-7) · [KPI-4](brief.md#kpi-4)
- **Gradle build red on JDK 21 but green on JDK 17**: Treated as a JDK 21 failure. Only a proven fix in `build.gradle` with the failing log cited is allowed. If the repo admin has not confirmed `gradle-build` is not a required check, the PR waits. The approver must accept any departure from the brief's 'does not gate' intent in writing. (SPEC-PLAT-15, KPI-5)
  Sources: [SPEC-PLAT-15](design.md#spec-plat-15) · [KPI-5](brief.md#kpi-5)
- **Gradle build was already red on JDK 17**: Recorded as a pre-existing failure; the exit code is still reported in the PR evidence. Branch protection and the Gradle workflow logic are not edited. (SPEC-PLAT-15)
  Sources: [SPEC-PLAT-15](design.md#spec-plat-15)
- **sdkman identifier for JDK 21 not found in `1-21-bullseye`**: The identifier must be verified with `sdk list java` inside the `1-21-bullseye` base image and recorded before editing the Dockerfile. If no suitable identifier is found, the finding is raised to the release manager before the Dockerfile is edited. (SPEC-PLAT-4, KPI-3)
  Sources: [SPEC-PLAT-4](design.md#spec-plat-4) · [KPI-3](brief.md#kpi-3)
- **`coderig gate blocked-scope` dry run fails**: The PR is not opened until the dry run passes. The red result and the exact PR-body wording the gate requires are recorded. The blocked list, `guard.py` and `coderig-gate.yml` stay unchanged. (SPEC-PLAT-14)
  Sources: [SPEC-PLAT-14](design.md#spec-plat-14)
- **Regenerated `.coderig/*` files trip `no-ci-edits`**: If the dry run shows this, the release manager and approver decide in writing whether to list them in the exception before the PR is opened. This is not assumed to be safe. (SPEC-PLAT-14)
  Sources: [SPEC-PLAT-14](design.md#spec-plat-14)
- **Boot 4.1.0 support found to have ended on or before the check date**: KPI-6 is marked failed, a separate Boot-upgrade track is raised, and the JDK 21 PR is not merged until that situation is resolved or the approver explicitly accepts the risk in writing. (AC-6, SPEC-PLAT-8, KPI-6)
  Sources: [KPI-6](brief.md#kpi-6) · [AC-6](brief.md#ac-6) · [SPEC-PLAT-8](design.md#spec-plat-8)
- **Deploy-target JDK unknown**: The JDK of the deploy target (`dsyer/petclinic` image, k8s demo) is not known from this repository. The platform/ops owner must confirm in writing, with the date, that the target can run JDK 21 and that a JDK 17 rebuild can be redeployed, before the release is authorized. Deploy time is not covered by the rollback rehearsal, which covers revert and CI build only. (SPEC-PLAT-12, KPI-7, KPI-8)
  Sources: [SPEC-PLAT-12](design.md#spec-plat-12) · [KPI-7](brief.md#kpi-7) · [KPI-8](brief.md#kpi-8)
- **Branch naming pattern conflict**: The `shiftforge/{transformation_id}` profile pattern may not satisfy the `git-branch-naming` harness rule (`type/issue-id-short-description`). The release manager states which pattern applies and records the branch name that passes the local dry run of SPEC-PLAT-14 in the PR body. (SPEC-PLAT-11, SPEC-PLAT-14)
  Sources: [SPEC-PLAT-14](design.md#spec-plat-14) · [SPEC-PLAT-11](design.md#spec-plat-11)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The build system MUST declare `<java.version>21</java.version>` in `pom.xml` such that the Maven enforcer rule `requireJavaVersion` passes and `META-INF/build-info.properties` reports `build.java.source=21` and `build.java.target=21` on JDK 21; the Spring Boot parent version MUST remain `4.1.0` and every other line in `pom.xml` MUST be unchanged (AC-1, SPEC-PLAT-1, KPI-1, KPI-6)
  Sources: [AC-1](brief.md#ac-1) · [SPEC-PLAT-1](design.md#spec-plat-1) · [KPI-1](brief.md#kpi-1) · [KPI-6](brief.md#kpi-6)
- **FR-002**: The Gradle build MUST declare `JavaLanguageVersion.of(21)` in the toolchain block in `build.gradle`; the `org.springframework.boot` plugin version MUST remain `4.1.0`; the diff of `build.gradle` MUST be one line (AC-1, SPEC-PLAT-1, KPI-1, KPI-3)
  Sources: [AC-1](brief.md#ac-1) · [SPEC-PLAT-1](design.md#spec-plat-1) · [KPI-1](brief.md#kpi-1) · [KPI-3](brief.md#kpi-3)
- **FR-003**: `./mvnw clean verify` MUST exit 0 on JDK 21 (AC-2, SPEC-PLAT-1, KPI-2)
  Sources: [AC-2](brief.md#ac-2) · [SPEC-PLAT-1](design.md#spec-plat-1) · [KPI-2](brief.md#kpi-2)
- **FR-004**: `.github/workflows/maven-build.yml` MUST set the matrix to `java: [ '21' ]`; `distribution: 'adopt'`, `cache: maven` and `./mvnw -B verify` MUST remain unchanged; the diff MUST be one line; the workflow commit MUST be authored by `swapnilaswale83215` with a Conventional Commit message carrying a ticket id (SPEC-PLAT-2, SPEC-PLAT-14, KPI-2, KPI-3)
  Sources: [SPEC-PLAT-2](design.md#spec-plat-2) · [SPEC-PLAT-14](design.md#spec-plat-14) · [KPI-2](brief.md#kpi-2) · [KPI-3](brief.md#kpi-3)
- **FR-005**: `.github/workflows/gradle-build.yml` MUST set the matrix to `java: [ '21' ]`; `cache: maven` and `distribution: 'adopt'` MUST remain unchanged; the diff MUST be one line; the Gradle build result (exit code and run URL) MUST be recorded in the PR evidence; a failure of the Gradle build MUST NOT block the merge unless the repo admin has confirmed in writing that `gradle-build` is a required check; the workflow commit MUST be authored by `swapnilaswale83215` with a Conventional Commit message carrying a ticket id (SPEC-PLAT-3, SPEC-PLAT-15, SPEC-PLAT-14, KPI-5, KPI-3)
  Sources: [SPEC-PLAT-3](design.md#spec-plat-3) · [SPEC-PLAT-15](design.md#spec-plat-15) · [SPEC-PLAT-14](design.md#spec-plat-14) · [KPI-5](brief.md#kpi-5) · [KPI-3](brief.md#kpi-3)
- **FR-006**: `.devcontainer/Dockerfile` MUST be edited to exactly three changed lines: `ARG VARIANT=21-bullseye`, the FROM tag `java:1-${VARIANT}`, and `ARG JAVA_VERSION=<sdkman id verified from sdk list java in the 1-21-bullseye image and recorded before editing>`; no other line in that file MUST change; `.gitpod.yml` and `devcontainer.json` MUST NOT be in the diff (SPEC-PLAT-4, KPI-3, AC-3)
  Sources: [SPEC-PLAT-4](design.md#spec-plat-4) · [KPI-3](brief.md#kpi-3) · [AC-3](brief.md#ac-3)
- **FR-007**: All 4 sensor image tags in `.coderig/harness.yaml` and `.coderig/sensors.json` MUST be `maven:3.9-eclipse-temurin-21`, set through the Launchpad editor by a person with editor rights (name recorded in the PR body) before the pom moves; the `format` and `tests` sensors MUST pass on the 21 image; a drift check MUST compare the live editor values with the committed files at the final gate (SPEC-PLAT-5, KPI-3)
  Sources: [SPEC-PLAT-5](design.md#spec-plat-5) · [KPI-3](brief.md#kpi-3)
- **FR-008**: The change MUST be delivered as one atomic PR; the PR file list MUST be exactly `pom.xml`, `build.gradle`, `maven-build.yml`, `gradle-build.yml`, `.devcontainer/Dockerfile`, `.coderig/harness.yaml` and `.coderig/sensors.json`; the PR MUST be squash-merged into one commit on `main` (SPEC-PLAT-6, KPI-3, KPI-8, AC-3)
  Sources: [SPEC-PLAT-6](design.md#spec-plat-6) · [KPI-3](brief.md#kpi-3) · [KPI-8](brief.md#kpi-8) · [AC-3](brief.md#ac-3)
- **FR-009**: The `coderig gate blocked-scope` gate MUST pass on the PR head; the gate MUST NOT be bypassed or overridden; the run URL MUST be recorded in the PR body; a local dry run (`coderig gate blocked-scope --base <main sha> --branch <branch> --pr-body <file>`) MUST be completed by `swapnilaswale83215` before the PR is opened, with the result and the exact PR-body wording the gate accepts recorded in the PR body (SPEC-PLAT-14, KPI-3, AC-3)
  Sources: [SPEC-PLAT-14](design.md#spec-plat-14) · [KPI-3](brief.md#kpi-3) · [AC-3](brief.md#ac-3)
- **FR-010**: The JDK 17 baseline (sha, exit code, executed/passed/failed/skipped counts) MUST be captured after `git pull --ff-only origin main` on a Docker-capable host before work starts, so that KPI-4 has a concrete baseline; the Gradle build result on JDK 17 and the startup status on JDK 17 MUST also be captured at the same sha (SPEC-PLAT-7, SPEC-PLAT-9, SPEC-PLAT-15, KPI-4, KPI-2, KPI-5, KPI-7)
  Sources: [KPI-4](brief.md#kpi-4) · [SPEC-PLAT-7](design.md#spec-plat-7) · [SPEC-PLAT-9](design.md#spec-plat-9) · [SPEC-PLAT-15](design.md#spec-plat-15) · [KPI-2](brief.md#kpi-2) · [KPI-5](brief.md#kpi-5) · [KPI-7](brief.md#kpi-7)
- **FR-011**: The full test suite MUST run on JDK 21 on a Docker-capable host with 0 failures, 0 errors, and executed count (run minus skipped) >= the JDK 17 baseline executed count; `MySqlIntegrationTests` and `PostgresIntegrationTests` MUST be executed; without Docker they MUST be reported as 'not run (no Docker)' and MUST NOT be counted as passed (AC-4, SPEC-PLAT-7, KPI-4)
  Sources: [AC-4](brief.md#ac-4) · [SPEC-PLAT-7](design.md#spec-plat-7) · [KPI-4](brief.md#kpi-4)
- **FR-012**: The application MUST start on JDK 21 with the default H2 profile; GET / MUST return 200 and `/actuator/health` MUST report UP; the startup log line showing Java 21 MUST be captured in the evidence (AC-7, SPEC-PLAT-9, KPI-7)
  Sources: [AC-7](brief.md#ac-7) · [SPEC-PLAT-9](design.md#spec-plat-9) · [KPI-7](brief.md#kpi-7)
- **FR-013**: Spring Boot MUST remain at version 4.1.0; the PR body MUST record the check date, the support policy source and the open-source end date for Boot 4.1.0; the end date MUST be after the check date; if it is not, KPI-6 MUST be marked failed and a separate Boot-upgrade track MUST be raised with no silent bump; the release manager MUST repeat the check on the merge commit and record the date (AC-6, SPEC-PLAT-8, KPI-6)
  Sources: [KPI-6](brief.md#kpi-6) · [AC-6](brief.md#ac-6) · [SPEC-PLAT-8](design.md#spec-plat-8)
- **FR-014**: `PetClinicRuntimeHints.java` MUST NOT appear in the PR diff and MUST still register `BaseEntity`, `Person` and `Vet`; the diff check is the acceptance criterion; an optional `./mvnw process-aot` run on JDK 21 is recorded as reported evidence only (AC-14, SPEC-PLAT-21)
  Sources: [AC-14](brief.md#ac-14) · [SPEC-PLAT-21](design.md#spec-plat-21)
- **FR-015**: No file under `src/main/java/**/owner`, `src/main/java/**/vet`, `src/main/resources/templates`, `src/main/resources/db`, `deploy/k8s/compose`, `coderig-gate.yml` or `README` MUST appear in the diff; no new test file MUST be committed (SPEC-PLAT-6, AC-3, AC-15)
  Sources: [SPEC-PLAT-6](design.md#spec-plat-6) · [AC-3](brief.md#ac-3) · [AC-15](brief.md#ac-15)
- **FR-016**: A `secret-scan` run (`python .coderig/verify.py --only secret-scan`) MUST be executed by `swapnilaswale83215` on the PR branch and its exit code recorded in the PR body; a `pii-exposure` Launchpad review MUST be run by a person with Launchpad editor rights on the PR diff and its verdict recorded; if either cannot be run, the PR body MUST state this and the diff check ('no owner file in the diff') is the only evidence for SPEC-PLAT-22 (SPEC-PLAT-22, AC-15)
  Sources: [SPEC-PLAT-22](design.md#spec-plat-22) · [AC-15](brief.md#ac-15)
- **FR-017**: The PR MUST be approved and merged by a named human; the agent MUST NOT merge, deploy or edit branch protection; the deploy pipeline MUST run only after human approval (SPEC-PLAT-11, ADR-PLAT-2)
  Sources: [SPEC-PLAT-11](design.md#spec-plat-11) · [ADR-PLAT-2](design.md#adr-plat-2)
- **FR-018**: The PR body MUST include the four required sections (What changed, Why, How tested, Risks and rollback plan) and the 'Gate evidence / exception' section naming the scope, exception author `swapnilaswale83215` and approver Sandeep Paithankar; the approver MUST NOT be the author; the approver's name MUST be confirmed in writing before merge (SPEC-PLAT-11, SPEC-PLAT-14)
  Sources: [SPEC-PLAT-11](design.md#spec-plat-11) · [SPEC-PLAT-14](design.md#spec-plat-14)
- **FR-019**: No source code, schema, entity, repository, dependency or plugin version MUST be changed unless a proven JDK 21 failure forces it, one file at a time with the failing log cited as evidence; any such forced change MUST be listed in the PR body and accepted by the approver (SPEC-PLAT-1, AC-3)
  Sources: [SPEC-PLAT-1](design.md#spec-plat-1) · [AC-3](brief.md#ac-3)
- **FR-020**: A repo-wide search for Java 17 pins on `main` (after `git pull --ff-only origin main`, sha recorded) MUST be run and every hit listed as in-scope, excluded with a reason, or raised as a conflict to the brief owner; the search results MUST be recorded in the PR evidence (SPEC-PLAT-23, KPI-3)
  Sources: [SPEC-PLAT-23](design.md#spec-plat-23) · [KPI-3](brief.md#kpi-3)
- **FR-021**: Whether `gradle-build` is a required check in branch protection MUST be recorded in the PR body as 'unverified' until the repo admin confirms it in writing with the date; the admin's answer MUST be sought before the first JDK 21 run where possible; branch protection and the Gradle workflow logic MUST NOT be edited by this change (SPEC-PLAT-15, SPEC-PLAT-3, KPI-5)
  Sources: [SPEC-PLAT-15](design.md#spec-plat-15) · [SPEC-PLAT-3](design.md#spec-plat-3) · [KPI-5](brief.md#kpi-5)
- **FR-022**: The deploy target's JDK readiness MUST be confirmed in writing by the platform/ops owner (name supplied by the release manager before architecture approval) before the release is authorized; the same owner MUST confirm that a JDK 17 rebuild can be redeployed; the previous JDK 17 runtime MUST remain available until the rollback window closes (SPEC-PLAT-12, KPI-7, KPI-8)
  Sources: [SPEC-PLAT-12](design.md#spec-plat-12) · [KPI-7](brief.md#kpi-7) · [KPI-8](brief.md#kpi-8)

### Key Entities *(include if the change touches data)*

No domain entities, schema or data are changed by this upgrade. The following build and configuration artefacts are the subjects of the change:

- **`pom.xml`**: Declares the Maven build. The `<java.version>` property changes from 17 to 21. The Spring Boot parent version (`4.1.0`), all other properties, dependencies and plugin configurations stay unchanged.
- **`build.gradle`**: Declares the Gradle build. The toolchain `JavaLanguageVersion` changes from 17 to 21. The Spring Boot plugin version (`4.1.0`) and all other declarations stay unchanged.
- **`.github/workflows/maven-build.yml`**: CI workflow for Maven. The `java` matrix value changes from `'17'` to `'21'`. All other steps, commands and settings stay unchanged.
- **`.github/workflows/gradle-build.yml`**: CI workflow for Gradle. The `java` matrix value changes from `'17'` to `'21'`. All other steps, commands and settings stay unchanged.
- **`.devcontainer/Dockerfile`**: Gitpod dev container image definition. Three lines change: `ARG VARIANT`, the `FROM` tag prefix and `ARG JAVA_VERSION`. No other line changes. `.gitpod.yml` and `devcontainer.json` are untouched.
- **`.coderig/harness.yaml` and `.coderig/sensors.json`**: Harness sensor definitions. The 4 sensor image tags change from `maven:3.9-eclipse-temurin-17` to `maven:3.9-eclipse-temurin-21`. Any other line regenerated by the Launchpad editor is listed in the PR body and accepted by the approver.

## Rollback *(mandatory)*

- **Trigger**: A post-merge failure in `maven-build.yml` on `main`, a confirmed application defect on JDK 21 in the target environment, or an explicit decision by the release manager or approver. A Boot 4.1.0 support failure found only after merge starts the Boot-upgrade track, not the rollback, unless the approver decides otherwise.
- **Procedure**:
  1. Run `git revert <squash-commit-sha>` on a branch; this returns all 11 pin lines in 7 files to their JDK 17 values in one commit
  2. Reset the 4 sensor images to `maven:3.9-eclipse-temurin-17` in the Launchpad editor and confirm the committed files match the reverted state
  3. Open a PR for the revert branch; obtain human approval; squash-merge to `main`
  4. CI (`maven-build.yml`) runs on JDK 17; confirm exit code 0
  5. Redeploy the JDK 17 build to the target environment; deploy time is added from the deploy pipeline; the platform/ops owner must have confirmed a JDK 17 rebuild can be redeployed before the original release was authorized
  6. Confirm GET / returns 200 and health is UP on the target after redeploy
- **Verification after rollback**: `./mvnw clean verify` exits 0 on JDK 17 after the revert; 0 data restores are performed; 0 schema or data files are in the revert diff; the anchored pin check finds all in-scope pins back at 17; the elapsed time of the revert and CI build is recorded; deploy time is added from the deploy pipeline (AC-8, SPEC-PLAT-10, KPI-8)
  Sources: [AC-8](brief.md#ac-8) · [SPEC-PLAT-10](design.md#spec-plat-10) · [KPI-8](brief.md#kpi-8)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: `<java.version>` in `pom.xml`, the Maven enforcer rule and `build-info.properties` all report 21 after the change; baseline is 17 in `pom.xml` on `main` (sha recorded after `git pull --ff-only origin main`) (KPI-1)
  Sources: [KPI-1](brief.md#kpi-1)
- **SC-002**: `./mvnw clean verify` exits 0 on JDK 21; baseline exit code captured on JDK 17 on `main` (sha recorded) on a Docker-capable host before work starts (KPI-2)
  Sources: [KPI-2](brief.md#kpi-2)
- **SC-003**: 0 in-scope Java pins remain at 17 after the change; all 11 pin lines in 7 files move in one PR; in `.devcontainer/Dockerfile` `VARIANT` is the 21 bullseye value, the FROM tag prefix is `1-`, and `JAVA_VERSION` is the sdkman JDK 21 identifier recorded from `sdk list java` in the `1-21-bullseye` image; baseline is all pins at 17 on `main` (sha recorded), confirmed by the anchored pin check run as a positive control (KPI-3)
  Sources: [KPI-3](brief.md#kpi-3)
- **SC-004**: 0 surefire/failsafe failures and 0 errors on JDK 21; executed test count (run minus skipped) on JDK 21 >= executed count on the JDK 17 baseline; `MySqlIntegrationTests` and `PostgresIntegrationTests` executed on a Docker-capable host; baseline counts (executed, passed, failed, skipped) captured on JDK 17 on `main` (sha recorded) on a Docker-capable host before work starts; without Docker both classes are 'not run (no Docker)' and the MySQL/PostgreSQL criteria are not claimed as verified (KPI-4)
  Sources: [KPI-4](brief.md#kpi-4)
- **SC-005**: Gradle build result (exit code and run URL) reported in the PR evidence on JDK 21; 0 merges blocked by the Gradle build (unless the repo admin has confirmed `gradle-build` is a required check); baseline Gradle exit code captured on JDK 17 on `main` (sha recorded) before work starts (KPI-5)
  Sources: [KPI-5](brief.md#kpi-5)
- **SC-006**: Spring Boot version is 4.1.0 and its open-source support end date is after the check date (the final gate evidence date recorded in the PR body and repeated on the merge commit); if the end date is on or before the check date, KPI-6 is marked failed and a separate Boot-upgrade track is raised; baseline is Boot 4.1.0 declared on `main` (KPI-6)
  Sources: [KPI-6](brief.md#kpi-6)
- **SC-007**: GET / returns 200 and `/actuator/health` reports UP on JDK 21 with the default H2 profile after one cold start; startup log line showing Java 21 captured in the evidence; baseline startup status captured on JDK 17 on `main` (sha recorded) before work starts (KPI-7)
  Sources: [KPI-7](brief.md#kpi-7)
- **SC-008**: Rollback requires exactly 1 revert commit and 0 data restores; `./mvnw clean verify` passes on JDK 17 after the revert; elapsed time of the revert and CI build recorded; deploy time added from the deploy pipeline after the platform/ops owner confirms redeployability (KPI-8)
  Sources: [KPI-8](brief.md#kpi-8)

## Assumptions

- The sdkman identifier for a JDK 21 distribution in the `1-21-bullseye` base image exists and can be verified with `sdk list java` before the Dockerfile is edited. If no suitable identifier is found, the finding is raised to the release manager before editing.
- `docker manifest inspect maven:3.9-eclipse-temurin-21` succeeds before the sensor images are changed, confirming the target image exists.
- The repository variable `CODERIG_PIP_SPEC` is set; `coderig-gate.yml` exits 1 without it. The repo admin confirms this before the dry run.
- The branch name for this change follows the `shiftforge/{transformation_id}` profile pattern. Whether this satisfies the `git-branch-naming` harness rule (`type/issue-id-short-description`) is an open item; the release manager states which pattern applies and records the branch name that passes the local dry run of SPEC-PLAT-14 in the PR body.
  Sources: [SPEC-PLAT-14](design.md#spec-plat-14)
- The deploy target (`dsyer/petclinic` image, k8s demo) is managed by a platform/ops owner whose identity is not known from this repository. The release manager supplies the person's name before architecture approval. That owner must confirm in writing, with the date, that the target can run JDK 21 and that a JDK 17 rebuild can be redeployed before the release is authorized.
- Whether `gradle-build` is a required check in branch protection is unverified. The repo admin's dated written answer is sought before the first JDK 21 run. Until confirmed, the PR body lists 'Gradle required check in branch protection: unverified'.
- The Spring Boot 4.1.0 open-source support end date is unknown at the time of writing; it is read from the Spring Boot support policy page on the check date. If support has ended, a separate Boot-upgrade track is raised and this change is not merged until that situation is resolved or the approver explicitly accepts the risk in writing.
- JDK 17 baseline test counts (executed, passed, failed, skipped) are not yet recorded. They must be captured on a Docker-capable host after `git pull --ff-only origin main` (sha recorded) before work starts, so that KPI-4 has a concrete baseline.
  Sources: [KPI-4](brief.md#kpi-4)
- The visit-date boundary rule (a date not after today is rejected; a later date is accepted) is read from the current `VisitController` code and assumed unchanged. AC-12 notes this is to be confirmed against the current code.
  Sources: [AC-12](brief.md#ac-12)
- No source, schema, entity, repository, dependency or plugin change is needed to pass `./mvnw clean verify` on JDK 21. If a proven JDK 21 failure forces such a change, it is made one file at a time with the failing log cited, and the spec is updated accordingly.
- The `pii-exposure` sensor is an inferential Launchpad review run by a person with Launchpad editor rights; it is not run by CI. If it cannot be run, the PR body states this and the diff check ('no owner file in the diff') is the only evidence for SPEC-PLAT-22.
  Sources: [SPEC-PLAT-22](design.md#spec-plat-22)
- Regenerated `.coderig/*` lines beyond the 4 image tags (for example pack version reference, meta fields or root path) are listed in the PR body and accepted by the approver; they do not count as pin failures. Whether they trip the `no-ci-edits` gate rule is determined by the dry run of SPEC-PLAT-14 before the PR is opened.
  Sources: [SPEC-PLAT-14](design.md#spec-plat-14)
- The constitution is an unratified draft as of 2026-11-25. Its principles are treated as the authoritative working baseline for this change.
