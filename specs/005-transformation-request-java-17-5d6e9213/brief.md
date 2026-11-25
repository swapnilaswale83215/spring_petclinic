# Brief: transformation-request-java-17-5d6e9213

**Goal**: Run the application and its build on a supported JDK: move the Java level from 17 to 21. No feature work. Spring Boot stays at the declared 4.1.0.

**Why now**: Java 17 is past (or near) the end of the support window we rely on. The build, CI and harness still pin 17 while Boot is already on 4.1.0. We need a current, supported runtime without a product change.

## Scope

In scope:

- pom.xml: <java.version> 17 to 21 (Maven enforcer and build-info follow it)
- build.gradle: toolchain JavaLanguageVersion 17 to 21
- The Java pin in both CI workflows (maven-build.yml and gradle-build.yml)
- .devcontainer/Dockerfile, Java pin move only: ARG VARIANT becomes a 21 bullseye value (21-bullseye); the FROM tag prefix changes from 0- to 1- (0-21-bullseye does not exist, 1-21-bullseye does); ARG JAVA_VERSION becomes a JDK 21 sdkman identifier verified with `sdk list java` in the 1-21-bullseye base image and recorded before editing. No other edit to that file.
- Regenerated harness files (.coderig sensor images, set in the Launchpad editor, not by hand)
- Verifying on JDK 21 that ./mvnw clean verify passes and the Gradle build runs and is reported
- Confirming Spring Boot 4.1.0 is still in open-source support on the check date

Out of scope:

- Any Spring Boot version change (stays at 4.1.0)
- New features, behaviour change, new committed tests
- Planned source, schema, entity, repository, dependency or plugin bumps (only a proven JDK 21 failure may force one, one file at a time with evidence cited)
- Distribution change in CI (adopt stays)
- coderig-gate.yml, deploy/k8s/compose, .devcontainer/devcontainer.json (does not use the Dockerfile; already pins Java through the 21-oracle feature) and .gitpod.yml (already points at ./.devcontainer/Dockerfile)
- Schema or data changes on the H2, MySQL and PostgreSQL profiles
- Making the Gradle build a gate
- Any edit to .devcontainer/Dockerfile other than the Java pin move (VARIANT, FROM tag prefix, JAVA_VERSION); .gitpod.yml and .devcontainer/devcontainer.json stay untouched
- A silent Spring Boot bump: if Boot 4.1.0 support has ended on or before the check date, a separate Boot-upgrade track is raised
- README: stale 'Java 17' lines there are a follow-up doc edit, not stray Java pins of this change

## Must not break

- Pet create and edit, including the duplicate-name and future-birth-date checks
- Owner register, edit and search, including telephone validation
- Visit booking, including the visit-date rule
- Vet list, JSON resource and the vets cache
- H2, MySQL and PostgreSQL profiles with no schema or data change
- Startup on the default H2 profile (GET / returns 200, health UP)
- Existing tests: 0 failures/errors, executed count not below the JDK 17 baseline
- AOT/native runtime hints stay unchanged (PetClinicRuntimeHints registers BaseEntity, Person and Vet)
- Owner records (name, address, telephone) are not changed or exposed differently

## Constraints

- [scope] Touch only pom.xml, build.gradle, the two CI workflow Java pins, the regenerated harness files (sensor images set in the Launchpad editor, not by hand), and in .devcontainer/Dockerfile only the Java pin move: ARG VARIANT to a 21 bullseye value, FROM tag prefix 0- to 1- (1-21-bullseye exists, 0-21-bullseye does not), ARG JAVA_VERSION to a JDK 21 sdkman identifier verified from `sdk list java` in the 1-21-bullseye base image and recorded before editing. No other edits to that file. .gitpod.yml and devcontainer.json stay untouched. README is out of scope.
- [technical] No planned source, schema, entity, repository, dependency or plugin bumps unless a proven JDK 21 failure forces one, one file at a time with evidence cited.
- [policy] No new features, no new committed tests, no behaviour change.
- [technical] No distribution change in CI (adopt stays).
- [scope] Leave alone coderig-gate.yml, deploy/k8s/compose, .devcontainer/devcontainer.json and .gitpod.yml.
- [policy] One atomic PR; no partial pin move that leaves main red.
- [technical] Spring Boot stays at the declared 4.1.0. The Boot 4.1.0 support check date is the date of the final gate evidence, recorded in the PR body and repeated on the merge commit. If open-source support ends on or before it, KPI-6 fails and a separate Boot-upgrade track is raised; no silent Boot bump.
- [technical] Data stores unchanged: no schema or data change on the H2, MySQL and PostgreSQL profiles.
- [technical] Baseline capture and all KPI baselines run only after `git pull --ff-only origin main`, with the resulting sha recorded, on a Docker-capable host. The JDK 17 baseline and the JDK 21 verification of the MySQL and PostgreSQL criteria use the GitHub-hosted ubuntu-latest runner of maven-build.yml (preferred) or local Docker Desktop with the host name recorded in the gate evidence. Without Docker, MySqlIntegrationTests and PostgresIntegrationTests are 'not run (no Docker)' and never counted as passed.
- [scope] Baseline specs are not a blocker for agreeing the brief. The brief (goal, KPIs, must-not-break, constraints) is the traceability spine. Generating baseline specs is out of this change's scope unless a gate later requires it.

## Success criteria

### KPI-1

- Criterion: <java.version> is 21, and Maven enforcer and build-info report 21
- Metric: Java level reported by pom <java.version>, the enforcer rule and build-info, integer
- Target: 21 in all three
- Baseline: 17 in pom.xml <java.version>. Capture on main after `git pull --ff-only origin main` (sha recorded) before work starts.
- Window: Evidence from the build of the change PR, rechecked at the final gate

### KPI-2

- Criterion: ./mvnw clean verify exits 0 on JDK 21
- Metric: Exit code of ./mvnw clean verify on JDK 21, integer
- Target: 0
- Baseline: unknown. Capture ./mvnw clean verify on JDK 17 (exit code) after `git pull --ff-only origin main`, with the sha recorded, on a Docker-capable host, before work starts.
- Window: Every CI run of the change PR; the last run before merge counts

### KPI-3

- Criterion: Every Java pin (pom, Gradle toolchain, both CI workflows, Gitpod Dockerfile, harness sensor images) is 21 in one change
- Metric: Count of in-scope Java pins still at 17 after the change, count
- Target: 0 in-scope pins at 17, all moved in one PR (one commit to revert). In .devcontainer/Dockerfile: VARIANT is the 21 bullseye value, the FROM tag prefix is 1-, JAVA_VERSION is the sdkman JDK 21 identifier recorded from `sdk list java`. No other line of that file changes; .gitpod.yml and devcontainer.json are unchanged.
- Baseline: All pins are at 17: pom.xml, build.gradle, 2 CI workflows, .devcontainer/Dockerfile (ARG VARIANT=17-bullseye, ARG JAVA_VERSION=17.0.7-ms) and the harness images. Re-run as an anchored pin check on main after `git pull --ff-only origin main` (sha recorded).
- Window: Once on main before work starts (positive control), once on the change branch before merge

### KPI-4

- Criterion: Existing tests: 0 failures/errors; executed count not below the JDK 17 baseline
- Metric: Surefire/Failsafe failures plus errors (count) and executed tests (count)
- Target: 0 failures and 0 errors; executed count >= the JDK 17 baseline count. MySqlIntegrationTests and PostgresIntegrationTests are counted as passed only when they ran with Docker; without Docker they are reported as 'not run (no Docker)', never as passed, and the MySQL/PostgreSQL criteria are not claimed as verified.
- Baseline: unknown. After `git pull --ff-only origin main` (sha recorded), capture executed/passed/failed/skipped counts on main with JDK 17 on a Docker-capable host (preferred: ubuntu-latest runner of maven-build.yml; local Docker Desktop acceptable with the host recorded), so MySqlIntegrationTests and PostgresIntegrationTests actually run.
- Window: The JDK 17 baseline run before work starts and the JDK 21 verification in the final CI run of the change PR, both on a Docker-capable environment

### KPI-5

- Criterion: Gradle build runs on 21 and is reported; it does not gate
- Metric: Result of the Gradle build on JDK 21 (exit code), reported in the PR; share of merges blocked by it
- Target: Result reported in the PR evidence (exit code recorded); 0 merges blocked by it
- Baseline: unknown. Capture the Gradle build result on main with JDK 17 after `git pull --ff-only origin main` (sha recorded) before work starts.
- Window: Final CI run of the change PR

### KPI-6

- Criterion: Boot remains 4.1.0 and is still in open-source support on the check date
- Metric: Declared Spring Boot version (string) and its open-source support end date compared with the check date (date)
- Target: Version = 4.1.0 and the open-source support end date is after the check date. If it is on or before the check date, KPI-6 fails and a separate Boot-upgrade track is raised; Boot is not bumped silently.
- Baseline: Boot 4.1.0 is declared on main (graph and earlier spec). Support end date: unknown, to be read from the Spring Boot support policy page on the check date.
- Window: The check date is the date of the final gate evidence, recorded in the PR body and repeated on the merge commit to main

### KPI-7

- Criterion: Startup on the default H2 profile (GET / 200, health UP) on JDK 21
- Metric: HTTP status of GET / and the actuator health status
- Target: 200 and UP
- Baseline: unknown. Capture on main with JDK 17 (startup status) after `git pull --ff-only origin main` (sha recorded), on a Docker-capable host, before work starts.
- Window: One cold start on the change branch before merge

### KPI-8

- Criterion: Rollback is a revert of that one change, then rebuild and redeploy on 17, with no data restore
- Metric: Reverts needed to return to JDK 17, and data restores needed, count
- Target: 1 revert, 0 data restores, and the build on JDK 17 passes after the revert
- Baseline: unknown. The rollback has not been rehearsed.
- Window: Rehearsed once on a branch before merge


## Acceptance criteria

### AC-1

- Given: The change branch with the pom.xml edit applied
- When: ./mvnw validate runs on JDK 21
- Then: <java.version> reads 21, the enforcer passes and build-info.properties reports 21

### AC-2

- Given: The change branch checked out on a JDK 21 runtime
- When: ./mvnw clean verify runs
- Then: The exit code is 0

### AC-3

- Given: An anchored pin check that passes on main as a positive control, finding all pins at 17
- When: The same check runs on the change branch
- Then: 0 in-scope pins read 17. In .devcontainer/Dockerfile, ARG VARIANT is the 21 bullseye value, the FROM tag prefix is 1-, and ARG JAVA_VERSION is the sdkman JDK 21 identifier recorded from `sdk list java` in the 1-21-bullseye image. The PR diff touches only the allowed files (pom.xml, build.gradle, 2 workflows, the Dockerfile Java pin lines, the regenerated harness files). .gitpod.yml, devcontainer.json and README are unchanged.

### AC-4

- Given: The JDK 17 baseline test counts recorded from main after `git pull --ff-only origin main` (sha recorded) on a Docker-capable host (ubuntu-latest runner of maven-build.yml preferred; local Docker Desktop acceptable with the host recorded)
- When: The full test suite runs on JDK 21 on a Docker-capable environment
- Then: 0 failures, 0 errors, and executed tests >= the baseline count, with MySqlIntegrationTests and PostgresIntegrationTests executed. Without Docker they are reported as 'not run (no Docker)' and not counted as passed.

### AC-5

- Given: The change branch on a CI runner with JDK 21
- When: gradle-build.yml runs
- Then: The Gradle build result is reported in the PR evidence, and a failure of it does not block the merge

### AC-6

- Given: The change branch and the final gate evidence date recorded in the PR body
- When: The Spring Boot parent version and the support policy are read on the check date, and read again on the merge commit
- Then: The version is 4.1.0 and its open-source support end date is after the check date, with the date repeated on the merge commit. If the end date is on or before the check date, KPI-6 fails and a separate Boot-upgrade track is raised, with no silent bump.

### AC-7

- Given: The application started on JDK 21 with the default H2 profile
- When: GET / and GET /actuator/health are called
- Then: GET / returns 200 and health status is UP

### AC-8

- Given: The merged change on a branch and the one change reverted
- When: The build and deploy run on JDK 17
- Then: The build passes, 0 data restores are needed, and the revert is a single commit or merge

### AC-9

- Given: An owner with a pet named 'Rex' exists, running on JDK 21
- When: A second pet named 'Rex' is submitted for the same owner, and a pet with a future birth date is submitted
- Then: Both submissions are rejected with validation errors (duplicate name, birth date in the future); a valid pet is created and an existing pet is edited

### AC-10

- Given: The application started with the MySQL profile and with the PostgreSQL profile on JDK 21, on a Docker-capable environment (ubuntu-latest runner of maven-build.yml preferred; local Docker Desktop acceptable with the host recorded)
- When: The profile's integration tests (vet list read) run
- Then: Each passes with no schema or data change in the diff (no migration or schema file touched). If Docker is unavailable, MySqlIntegrationTests and PostgresIntegrationTests are reported as 'not run (no Docker)', never as passed, and this criterion is not claimed as verified.

### AC-11

- Given: An owner is registered on JDK 21
- When: An owner is registered or edited with an invalid telephone number, then searched by last name
- Then: The invalid telephone is rejected; a valid owner is saved and returned by the last-name search

### AC-12

- Given: An existing pet on JDK 21
- When: A visit dated today and a visit dated tomorrow are submitted
- Then: The visit dated today is rejected and the visit dated tomorrow is accepted and listed on the owner page (this matches the boundary in the earlier spec, to be confirmed against current code)

### AC-13

- Given: The application started on JDK 21 with vet data loaded
- When: The vet list page and the JSON vets resource are requested twice
- Then: Both return the vets with their specialties, and the second request is served from the cache (no second vet query in the SQL log)

### AC-14

- Given: The change branch with PetClinicRuntimeHints.java unmodified
- When: The PR diff is compared with main
- Then: PetClinicRuntimeHints.java is not in the diff and still registers BaseEntity, Person and Vet; the AOT/native hints test or processing step, where one exists, passes

### AC-15

- Given: An owner with name, address and telephone exists on JDK 21
- When: The owner is read on the owner page and in the search results, and the PR diff is checked
- Then: The same name, address and telephone values appear as on the JDK 17 baseline, no new field or endpoint exposes them, and no owner entity, repository or template file is in the diff
