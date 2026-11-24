# Feature Specification: Java 17 to 21 and Spring Boot Version Upgrade

**Feature Branch**: `001-java-17-to-21-and-spring-boot-fa606d0c`

**Created**: 2026-11-24

**Status**: Draft

**Input**: User description: "Run on a current, supported Java and Spring Boot version."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Build and test the application on Java 21 (Priority: P1)

A developer checks out the change branch, runs the full Maven wrapper build on a JDK 21 toolchain, and expects it to compile, package and pass all tests with exit code 0. The Gradle build is also run on JDK 21 and its result is recorded, though it does not gate the change.

**Why this priority**: Without a green build on Java 21 the upgrade has not happened. Every other story depends on this outcome. It is the primary deliverable of the change.

**Independent Test**: Run `./mvnw clean verify` on a machine with only JDK 21 on the path. A zero exit code and a build log showing the compiler and runtime at Java 21 constitute a complete, independently verifiable result.

**Acceptance Scenarios**:

1. **Given** a clean checkout of the change branch and a JDK 21 toolchain with no other JDK on the path, **When** `./mvnw clean verify` is run, **Then** the build exits with code 0, the build log shows the Maven Enforcer rule passed at Java 21, and build-info reports `java.source` and `java.target` as 21. *(AC-1, SPEC-APP-1)*
2. **Given** the same clean checkout and JDK 21, **When** `./gradlew build` is run via the gradle-build workflow, **Then** the result is recorded in the PR body gate-evidence section; it does not gate the merge. *(AC-11, SPEC-APP-13)*
3. **Given** the full test suite run on JDK 21 in CI, **When** the surefire/failsafe reports are read, **Then** failures plus errors equal 0 and the executed test count is at least the baseline count recorded on main before the change. *(AC-3, SPEC-APP-10)*

---

### User Story 2 — Application starts and serves traffic on Java 21 (Priority: P2)

A developer or operator starts the packaged application cold on JDK 21 with the default H2 profile and confirms that it reaches the started state, serves the home page and reports healthy.

**Why this priority**: A green build that produces an application that cannot start delivers no value. This story confirms the runtime is viable before any functional regression checks are run.

**Independent Test**: Start the packaged jar on JDK 21 with the default profile, request `GET /`, and check the actuator health endpoint. A 200 response and an UP status with no startup errors in the log constitute a complete, independently verifiable result.

**Acceptance Scenarios**:

1. **Given** the packaged application and JDK 21 with the default H2 profile, **When** the application is started cold and `GET /` is requested once startup completes, **Then** the log shows no startup errors, `GET /` returns HTTP 200, and the health endpoint reports UP. *(AC-2, SPEC-APP-8)*
2. **Given** the same startup on main with JDK 17 run first, **When** the startup log, `GET /` status and health status are recorded, **Then** those values are stored in the PR body gate-evidence section as the baseline before any edit is made. *(SPEC-APP-8, SPEC-APP-10)*

---

### User Story 3 — All Java version pins moved to 21 in one atomic change (Priority: P3)

A named person (swapnilaswale83215) authors edits to both CI workflows, the Gitpod image Dockerfile (`.devcontainer/Dockerfile`), `.coderig/harness.yaml` and its source `.coderig/sensors.json`, changing only the Java version pin in each file. These edits are committed in the same pull request as the build-file changes so that no workflow or sensor ever runs on a mismatched Java version.

**Why this priority**: If any pin is left on Java 17 while `java.version` is 21, the Maven Enforcer rule fails that workflow. Moving all pins together in one pull request is a hard constraint, not an optional improvement.

**Independent Test**: After the pull request is merged, run the anchored pin check against the build files, both workflows, the Dockerfile, `harness.yaml` and `sensors.json`. Finding zero matches of every Java 17 pattern and the expected count of every Java 21 pattern constitutes a complete, independently verifiable result.

**Acceptance Scenarios**:

1. **Given** the change branch with `maven-build.yml`, `gradle-build.yml` and `.devcontainer/Dockerfile` moved to Java 21 in the same commit as the build files, **When** the maven-build and gradle-build workflows run on the branch and the Gitpod image is built from `.devcontainer/Dockerfile`, **Then** maven-build passes on JDK 21 with its enforcer step passing, gradle-build runs on JDK 21 with its result reported, and `./mvnw -v` inside the Gitpod image reports Java 21. *(AC-11, SPEC-APP-17)*
2. **Given** the harness source `.coderig/sensors.json` has been updated in the Launchpad harness editor for proj-d9b4cf and `coderig connect` has been run, **When** `harness.yaml` and `sensors.json` are read and a second regeneration is run in a scratch copy, **Then** the tests and format sensor images use Java 21 in both files, regeneration leaves zero Java 17 pins, and the tests sensor runs the build green on the Java 21 image. *(AC-12, SPEC-APP-17)*
3. **Given** the final change on the change branch, **When** the diff against main is listed by path, **Then** only the allowed paths are modified: `pom.xml`, `build.gradle`, `.github/workflows/maven-build.yml`, `.github/workflows/gradle-build.yml`, `.devcontainer/Dockerfile`, `.coderig/harness.yaml`, `.coderig/sensors.json`, and any source or test files that failed to compile or test after the upgrade. In the workflow, Dockerfile and harness files the only change is the Java version pin. `README.md` is not in the diff (explicit exception). *(AC-10, SPEC-APP-2)*

---

### User Story 4 — Existing functional behaviour is preserved on Java 21 (Priority: P4)

A tester or developer runs the existing test suite and a set of named verification checks (VR-1 to VR-6) on JDK 21 and confirms that every must-not-break behaviour — pet create and edit, owner register/edit/search, visit booking, vet list and cache, persistence profiles, AOT hints and i18n bundles — produces the same outcome as on the JDK 17 baseline.

**Why this priority**: The upgrade must not change any observable behaviour. This story is the regression gate. It is lower priority than the build succeeding because it cannot be evaluated until the build is green.

**Independent Test**: Run the full test suite on JDK 21 and compare pass, fail and skipped counts against the baseline. Run VR-1 to VR-6 on main with JDK 17 and on the branch with JDK 21 and compare outputs. Zero failures and matching counts constitute a complete, independently verifiable result.

**Acceptance Scenarios**:

1. **Given** an owner with a pet named "Leo" exists, **When** a second pet named "Leo" is submitted for the same owner on the pet create form, **Then** the form is shown again with the duplicate-name error and no new pet is saved; the owner still has exactly one pet named "Leo". *(AC-4, SPEC-APP-3)*
2. **Given** an owner exists and the pet create form is open, **When** a pet is submitted with a birth date one day in the future, **Then** the form is shown again with the birth-date error and no pet is saved; submitting a valid pet then saves it and redirects to the owner page, and editing it saves the change. *(AC-5, SPEC-APP-3, SPEC-APP-4)*
3. **Given** the seed data on JDK 21, **When** an owner is registered with a valid telephone number, then searched by last name, edited, and a visit is booked for one of their pets, **Then** each step returns its normal page (200, or a redirect to the owner page after saving), the owner and visit appear on the owner page, and an invalid telephone number is rejected as before. *(AC-7, SPEC-APP-6, SPEC-APP-14)*
4. **Given** the seed data on JDK 21, **When** the vet list page and the JSON vet resource are requested twice in a row, **Then** both return HTTP 200 with the same vets and specialties as before the change, and the second read is served from the cache. *(AC-8, SPEC-APP-7)*
5. **Given** a CI runner with Docker available, **When** `MySqlIntegrationTests` and `PostgresIntegrationTests` run on JDK 21 with the Maven wrapper, **Then** both pass with zero failures. If Docker is not available, both are reported separately as not run with the reason and are not counted as passed; KPI-3 is then read on the remaining tests and the gap is shown at the gate. *(AC-9, SPEC-APP-9)*
6. **Given** the final `pom.xml` and `build.gradle` and the Spring Boot target 4.1.0 confirmed by the product owner, **When** the Spring Boot version declared in the `pom.xml` parent or BOM is compared with the target version and the vendor's support calendar is checked on the same day, **Then** the declared version is 4.1.0 in both files and its open-source support window has not ended. *(AC-6, SPEC-APP-11)*

---

### Edge Cases

- What happens when Docker is unavailable on the CI runner? `MySqlIntegrationTests` and `PostgresIntegrationTests` are detected as skipped, listed as "not run (no Docker)" with the reason, and never counted as passed. KPI-3 is evaluated on the remaining tests and the gap is shown at the gate. *(SPEC-APP-9, AC-9)*
- What happens if a Java source file fails to compile on JDK 21? The file is edited only to fix the proven compilation failure, with the failing compiler output cited. No other change is made to that file. *(ADR-APP-1, SPEC-APP-2)*
- What happens if a plugin version is incompatible with JDK 21? The plugin is bumped only when a failure on JDK 21 proves it is necessary. The bump is taken to a named person. *(ADR-APP-6)*
- What happens if `.coderig/harness.yaml` is regenerated from the Launchpad harness editor after a hand edit? The sensor images are changed first in the Launchpad harness editor for proj-d9b4cf. A regeneration in a scratch copy is run to confirm it leaves zero Java 17 pins in both files before the change is committed. Neither file is hand-edited. *(SPEC-APP-17, AC-12)*
- What happens if the Spring Boot 4.1 open-source support end date has already passed at the time of the check? The spec fails KPI-4 and a new Boot-upgrade track is added as a separate design decision. The version is not patched silently. *(ADR-APP-7, SPEC-APP-11)*
- What happens if the rollback is needed after the first write on the Java 21 build? A git revert of the merge commit is applied, the Launchpad editor image for proj-d9b4cf is set back to 17, the harness is regenerated, and the reverted commit is built on JDK 17 and redeployed. No data is undone because the JDK does not change the schema or stored data. *(ADR-APP-4, SPEC-APP-12)*
- What happens if a visit is submitted with today's date? The form is shown again with `typeMismatch.visitDate` on `date` and no visit is saved. A visit dated tomorrow is accepted. *(SPEC-APP-14)*
- What happens if the coderig-gate blocked-scope gate rejects the person-authored pull request because it touches `.github/workflows/**`? The gate result is recorded in the PR body. The person resolves the gate finding before merge. *(SPEC-APP-2, ADR-APP-9)*
- What happens if the `mcr.microsoft.com/vscode/devcontainers/java:1-21-bullseye` image tag is missing when the author inspects it? The author records the manifest result in the PR body and the approver chooses the fallback option before the Dockerfile is edited. *(ADR-APP-3, SPEC-APP-17)*
- What happens if an anchored pin is found in a file that is not on the SPEC-APP-2 allow-list? The finding is escalated to the approver as a scope question and is not edited silently. *(SPEC-APP-17, ADR-APP-9)*
- What happens if a Java 17 pattern matches zero times on main during the positive control run? The pattern is corrected before use so that no check is vacuous. Work does not proceed until every pattern matches its expected count on main. *(SPEC-APP-17)*

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001** *(SPEC-APP-1)*: The Maven build MUST declare Java 21 through the single property `<java.version>` in `pom.xml`. No other Java-level setting is changed. `./mvnw clean verify` MUST exit with code 0 on JDK 21 with no other JDK on the path, and the build log MUST show the Maven Enforcer rule passed at Java 21 and build-info reporting `java.source` and `java.target` as 21.
- **FR-002** *(SPEC-APP-13)*: The Gradle build MUST declare `JavaLanguageVersion.of(21)` as the toolchain in `build.gradle`. `./gradlew build` MUST be run on JDK 21 via the gradle-build workflow and its result MUST be recorded in the PR body gate-evidence section. The Gradle build result is reported but does NOT gate the merge.
- **FR-003** *(SPEC-APP-17)*: Every Java 17 pin MUST be moved to its Java 21 equivalent in the same pull request. The pins are: `<java.version>17</java.version>` in `pom.xml`; `JavaLanguageVersion.of(17)` in `build.gradle`; the matrix line `java: [ '17' ]` in both workflow files; `ARG VARIANT=17-` and `ARG JAVA_VERSION=17.` in `.devcontainer/Dockerfile`; and `maven:3.9-eclipse-temurin-17` in `.coderig/harness.yaml` and `.coderig/sensors.json`. An anchored pin check run after the change MUST find zero matches of every Java 17 pattern and the expected count of every Java 21 pattern across those files. Each pattern MUST be confirmed to match its expected count on main before work starts (positive control); a pattern that matches nothing on main is corrected before use.
- **FR-004** *(SPEC-APP-2, ADR-APP-9)*: The diff against main MUST contain only the following entries: `pom.xml`; `build.gradle`; `.github/workflows/maven-build.yml`; `.github/workflows/gradle-build.yml`; `.devcontainer/Dockerfile`; `.coderig/harness.yaml`; `.coderig/sensors.json`; and any source or test files that failed to compile or test after the upgrade. `README.md` is an explicit exception and MUST NOT appear in the diff. In the workflow, Dockerfile and harness files the ONLY changed content MUST be the Java version pin. No distribution change, no new job, step, property or comment is permitted in those files. `coderig-gate.yml`, `deploy-and-test-cluster.yml`, `k8s/`, `docker-compose.yml`, `.devcontainer/devcontainer.json` and `.gitpod.yml` MUST NOT appear in the diff.
- **FR-005** *(SPEC-APP-2, ADR-APP-9)*: A named person (swapnilaswale83215) MUST author the edits to `.github/workflows/maven-build.yml`, `.github/workflows/gradle-build.yml`, `.devcontainer/Dockerfile`, `pom.xml` and `build.gradle`. The two `.coderig` files MUST be the regenerated output of `coderig connect` run by a person with editor rights after the sensor images are updated in the Launchpad harness editor for proj-d9b4cf; they MUST NOT be hand-edited. Agents MUST NOT edit any of these files.
- **FR-006** *(SPEC-APP-3, ADR-APP-5)*: `PetController` MUST continue to reject a duplicate pet name per owner (error code `duplicate` on `name`) and a birth date after today (`typeMismatch.birthDate` on `birthDate`) on both the create and edit forms. A valid pet create MUST save the pet and redirect to the owner page with flash message "New Pet has been Added". A `unique_owner_pet_name` constraint violation on save MUST also produce the duplicate error. `PetController.java` MUST NOT be edited unless a JDK 21 compile or test failure is cited.
- **FR-007** *(SPEC-APP-4, ADR-APP-5)*: `PetController` MUST continue to allow a valid pet edit, saving the change and redirecting to the owner page with flash message "Pet details has been edited". Renaming a pet to another pet's name MUST give the `duplicate` error; keeping the pet's own name MUST raise no error. A future birth date on edit MUST give `typeMismatch.birthDate` and save nothing. `PetController.java` MUST NOT be edited unless a JDK 21 failure is cited.
- **FR-008** *(SPEC-APP-5)*: `PetValidator` MUST continue to enforce: name required and at most 30 characters; type required for a new pet; birth date required. `PetValidator.java` MUST NOT appear in the diff.
- **FR-009** *(SPEC-APP-6)*: Owner register, edit and search by last name, including telephone validation, MUST behave identically to the JDK 17 baseline on JDK 21. An invalid telephone number MUST be rejected with the same field error as on the baseline. `OwnerController.java` and `Owner.java` MUST NOT appear in the diff unless a JDK 21 failure is cited.
- **FR-010** *(SPEC-APP-14)*: Visit booking for a pet MUST behave identically to the JDK 17 baseline on JDK 21. A visit dated today MUST be rejected with `typeMismatch.visitDate` on `date`; a visit dated tomorrow MUST be accepted and appear on the owner page with flash "Your visit has been booked". `VisitController.java` and `Visit.java` MUST NOT appear in the diff unless a JDK 21 failure is cited.
- **FR-011** *(SPEC-APP-7)*: The vet list page and the JSON vet resource MUST return HTTP 200 with the same vets and specialties as the JDK 17 baseline. The second read of the vet list MUST be served from the `vets` JCache with no SQL select on the second call. `CacheConfiguration.java` and `VetController.java` MUST NOT appear in the diff unless a JDK 21 failure is cited.
- **FR-012** *(SPEC-APP-9)*: The H2 (default), MySQL and PostgreSQL persistence profiles MUST work on JDK 21 with no schema or data change. `MySqlIntegrationTests` and `PostgresIntegrationTests` MUST pass on a Docker-enabled runner. Without Docker, both classes MUST be detected as skipped, listed as "not run (no Docker)" and never counted as passed. No file under `db/*` and no entity or repository MUST appear in the diff.
- **FR-013** *(SPEC-APP-15)*: `PetClinicRuntimeHints` MUST continue to register `db/*`, `messages/*` and serialization hints for `BaseEntity`, `Person` and `Vet`. AOT processing (`./mvnw -B spring-boot:process-aot`) MUST exit 0 on JDK 21 and the result MUST be recorded in the PR body gate-evidence section alongside the JDK 17 baseline result. `PetClinicRuntimeHints.java` MUST NOT appear in the diff unless a JDK 21 failure is cited. The native image build is checked only if the platform track confirms a pipeline uses it; otherwise it is reported as "not run" and never as passed.
- **FR-014** *(SPEC-APP-16)*: The `messages/*` bundles MUST stay in sync. `I18nPropertiesSyncTest` MUST pass on JDK 21 and MUST NOT be skipped. No file under `src/main/resources/messages/` MUST appear in the diff.
- **FR-015** *(SPEC-APP-10)*: A baseline record MUST be captured on main with JDK 17 before any edit is made, from a local main pulled up to `origin/main` (sha recorded) on a Docker-enabled host. It MUST include: the JDK version, the Spring Boot version, the commit id, the exit code of `./mvnw clean verify`, the pass/fail/skipped test counts, the list of skipped container tests (`MySqlIntegrationTests` and `PostgresIntegrationTests`), the AOT check result, and the startup baseline (startup log, `GET /` status, health status). It MUST be stored in the PR body gate-evidence section.
- **FR-016** *(SPEC-APP-11, ADR-APP-7)*: The Spring Boot version declared in `pom.xml` and `build.gradle` MUST equal 4.1.0. A person MUST record the vendor's open-source support end date for the Spring Boot 4.1 line and confirm it is later than the date of the check. This check MUST be repeated on the merge commit to main. If the end date has passed, the spec fails KPI-4 and a new Boot-upgrade track is raised; the version is not patched silently.
- **FR-017** *(SPEC-APP-12, ADR-APP-3)*: All Java pin changes MUST be delivered in one pull request containing `pom.xml`, `build.gradle`, both workflow files, `.devcontainer/Dockerfile`, `.coderig/harness.yaml` and `.coderig/sensors.json`. A revert of the merge commit, with the Launchpad editor image for proj-d9b4cf set back to 17 and the harness regenerated, MUST produce a green Java 17 build and green sensors with no data restore required.
- **FR-018** *(ADR-APP-6)*: Plugin versions (checkstyle 12.3.1, spring-javaformat 0.0.47, jacoco 0.8.15, GraalVM native plugin, cyclonedx, git-commit-id) MUST NOT be changed unless a failure on JDK 21 proves it is necessary. Any new dependency MUST be taken to a named person. The `adopt` distribution line in the CI workflows MUST NOT be changed.
- **FR-019** *(ADR-APP-10)*: The six named verification checks VR-1 to VR-6 MUST be run on main with JDK 17 and on the change branch with JDK 21, and their outputs compared. The results MUST be recorded in the PR body gate-evidence section. No new test file MUST appear in the committed diff.
- **FR-020** *(SPEC-APP-17, ADR-APP-3)*: Before editing `.devcontainer/Dockerfile`, the author MUST run `docker manifest inspect mcr.microsoft.com/vscode/devcontainers/java:1-21-bullseye` and record the result in the PR body. If the tag is missing, the approver chooses the fallback option before the Dockerfile is edited. The anchored pin check MUST be run on main before any edit as a positive control, confirming each Java 17 pattern matches its expected count; a pattern that matches nothing on main is flagged before work proceeds.

### Key Entities

- **Java version pin**: The single value `<java.version>` in `pom.xml` (and the equivalent `JavaLanguageVersion.of(21)` toolchain declaration in `build.gradle`) that controls the compiler source/target level, the Maven Enforcer minimum and the build-info output. Changing this one value is the primary act of the upgrade.
- **Spring Boot parent BOM**: The `spring-boot-starter-parent` version declared in `pom.xml` and `build.gradle`. Already set to 4.1.0 on main. Controls all managed dependency versions. Not changed by this upgrade.
- **CI workflow Java matrix entry**: The `java: [ '17' ]` matrix line in `.github/workflows/maven-build.yml` and `.github/workflows/gradle-build.yml`. Must match `java.version` in `pom.xml` to satisfy the Maven Enforcer rule. The `java-version: ${{matrix.java}}` line that reads the matrix value is not changed. Authored by a named person; agents do not edit these files.
- **Gitpod image pin**: The `VARIANT` argument (line 2), the tag prefix on line 3, and the `JAVA_VERSION` argument (line 9) in `.devcontainer/Dockerfile` (the Gitpod image). Controls the Java version available to developers and in Gitpod environments. Authored by a named person.
- **Harness sensor image tag**: The Java version embedded in the `maven:3.9-eclipse-temurin-{version}` image tag in `.coderig/harness.yaml` and its source `.coderig/sensors.json`. Set in the Launchpad harness editor for proj-d9b4cf; both files are regenerated by `coderig connect` and not hand-edited.
- **Baseline record**: The pre-change snapshot of JDK version, Spring Boot version, commit id, build exit code, test counts (pass/fail/skipped), container-test skip list (`MySqlIntegrationTests` and `PostgresIntegrationTests`), AOT check result and startup metrics captured on main with JDK 17 from a pulled local main. Stored in the PR body gate-evidence section. Used to verify that the executed test count does not decrease and that no behaviour changes.
- **Anchored pin check**: A set of file-and-key-specific patterns (not a free-text search for "17") used to verify that every Java 17 pin has been replaced and every Java 21 pin is present with its expected count. Run on main as a positive control before work starts and on the change branch before merge. Each pattern must match its expected count on main; a zero-count pattern on main is corrected before use.
- **Verification check (VR-1 to VR-6)**: Scratch test classes run in the working tree but not committed. Each closes a gap in the existing test suite for a specific must-not-break behaviour. VR-1: pet edit with future birth date. VR-2: flash messages on valid create and edit. VR-3: duplicate-name check with real H2 persistence. VR-4: vet cache SQL log check. VR-5: visit listed on owner page. VR-6: visit-date rule boundary (today rejected, tomorrow accepted). Results are recorded as gate evidence by swapnilaswale83215.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **KPI-1**: The full Maven wrapper build (`./mvnw clean verify`: compile, test, package) exits with code 0 on JDK 21 (21.0.13.1 or any JDK 21 patch level). Target: exit code 0. Baseline: exit code of `./mvnw clean verify` on current main with its current JDK, recorded before work starts.
- **KPI-2**: The application starts cold on JDK 21 with the default H2 profile, `GET /` returns HTTP 200, and the actuator health endpoint reports UP, with zero startup errors in the log. Target: starts with 0 startup errors; `GET /` returns 200; health reports UP. Baseline: startup log, `GET /` status and health status from main on JDK 17, recorded before work starts.
- **KPI-3**: Failures plus errors in the surefire/failsafe reports on JDK 21 equal 0, and the executed test count is at least the baseline count. When Docker is available, `MySqlIntegrationTests` and `PostgresIntegrationTests` are included in the count. When Docker is not available, they are listed separately as not run and the gap is shown at the gate. Target: 0 failures and errors; executed count ≥ baseline (in the same Docker mode). Baseline: pass, fail and skipped counts from `./mvnw clean verify` on main with JDK 17, recorded before work starts on a Docker-enabled host.
- **KPI-4**: The Spring Boot version declared in `pom.xml` and `build.gradle` is 4.1.0 (already the case on main), and the vendor's open-source support end date for the Spring Boot 4.1 line is later than the date of the check. Target: declared version is 4.1.0 in both files; support end date is later than the check date. Baseline: 4.1.0 is already declared; the support end date is recorded from the vendor's calendar before work starts.

## Existing Behaviour That Must Stay Unchanged

The following behaviours are in force for this change. They may not be altered. Each is verified by the test suite, the named verification checks and the acceptance criteria listed above.

- **Pet create and edit flows**: `PetController` rejects a duplicate pet name per owner and a birth date after today on both create and edit. A valid create saves the pet and redirects to the owner page; a valid edit saves the change and redirects. A `unique_owner_pet_name` constraint violation on save also produces the duplicate error. *(SPEC-APP-3, SPEC-APP-4, AC-4, AC-5)*
- **Pet validation rules**: `PetValidator` enforces name required and at most 30 characters, type required for a new pet, and birth date required. *(SPEC-APP-5)*
- **Owner register, edit and search**: Owner register, edit and search by last name work as before, including telephone validation. An invalid telephone number is rejected with the same field error. *(SPEC-APP-6, AC-7)*
- **Visit booking**: A visit dated today is rejected with `typeMismatch.visitDate`; a visit dated tomorrow is accepted and appears on the owner page with flash "Your visit has been booked". *(SPEC-APP-14, AC-7)*
- **Vet list page, JSON resource and cache**: Both return HTTP 200 with the same vets and specialties. The second read is served from the `vets` JCache with no SQL select. *(SPEC-APP-7, AC-8)*
- **Profile-based persistence**: The H2 (default), MySQL and PostgreSQL profiles work with no schema or data change. No file under `db/*` and no entity or repository is changed. *(SPEC-APP-9, AC-9)*
- **Runtime hints and AOT**: `PetClinicRuntimeHints` registers `db/*`, `messages/*` and serialization hints for `BaseEntity`, `Person` and `Vet` unchanged. AOT processing exits 0 on JDK 21. *(SPEC-APP-15)*
- **i18n message bundles**: The `messages/*` bundles stay in sync and `I18nPropertiesSyncTest` passes without being skipped. *(SPEC-APP-16)*

## Assumptions

- Maven (`./mvnw`) is the authoritative build for KPI-1 to KPI-4. The Gradle build is kept compatible only where it already builds on main and is reported but does not gate.
- JDK 21.0.13.1 is the Java runtime available on the machine as stated by the product owner. Any JDK 21 patch level is acceptable for CI unless the product owner states otherwise.
- Spring Boot 4.1.0 is already declared in `pom.xml` and `build.gradle` on main. The upgrade is Java 17 to 21 only; the Spring Boot version is not changed by this work.
- The database-container tests (`MySqlIntegrationTests` and `PostgresIntegrationTests`) count toward "all tests pass" only when Docker is available on the CI runner. Without Docker they are reported separately as not run and are never counted as passed. The baseline and post-change runs are compared only in the same Docker mode.
- `SeedParityTests` is deleted on `origin/main` and is therefore not present in this change. The Docker-dependent tests are `MySqlIntegrationTests` and `PostgresIntegrationTests` only.
- The must-not-break items (pet create/edit, owner register/edit/search, visit booking, vet list and cache, persistence profiles, AOT hints, i18n bundles) remain in force unless the product owner explicitly removes them.
- All Java pins must move in one pull request. If the build moves to 21 while any workflow, Dockerfile or harness file stays on 17, that workflow or sensor will fail due to the Maven Enforcer rule.
- `.coderig/harness.yaml` and `.coderig/sensors.json` are generated by `coderig connect` from the Launchpad harness editor for proj-d9b4cf. The sensor images are changed in the editor, not by hand-editing the files. A regeneration in a scratch copy confirms zero Java 17 pins in both files before the change is committed.
- A named person (swapnilaswale83215) authors the edits to the guarded files (`.github/workflows/**`, `.devcontainer/Dockerfile`, `pom.xml`, `build.gradle`). Agents do not edit these files.
- No application source file is edited unless a JDK 21 compile or test failure is proven and cited. Each such edit is limited to fixing the proven failure.
- No schema, entity, repository or data file is changed. No data migration is required and no write-freeze window is needed.
- Rollback is a git revert of the merge commit, with the Launchpad editor image for proj-d9b4cf set back to 17 and the harness regenerated, followed by a rebuild and redeploy on JDK 17. No data restore is required before or after the first write on the Java 21 build.
- The native image build is checked only if the platform track confirms a pipeline uses it; otherwise it is reported as "not run" and never as passed.
- The `adopt` distribution line in the CI workflows is not changed. Only the Java version matrix value changes.
- `coderig-gate.yml`, `deploy-and-test-cluster.yml`, `k8s/`, `docker-compose.yml`, `.devcontainer/devcontainer.json` and `.gitpod.yml` are not touched.
- The platform track (ADR-PLAT and SPEC-PLAT identifiers) is not traced in this change spec. It is traced in the platform track of Architecture revision 8. References to `(SPEC-PLAT-n)` in this document are pointers to where the platform track defines the mechanism, not trace links.
- `README.md` is an explicit exception to the allow-list. Line 17 ("Java 17 or later is required for the build") and line 111 ("Java 17 or newer") become stale once the enforcer requires JDK 21 and need a follow-up edit outside this change. `README.md` does not appear in the diff and its two Java 17 matches are not counted as stray pins in the anchored pin check.
- The Gitpod Dockerfile tag prefix changes from `0-` to `1-` (line 3) to form the valid `1-21-bullseye` tag, in addition to the `VARIANT` and `JAVA_VERSION` argument changes. The author confirms the tag exists via `docker manifest inspect` before editing the file.
- Local main is pulled to `origin/main` (`git pull --ff-only origin main`, sha recorded) before the baseline is captured and before the anchored pin positive control is run, so the index is not stale.
- The `JAVA_VERSION` sdkman identifier on line 9 of `.devcontainer/Dockerfile` is the value verified and recorded by the PR author (swapnilaswale83215) by running `sdk list java` inside the built Gitpod image and selecting the JDK 21 identifier per ADR-PLAT-5. This spec does not name a specific identifier and uses whatever that step records.
