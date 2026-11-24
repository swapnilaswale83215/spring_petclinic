# Design: java-17-to-21-upgrade-5d8272c2

## Application track

Rework for DEF-1 and DEF-2 only. The real-database duplicate-name test now lives in a new @SpringBootTest class (PetCreateEditRealDbTests) that runs on H2 under ./mvnw test. The same message check is added to the existing MySQL and PostgreSQL Testcontainers classes. PetControllerTests, PetController and PetValidator stay untouched. Baseline capture now has a named owner and comes first: B0 is captured before any change, B after the new tests is the KPI-2 gate value, and the platform track must cite this order. Everything else, including Maven as the gate, is unchanged.

| Component | Change | Responsibility |
|---|---|---|
| pom.xml (Maven build, acceptance gate) | changed | Source of truth for versions. Parent goes 4.1.0 to the pinned 4.2.x, java.version goes 17 to 21, and maven-enforcer follows ${java.version}. Boot manages every library version. Only the webjars, Checkstyle, spring-javaformat, nohttp and JaCoCo pins remain explicit. |
| build.gradle (Gradle build, sync mirror) | changed | Mirrors the pom. The org.springframework.boot plugin takes the same 4.2.x string and the toolchain languageVersion becomes 21. Dependency-management 1.1.7 and the GraalVM native plugin 1.1.2 are bumped only if the build breaks. It is not the gate. |
| Version-sync check | new | A small script or CI step that reads the Java and Boot versions from both build files and fails on any mismatch (KPI-3, AC-2). It adds no test to the Maven suite, so the KPI-2 count is unaffected. |
| PetController / PetValidator / owner and visit code | unchanged | Create and edit pet, duplicate-name rejection, future-birth-date rejection and validator rules stay as they are. Only the framework underneath moves. |
| PetCreateEditRealDbTests (new test class) | new | A @SpringBootTest with MockMvc on the default H2 profile, in src/test/java/org/springframework/samples/petclinic/owner. It needs no Docker and runs under ./mvnw test. It uses real repositories and a real schema. It proves that a real unique_owner_pet_name violation produces the 'duplicate' error on create and edit, and that edit rejects a future birth date. PetControllerTests, which is a @WebMvcTest with mocked repositories, is NOT changed and keeps its two hand-written DataIntegrityViolation tests. The same message check is added as one method each to MySqlIntegrationTests and PostgresIntegrationTests, run where Docker exists. |
| PetClinicRuntimeHints (AOT hints) | unchanged | Keeps the resource patterns (db/*, db/*/*, messages/*, mysql-default-conf) and the serialization hints for BaseEntity, Person and Vet. It is edited only if AOT processing or the hints tests fail on 4.2.x. |
| CacheConfiguration (vets cache) | unchanged | Creates the vets cache through JCacheManagerCustomizer. It imports org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer, a package that may move in 4.2. It is fixed minimally only if compilation fails. |
| application.properties (Actuator exposure) | unchanged | management.endpoints.web.exposure.include=* is kept so behaviour is preserved. Health and metrics are verified on 4.2.x. |
| Test suite and integration tests | unchanged | All current tests run. None are removed, disabled or skipped. The MySQL and PostgreSQL Testcontainers tests run where a container runtime exists and are reported separately if not. |

**Specifications**

### SPEC-APP-1

- Module: pom.xml
- Statement: The Maven build targets Java 21 and the pinned Boot 4.2.x parent, and ./mvnw -B verify succeeds on a Java 21 JDK.
- Accepted when: spring-boot-starter-parent version is the pinned 4.2.x and is not 4.1.x, java.version is 21 and maven-enforcer requires at least 21, ./mvnw -B verify exits 0 with 0 failures on JDK 21, Checkstyle, javaformat and nohttp validation pass without a rule change
- Traces to: [KPI-1](brief.md#kpi-1), [AC-1](brief.md#ac-1), [ADR-APP-1](design.md#adr-app-1), [ADR-APP-5](design.md#adr-app-5)

### SPEC-APP-2

- Module: build.gradle
- Statement: The Gradle build uses the same Java and Boot versions as the pom and builds on a Java 21 JDK, as a sync check only.
- Accepted when: org.springframework.boot plugin version equals the pom parent version, java toolchain languageVersion is 21, ./gradlew build exits 0 on JDK 21, Any added Gradle dependency or plugin bump is listed in the PR with its reason
- Traces to: [KPI-3](brief.md#kpi-3), [AC-13](brief.md#ac-13), [ADR-APP-2](design.md#adr-app-2)

### SPEC-APP-3

- Module: Version-sync check
- Statement: A check reads the Java version and the Boot version from pom.xml and build.gradle and fails on any difference.
- Accepted when: Returns 0 mismatches on the upgrade branch, Fails with a message naming the field when one file is changed alone, Runs without the Maven test suite and adds no tests to it
- Traces to: [KPI-3](brief.md#kpi-3), [AC-2](brief.md#ac-2), [ADR-APP-2](design.md#adr-app-2)

### SPEC-APP-4

- Module: Baseline capture
- Statement: Before any other change, the named owner captures B0 (build, start and test totals on Java 17 + Boot 4.1.0) as an artefact. After the new tests of SPEC-APP-5 are added on the same stack, the count B is captured. B is the KPI-2 gate value. The capture is the first step of the work order in ADR-APP-4.
- Accepted when: The PR description names the baseline owner (swapnilaswale83215, to be confirmed at approval) before any code commit, B0 is recorded from ./mvnw -B verify on Java 17 + Boot 4.1.0 before any other change. It holds run, failures, errors and skipped, read from Surefire XML only, with the MySQL and PostgreSQL class rows as separate lines, The artefact records the ./mvnw spring-boot:run start result and the /actuator/health status on the baseline stack, B is recorded after SPEC-APP-5 tests are added on Java 17 + Boot 4.1.0 and is at least B0 plus the added tests. AC-4 and KPI-2 compare with B, The order is B0, then new tests, then B, then the Boot 4.2 commit. The platform track cites this order (ADR-PLAT-3)
- Traces to: [KPI-1](brief.md#kpi-1), [KPI-2](brief.md#kpi-2), [AC-4](brief.md#ac-4), [ADR-APP-4](design.md#adr-app-4)

### SPEC-APP-5

- Module: PetCreateEditRealDbTests (src/test/java/.../owner) and the MySQL and PostgreSQL integration test classes
- Statement: Tests against a real database are added in a new @SpringBootTest class that runs on H2 under ./mvnw test. They show that Hibernate and Spring Data still produce a DataIntegrityViolationException whose message contains 'unique_owner_pet_name', and that PetController turns it into the 'duplicate' error on create and edit. They also show that edit rejects a future birth date. No existing test, PetController, PetValidator or PetControllerTests is changed.
- Accepted when: PetCreateEditRealDbTests is @SpringBootTest with MockMvc on the default H2 profile. It needs no Docker and runs under ./mvnw test. git diff shows PetControllerTests.java unchanged, Repository level: with two copies of the same owner loaded before either saves, the second saveAndFlush that adds a pet with the same name throws DataIntegrityViolationException, and getMessage().toLowerCase() contains 'unique_owner_pet_name'. This is the exact predicate of PetController.isDuplicatePetNameViolation. The test uses its own owner row and removes it in @AfterEach, Controller level, create and edit: OwnerRepository is a Spring spy that returns the stale owner copy from findById, so the in-memory check passes and the real flush hits the constraint. The POST returns HTTP 200 with a 'duplicate' field error on name, and one row with that name exists, Edit with a future birth date returns HTTP 200 with a typeMismatch.birthDate field error, and the stored pet is unchanged. If the spy cannot be applied to the repository, the controller-level criterion moves to the MySQL and PostgreSQL classes and the gap is recorded in the PR, The same message check is one added method each in MySqlIntegrationTests and PostgresIntegrationTests, run where Docker exists and reported as 'not run' otherwise. All new tests pass on Java 17 + Boot 4.1.0 before the bump and on Java 21 + Boot 4.2.x after it
- Traces to: Pet create and edit flows, [AC-5](brief.md#ac-5), [AC-6](brief.md#ac-6), [AC-7](brief.md#ac-7), [ADR-APP-3](design.md#adr-app-3), [ADR-APP-4](design.md#adr-app-4)

### SPEC-APP-6

- Module: PetController
- Statement: Pet create and edit behave as on the baseline: duplicate name is rejected, a future birth date is rejected, and a valid pet is saved and redirected. The code is unchanged.
- Accepted when: git diff shows no change to PetController.java, PetValidator.java, the pet templates or the messages files, Duplicate 'Leo' on create: 'duplicate' on name, HTTP 200, one pet only, Edit with own name redirects 3xx, with another pet's name gives 'duplicate', with a future date gives a birthDate error, Valid create redirects to the owner page and the pet appears in the list
- Traces to: Pet create and edit flows, [AC-5](brief.md#ac-5), [AC-6](brief.md#ac-6), [AC-7](brief.md#ac-7), [AC-8](brief.md#ac-8), [ADR-APP-3](design.md#adr-app-3)

### SPEC-APP-7

- Module: PetValidator
- Statement: PetValidator rules stay: name required, name length at most 30, birth date required, and type required on new pets.
- Accepted when: PetValidatorTests run unchanged and pass, No change to PetValidator.java
- Traces to: Pet validation rules in PetValidator, [AC-8](brief.md#ac-8), [ADR-APP-3](design.md#adr-app-3)

### SPEC-APP-8

- Module: Owner and visit flows
- Statement: Owner registration, edit, search by last name and detail, and visit booking and history, return the same status and visible result as the baseline on 4.2.x.
- Accepted when: OwnerControllerTests, OwnerTests, VisitControllerTests and ClinicServiceTests pass unchanged, Create, edit and find by last name for an owner, and a visit booking, give the baseline status and result, No owner name, address or telephone value appears in the startup log
- Traces to: Owner registration, edit, search by last name and detail flows, Visit scheduling for a pet and visit history, [AC-9](brief.md#ac-9)

### SPEC-APP-9

- Module: Vet directory and cache
- Statement: The vet page and /vets JSON return the 6 seeded vets with specialties, twice in a row, with the vets cache active.
- Accepted when: VetControllerTests and VetTests pass unchanged, Two requests of each return 6 vets with specialties as on the baseline, The 'vets' cache is created at startup with no error, If CacheConfiguration is changed, the change is only to compile against 4.2.x
- Traces to: Vet directory page, JSON resource and cached vet list, [AC-10](brief.md#ac-10), [ADR-APP-5](design.md#adr-app-5)

### SPEC-APP-10

- Module: Persistence profiles
- Statement: The H2 default profile starts and its tests pass on Java 21 + Boot 4.2.x. The MySQL and PostgreSQL profiles pass their integration tests where a container runtime exists. No schema or SQL file changes.
- Accepted when: PetClinicIntegrationTests, SeedParityTests and PetClinicConcurrencyTests pass, MySqlIntegrationTests and PostgresIntegrationTests pass where Docker is available, otherwise they are reported as 'not run' and not as passes, git diff shows no change under src/main/resources/db, Hibernate ddl-auto is confirmed as not altering the schema
- Traces to: Persistence on the H2 (default), MySQL and PostgreSQL profiles, [AC-11](brief.md#ac-11), [ADR-APP-7](design.md#adr-app-7)

### SPEC-APP-11

- Module: Startup and Actuator
- Statement: With the default H2 profile on Java 21, ./mvnw spring-boot:run starts with 0 errors, logs Java 21 and Boot 4.2.x, and serves /actuator/health with 200 and /actuator/metrics.
- Accepted when: Startup log has 0 ERROR lines and shows Java 21 and the pinned Boot version, GET /actuator/health returns 200, GET /actuator/metrics returns 200, The exposure setting include=* is unchanged
- Traces to: [KPI-1](brief.md#kpi-1), Actuator health and metrics endpoints, [AC-3](brief.md#ac-3), [ADR-APP-10](design.md#adr-app-10)

### SPEC-APP-12

- Module: AOT and runtime hints
- Statement: AOT processing completes on Java 21 + Boot 4.2.x and the hint-covered resources and types are still registered.
- Accepted when: AOT processing exits 0, Hints for db/*, db/*/*, messages/* and mysql-default-conf and the three serialization types are present in the generated hints, PetClinicRuntimeHints is unchanged or has a documented minimal fix
- Traces to: [KPI-1](brief.md#kpi-1), [ADR-APP-6](design.md#adr-app-6)

### SPEC-APP-13

- Module: Test suite
- Statement: The full Maven test run has 0 failures, executed tests at least B, and no test removed, disabled or newly skipped.
- Accepted when: ./mvnw test reports 0 failures and 0 errors on JDK 21, Executed count >= B from SPEC-APP-4, git diff shows no new @Disabled, no deleted test file and no deleted test method, I18nPropertiesSyncTest, WelcomeControllerTests, CrashController tests, ValidatorTests and PetTypeFormatterTests pass unchanged
- Traces to: [KPI-2](brief.md#kpi-2), [AC-4](brief.md#ac-4), [ADR-APP-9](design.md#adr-app-9)

### SPEC-APP-14

- Module: Platform interface
- Statement: CI workflows, devcontainer and Gitpod build the project on Java 21 using the versions in pom.xml, as set out in ADR-APP-8.
- Accepted when: Both workflows run Java 21 and the Maven job runs ./mvnw -B verify, The Dockerfile, devcontainer.json and .gitpod.yml all name Java 21, The Gradle workflow caches Gradle and runs ./gradlew build, All Maven CI jobs are green on the PR, and the Docker Compose and wrappers still work, No platform file holds a Boot version
- Traces to: [KPI-1](brief.md#kpi-1), [KPI-2](brief.md#kpi-2), [AC-12](brief.md#ac-12), [ADR-APP-8](design.md#adr-app-8)

### SPEC-APP-15

- Module: Rollback
- Statement: Reverting the upgrade PR returns a green build on Java 17 + Boot 4.1.0 with no data or schema step.
- Accepted when: A rehearsal revert on a branch gives ./mvnw -B verify exit 0 on JDK 17, The rollback needs no database step, The behaviour after a first write on the new build is documented as 'no schema difference'
- Traces to: [ADR-APP-7](design.md#adr-app-7), [KPI-1](brief.md#kpi-1)

**Decisions**

### ADR-APP-1

- Title: Target Java 21 and one pinned Spring Boot 4.2.x patch, identical in both builds
- Decision: Set java.version to 21 and the toolchain to 21. Set the Boot parent in the pom and the plugin in build.gradle to the same latest GA 4.2.x patch available at implementation time. Do not use a range or a snapshot or milestone. Record the chosen patch in the PR description.
- Rationale: The product owner chose 4.2.x. A single pinned string makes KPI-3 checkable with 0 mismatches. 4.1.x (latest patch 4.1.3) is not at end of support, so the upgrade is for currency, not an EOL emergency. The exact patch is not named here because it cannot be read from the repository.
- Alternatives: Stay on 4.1.3 and move only Java: rejected, it misses the agreed goal, Use a version range or a milestone: rejected, it is not reproducible

### ADR-APP-2

- Title: Maven is the authority and Gradle is mirrored, not aligned
- Decision: The pom decides acceptance. Both files change in the same commit. Run ./gradlew build on Java 21 as a sync check. Do not add the extra test starters that only the pom has (restclient, thymeleaf-test, validation-test, actuator-test, cache-test) to Gradle, unless the Gradle build fails without them. Record the gap in the PR.
- Rationale: The scope is build files plus code that breaks. Aligning the dependency sets would be a refactor beyond that. Gradle is not the gate, so its test-set difference does not affect KPI-2.
- Alternatives: Fully align Gradle test dependencies with the pom: rejected, scope creep and a different test count, Drop build.gradle: rejected, the brief keeps it in sync

### ADR-APP-3

- Title: Pet create and edit behaviour is frozen; framework changes are fixed in the build, never in the controller
- Decision: Do not edit PetController, PetValidator, the Pet templates, the messages or the schema. If a check fails on 4.2.x, fix the dependency or configuration. Only if no such fix exists, raise it as a blocking issue to the product owner. PR #144's statement that future birth dates are accepted is wrong and is not used for any test or code.
- Rationale: This is the must-not-break item. The checks rely on framework behaviour: the 'duplicate' error code, typeMismatch.birthDate on a future date, and the string match on 'unique_owner_pet_name' in the DataIntegrityViolationException message. Tests are the guard.
- Alternatives: Rewrite the unique-name check as a repository lookup that does not depend on the exception message: rejected, a behaviour change outside scope, Follow the PR #144 spec: rejected by the product owner

### ADR-APP-4

- Title: Baseline first (named owner), tests next, then a staged bump inside one PR
- Decision: Owner: swapnilaswale83215, the repository owner named in the run, owns baseline capture. This is a proposed assignment that the approver must confirm. Order of work, which the platform track must cite (ADR-PLAT-3): (0) the owner is recorded in the PR description. (1) FIRST, on Java 17 + Boot 4.1.0 and before any other change, run ./mvnw -B verify and ./mvnw spring-boot:run. Record B0 (run, failures, errors, skipped), the start result, /actuator/health status and the Surefire XML as the baseline artefact. The totals script reads target/surefire-reports only, because pom.xml configures no maven-failsafe-plugin. The MySQL and PostgreSQL totals are separate lines derived from the MySqlIntegrationTests and PostgresIntegrationTests rows in the Surefire XML. (2) On the same stack, add the new tests of SPEC-APP-5 and re-run. The executed count is then B, which is the KPI-2 gate value (B >= B0 + new tests). (3) Commit 1: Boot 4.2.x on Java 17, to isolate framework breaks. (4) Commit 2: Java 21 in both builds, the enforcer and the devcontainer interface. (5) Fix only what breaks. AC-4 and KPI-2 'baseline' means B. B0 is kept so the PR shows how many tests were added.
- Rationale: KPI-1 and KPI-2 compare against an unknown baseline, and nobody owned capturing it. Two numbers exist, B0 before the new tests and B after them. The gate is stated once so both tracks use the same one. Boot 4.2 still runs on Java 17, so two commits tell framework breaks from JDK breaks. Adding the tests on the old stack proves they pass before the bump.
- Alternatives: Bump both at once: rejected, a failure cannot be attributed, Skip the baseline and compare absolute counts: rejected, KPI-2 needs a baseline, Capture only B after the new tests: rejected, it hides how many tests were added

### ADR-APP-5

- Title: Dependency-managed libraries follow the Boot BOM; explicit pins move only when broken
- Decision: Do not override versions of Hibernate, Spring Data, Thymeleaf, Jackson, Caffeine, Micrometer, the JDBC drivers or Testcontainers; the 4.2 parent manages them. Keep Checkstyle 12.3.1 and spring-javaformat 0.0.47, and bump a build plugin (javaformat, nohttp, JaCoCo, native-maven-plugin, CycloneDX, Gradle dependency-management or wrapper) only if ./mvnw verify or ./gradlew build fails on it. Do not move to Checkstyle 13. Record each bump with its reason.
- Rationale: Spring owns the compatibility matrix and manual overrides add risk. A Checkstyle 13 jump changes the rules and is not needed. The Java 21 baseline only removes the cap, it does not demand the move.
- Alternatives: Move Checkstyle to 13 now: rejected, it changes lint rules and is out of scope, Pin newer drivers: rejected, not needed for 4.2

### ADR-APP-6

- Title: AOT and runtime hints are verified and changed only on failure
- Decision: Run AOT processing (./mvnw -Pnative -DskipTests process-aot, or the equivalent) and the existing tests on 4.2.x + Java 21. Keep PetClinicRuntimeHints as it is. Change only the hint a failure points to. The native image build is not part of the gate.
- Rationale: The hints cover resources and the serialization of three types. Nothing shows they break, and they are not a gate item in the brief.
- Alternatives: Rewrite hints with @RegisterReflectionForBinding or @ImportRuntimeHints: rejected, unneeded churn, Make a native-image build a gate: rejected, not in Done means

### ADR-APP-7

- Title: Cutover, write-freeze and rollback: single-PR merge, no write-freeze, revert to roll back
- Decision: There is no data migration, so write-freeze is not applicable (maximum duration 0). The cutover is the merge of one upgrade PR to main. Any running deployment is a redeploy of the new build, because a running JVM cannot change its own Java or Boot version. Rollback is a git revert of the PR plus a redeploy of the previous build, verified by a green ./mvnw -B verify on Java 17 + Boot 4.1.0. After the first write on the new build, the old build still works because no schema, SQL or entity change is made: H2 is in memory and discarded, and the MySQL and PostgreSQL schema is unchanged. This assumes Hibernate does not alter the schema on its own (ddl-auto not set to update, to be checked in the application properties).
- Rationale: The change is build-time and stateless for data. A revert is the smallest reliable rollback. The ddl-auto assumption is named so that it is checked rather than assumed.
- Alternatives: Run old and new side by side: rejected, one repository and one artefact, Feature flag the Boot version: not possible

### ADR-APP-8

- Title: Interface the platform track follows
- Decision: The platform track must: (a) set every Java setting to 21 in maven-build.yml and gradle-build.yml (matrix ['21']), in .devcontainer/Dockerfile (VARIANT and JAVA_VERSION) and in .gitpod.yml, with devcontainer.json already at 21 and no file disagreeing; (b) keep ./mvnw -B verify as the gate job and ./gradlew build as the sync job; (c) use a JDK distribution with a Java 21 build, with temurin as the fallback if adopt has none; (d) fix cache: maven in the Gradle workflow to cache: gradle; (e) not hard-code the Boot version, and read it from pom.xml; (f) run the Testcontainers tests where Docker exists and report skips separately; (g) leave docker-compose.yml images and k8s/petclinic.yml unchanged. The application track provides the pinned versions (ADR-APP-1), the baseline artefact (ADR-APP-4) and the sync check (SPEC-APP-3).
- Rationale: The application track sets the contract so the specialist can cite it. Without it CI could stay on 17 and pass without proving Java 21 (AC-12).
- Alternatives: Let CI choose its own Java version: rejected, the evidence for KPI-1 and KPI-2 would be unreliable, Add a Java 17 and 21 matrix: rejected, the enforcer requires at least 21

### ADR-APP-9

- Title: Test approach: no test removed, count compared with the baseline
- Decision: KPI-2 is met when the Maven run has 0 failures and 0 errors, executed tests are at least B (ADR-APP-4), and no test is newly @Disabled or skipped. New tests are added only on the baseline (ADR-APP-4). Tests that need Docker and cannot run are reported as 'not run', never as passes. Tests are not generated from the PR #144 Pets spec.
- Rationale: A count that must not fall stops 'green by deletion'. The skipped-test rule keeps container tests from being read as proof.
- Alternatives: Accept any green run: rejected, it can hide deleted tests, Gate on Gradle tests: rejected, Maven is the gate

### ADR-APP-10

- Title: Actuator exposure is unchanged in this upgrade
- Decision: Keep management.endpoints.web.exposure.include=*. Verify /actuator/health returns 200 and /actuator/metrics responds on 4.2.x. If 4.2 changes the exposed set or the default, record the difference and do not narrow exposure inside this change.
- Rationale: Narrowing exposure is a behaviour change outside the brief. It is passed to security as an open issue.
- Alternatives: Restrict to health, info and metrics: rejected for this change as scope creep, recommended as a follow-up

## Platform track

The platform track moves both CI workflows, the Gitpod Dockerfile and .gitpod.yml to Java 21, using Temurin. It makes the Maven job the evidence source for KPI-1 to KPI-3: test totals compared with the Java 17 baseline, a startup and Actuator check, and a version-sync step. Rollback is a git revert of the single PR, rehearsed on a branch. There is no deployment pipeline for this application in the repo, so release authorization is the product owner's approval and merge of the PR once the required checks are green.

| Component | Change | Responsibility |
|---|---|---|
| maven-build.yml (Maven CI, acceptance gate) | changed | Matrix ['17'] becomes ['21'] and the distribution moves from adopt to temurin. It keeps `./mvnw -B verify` as the gate. It adds a test-totals step (compare with the baseline, skips reported separately), a startup and Actuator check, evidence upload, `timeout-minutes` and `permissions: contents: read`. |
| gradle-build.yml (Gradle CI, sync job) | changed | Matrix ['21'] with temurin. `cache: maven` becomes `cache: gradle`. It runs the version-sync check before `./gradlew build`. It is a sync job, not the gate. |
| .devcontainer/Dockerfile | changed | Gitpod image. VARIANT 17-bullseye becomes a Java 21 variant and JAVA_VERSION 17.0.7-ms becomes a Java 21 sdkman identifier. The base image moves to mcr.microsoft.com/devcontainers/java:1-${VARIANT}, because the legacy vscode/devcontainers/java:0- path is not known to publish a Java 21 tag. |
| .devcontainer/devcontainer.json | unchanged | Already requests Java 21 (21-oracle). It is the reference value the Dockerfile must now agree with. It stays unchanged, so a rollback leaves it at 21, which still builds a Java 17 target. |
| .gitpod.yml | unchanged | Uses the Dockerfile image and runs `./mvnw install`, so it gets Java 21 through the Dockerfile. It holds no Java or Boot version, so the file does not change. |
| devcontainer-build.yml (image check) | new | Path-filtered workflow (.devcontainer/**, .gitpod.yml). It builds the Dockerfile and fails unless `java -version` in the image reports 21. It is the pre-merge rehearsal of the Gitpod image and is not a required check. |
| ci/test-totals.sh and ci/baseline-test-totals.json | new | Reads the Surefire and Failsafe XML and writes run, failures, errors and skipped to the job summary and to JSON. It has capture mode and compare mode. It adds no test to the Maven suite, so the KPI-2 count is unaffected. |
| ci/startup-check.sh | new | Starts `./mvnw spring-boot:run` on the default H2 profile and polls /actuator/health up to 120 s. It then checks /actuator/metrics, the log for ERROR lines, Java 21 and the pom's Boot version. It also checks /livez and /readyz with the additional-paths property, because k8s/petclinic.yml relies on them. It stops the app and keeps the log. |
| Version-sync check (application track, run by CI) | new | Owned by the application track (SPEC-APP-3). CI only calls it and fails the job on a mismatch. It reads Java and Boot versions from pom.xml and build.gradle, and no platform file holds a Boot version. |
| docker-compose.yml, k8s/petclinic.yml, deploy-and-test-cluster.yml | unchanged | Compose has database images only. The Kubernetes manifest deploys the external image dsyer/petclinic, and its workflow triggers only on k8s/** paths. None of them is reached by the Java move, and this PR does not trigger the cluster workflow. |

**Specifications**

### SPEC-PLAT-1

- Module: maven-build.yml
- Statement: The Maven workflow builds on Java 21 with Temurin and runs `./mvnw -B verify` as the acceptance gate.
- Accepted when: The matrix is ['21'] and the distribution is temurin; `cache: maven` is kept, The job runs `./mvnw -B verify` and prints `java -version`, The job has `permissions: contents: read` and `timeout-minutes` set, The file holds no Spring Boot version, Before the Java 21 commit the matrix stays ['17'], so the Boot-4.2-on-17 commit is tested on 17
- Traces to: [KPI-1](brief.md#kpi-1), [KPI-2](brief.md#kpi-2), [AC-1](brief.md#ac-1), [AC-12](brief.md#ac-12), [ADR-PLAT-1](design.md#adr-plat-1), [ADR-APP-8](design.md#adr-app-8)

### SPEC-PLAT-2

- Module: gradle-build.yml
- Statement: The Gradle workflow runs `./gradlew build` on Java 21 as a sync job and caches Gradle, not Maven.
- Accepted when: The matrix is ['21'] and the distribution is temurin, `cache: maven` is replaced by `cache: gradle`, The job runs `./gradlew build` and exits 0 on the upgrade branch, The file holds no Spring Boot version
- Traces to: [KPI-3](brief.md#kpi-3), [AC-13](brief.md#ac-13), [ADR-PLAT-1](design.md#adr-plat-1), [ADR-APP-8](design.md#adr-app-8)

### SPEC-PLAT-3

- Module: Test totals and baseline comparison
- Statement: The Maven job records the test totals on every run and compares them with the Java 17 + Boot 4.1.0 baseline, with Testcontainers results reported separately.
- Accepted when: The summary shows run, failures, errors and skipped, and separate lines for MySqlIntegrationTests and PostgresIntegrationTests, On the Java 17 baseline commit the step captures ci/baseline-test-totals.json content as an artifact, In compare mode the step fails when failures or errors > 0, executed < B or skipped > baseline skipped, On a Java 21 run with no baseline file the step fails, The step adds no test to the Maven suite
- Traces to: [KPI-2](brief.md#kpi-2), [AC-4](brief.md#ac-4), [AC-11](brief.md#ac-11), [ADR-PLAT-3](design.md#adr-plat-3), [ADR-PLAT-7](design.md#adr-plat-7), [ADR-APP-9](design.md#adr-app-9)

### SPEC-PLAT-4

- Module: Startup and Actuator check
- Statement: After verify, CI starts the application on Java 21 with the default H2 profile and proves startup and the Actuator endpoints.
- Accepted when: `./mvnw spring-boot:run` reaches HTTP 200 on /actuator/health within 120 s, otherwise the step fails, startup.log has 0 ERROR lines and shows Java 21 and a Boot version equal to the pom parent version, /actuator/metrics returns 200 with a non-empty names list, With `management.endpoint.health.probes.add-additional-paths=true`, /livez and /readyz return 200, The process is stopped in every case and startup.log is uploaded
- Traces to: [KPI-1](brief.md#kpi-1), Actuator health and metrics endpoints, [AC-3](brief.md#ac-3), [ADR-PLAT-2](design.md#adr-plat-2), [ADR-APP-10](design.md#adr-app-10)

### SPEC-PLAT-5

- Module: Version-sync in CI
- Statement: CI calls the application track's version-sync check on every run and fails on any mismatch of the Java or Boot version between pom.xml and build.gradle.
- Accepted when: The Gradle job runs the check before `./gradlew build`, The output names the field that differs, A PR that changes only one file fails this step, The exact Boot version appears in the summary and in the evidence artifact
- Traces to: [KPI-3](brief.md#kpi-3), [AC-2](brief.md#ac-2), [ADR-APP-1](design.md#adr-app-1), [ADR-APP-2](design.md#adr-app-2)

### SPEC-PLAT-6

- Module: Devcontainer and Gitpod image
- Statement: The Gitpod image builds on Java 21 and no platform file names a different Java major version.
- Accepted when: Dockerfile VARIANT and JAVA_VERSION name Java 21; devcontainer.json is 21; .gitpod.yml holds no Java version, devcontainer-build.yml builds the Dockerfile and `bash -lc 'java -version'` in the image reports 21, The workflow triggers only on .devcontainer/** and .gitpod.yml, A grep of .github, .devcontainer and .gitpod.yml finds no Java 17 setting
- Traces to: [KPI-1](brief.md#kpi-1), [AC-12](brief.md#ac-12), [ADR-PLAT-4](design.md#adr-plat-4), [ADR-APP-8](design.md#adr-app-8)

### SPEC-PLAT-7

- Module: Evidence and retention
- Statement: Every Maven and Gradle run on the upgrade branch leaves evidence that the close-out can read for KPI-1 to KPI-3.
- Accepted when: Artifact java21-evidence is uploaded with `if: always()` and kept 90 days, It holds the Surefire and Failsafe reports, totals JSON, startup.log, HTTP codes and the Java and Boot versions, The job summary shows build result, startup result, executed tests and the version pair, A failed job still uploads the artifact
- Traces to: [KPI-1](brief.md#kpi-1), [KPI-2](brief.md#kpi-2), [KPI-3](brief.md#kpi-3), [ADR-PLAT-2](design.md#adr-plat-2)

### SPEC-PLAT-8

- Module: Release authorization and required checks
- Statement: The PR can be merged only with the product owner's approval and the required checks green, and the required-check names match the renamed jobs.
- Accepted when: Branch protection on main requires the Maven job and the Gradle job under the names they have after the matrix change, No required check is left waiting for `build (17)`, The PR needs the product owner's approval, The PR description lists the pinned Boot patch, the baseline numbers and the devcontainer-build result, No workflow deploys the build
- Traces to: [AC-12](brief.md#ac-12), [ADR-PLAT-5](design.md#adr-plat-5), [KPI-1](brief.md#kpi-1)

### SPEC-PLAT-9

- Module: Rollback rehearsal
- Statement: Reverting the PR restores the Java 17 + Boot 4.1.0 pipeline, and this is rehearsed before merge.
- Accepted when: On a throw-away branch, the revert gives a green Maven job on Java 17 (the matrix is back to ['17'] with adopt), The revert changes both build files, both workflows and the Dockerfile in one commit, and needs no database step, The elapsed time from decision to green main is recorded in the PR and is at most 30 minutes, devcontainer.json is shown to stay unchanged and harmless
- Traces to: [ADR-PLAT-6](design.md#adr-plat-6), [ADR-APP-7](design.md#adr-app-7), [KPI-1](brief.md#kpi-1), [AC-12](brief.md#ac-12)

### SPEC-PLAT-10

- Module: Unchanged platform files
- Statement: Compose, Kubernetes and the cluster workflow are not changed, and Compose and the wrappers still work.
- Accepted when: git diff shows no change to docker-compose.yml, k8s/** or deploy-and-test-cluster.yml, Docker Compose based tests and the MySQL and PostgreSQL integration tests run on ubuntu-latest, or are shown as 'not run', `./mvnw` and `./gradlew` run on JDK 21 in CI
- Traces to: [AC-11](brief.md#ac-11), [AC-12](brief.md#ac-12), [ADR-PLAT-7](design.md#adr-plat-7)

**Decisions**

### ADR-PLAT-1

- Title: Java 21 everywhere in the platform files, with Temurin
- Decision: Set the matrix to ['21'] in maven-build.yml and gradle-build.yml. Change `distribution: 'adopt'` to `temurin` in both. Move the Dockerfile VARIANT and JAVA_VERSION to Java 21. Leave devcontainer.json at 21-oracle. Do not run a 17 and 21 matrix. Print `java -version` in each job so the exact patch is in the evidence.
- Rationale: The enforcer requires at least ${java.version}, so a 17 leg would fail on the 21 pom. The `adopt` distribution is the deprecated AdoptOpenJDK alias, and there is no evidence it serves Java 21. Temurin is the maintained Adoptium build. A floating `21` picks up JDK security patches. The cost is that the patch level can change between runs, so the exact version goes into the evidence.
- Alternatives: Keep adopt and hope it resolves 21: rejected, no evidence and a deprecated alias, 17+21 matrix: rejected, the enforcer fails the 17 leg and the gate is Java 21

### ADR-PLAT-2

- Title: Evidence for KPI-1 to KPI-3 is produced inside the existing jobs
- Decision: The Maven job runs, in order: verify, test-totals step, startup-check step, artifact upload (`if: always()`). The upload holds the Surefire and Failsafe reports, totals JSON, startup.log, the HTTP codes and the versions. The Gradle job runs the version-sync step and `./gradlew build`. Each job writes a summary to $GITHUB_STEP_SUMMARY. Neither job is split into extra jobs.
- Rationale: The brief reads KPI-1 and KPI-2 from the Maven build log, the startup log and CI test results. One gate job keeps one required check. The startup check proves AC-3 on the built tree, which `verify` alone does not.
- Alternatives: Separate startup workflow: rejected, a second check to require and a second checkout and build, Rely on the CI log only: rejected, logs are hard for the close-out to read and are not kept as long

### ADR-PLAT-3

- Title: The baseline is captured by the PR's own CI on the Java 17 commit
- Decision: The first PR commit adds ci/test-totals.sh and calls it while the matrix is still 17, on Java 17 + Boot 4.1.0. That run produces the baseline artifact (capture mode). The application track commits the numbers as ci/baseline-test-totals.json after the new PetController tests are added (final count B). From the Boot 4.2 commit on, the step runs in compare mode: it fails on any failure or error, on executed < B, and on skipped > the baseline skipped. If the file is missing on a Java 21 run, the step fails. A workflow_dispatch baseline workflow is not used.
- Rationale: A workflow_dispatch workflow must already be on the default branch, so it could not run before the change. The PR's first commit runs on the old stack by construction, and the Boot-on-17 commit isolates framework breaks from JDK breaks (ADR-APP-4). This enforces the no-fewer-tests rule (ADR-APP-9) in CI.
- Alternatives: Capture on a developer machine: rejected, no CI artifact, not reproducible, Hard-code B in the workflow: rejected, it hides the number in YAML

### ADR-PLAT-4

- Title: Gitpod image: devcontainers/java base and a build check
- Decision: In the Dockerfile, set the base to mcr.microsoft.com/devcontainers/java:1-${VARIANT} with VARIANT=21-bookworm. Set JAVA_VERSION to a Java 21 Microsoft sdkman identifier, the newest one that `sdk list java` shows at implementation time. Keep the user, the .m2 and .gradle volumes and the NODE_VERSION logic. Add devcontainer-build.yml, which builds the image and fails unless `java -version` reports 21. Leave .gitpod.yml unchanged.
- Rationale: The current file takes mcr.microsoft.com/vscode/devcontainers/java:0-17-bullseye, and no evidence shows a Java 21 tag on that legacy path. The Dockerfile then sdk-installs 17.0.7-ms on top, so Gitpod would build a Java 21 pom on Java 17. A build check proves the image before merge. Gitpod itself cannot be run from CI.
- Alternatives: Keep the legacy image path and only change the numbers: rejected, the tag may not exist and nothing would prove it, Delete the Dockerfile and use devcontainer.json for Gitpod: rejected, removes a file outside the brief's scope

### ADR-PLAT-5

- Title: Release authorization is the PR approval; required checks keep their names
- Decision: The release authorization is the product owner's approval and merge of the upgrade PR to main. It needs the Maven job green with the evidence artifact and the Gradle sync job green. The devcontainer-build job must be green for this PR but is not a branch-protection requirement. The Maven and Gradle job and check names must stay what branch protection requires. A matrix change renames the check `build (17)` to `build (21)`, so the required-check list is updated in the same step (SPEC-PLAT-8). No pipeline deploys the application, and the platform track adds none.
- Rationale: A required check that is still named `build (17)` would stay 'pending' forever and block the merge. Or it would not be enforced at all. The repo has only CI, a kind-cluster workflow for k8s/** and an external image, so there is no deployment of this build to design.
- Alternatives: Drop the matrix and use a fixed job name: rejected, ADR-APP-8 asks for a matrix of 21 and the rename would still need checking, Add an auto-deploy job: rejected, nothing in the repo is deployed from this build

### ADR-PLAT-6

- Title: Rollback is a git revert of the PR, rehearsed before merge
- Decision: Rollback = `git revert` of the merge commit (or the PR commits) on a branch, then CI must show `./mvnw -B verify` green on Java 17 + Boot 4.1.0. The revert restores matrix 17, adopt, the 17 Dockerfile and both build files together. devcontainer.json stays at 21 and still builds the 17 target. The rehearsal runs on a throw-away branch before merge. The target is a green main within 30 minutes of the decision, to be timed in the rehearsal. No database step is needed (ADR-APP-7).
- Rationale: The change is version strings, CI files and a Dockerfile in one PR, so a revert restores everything atomically. A rehearsal is the only way to know the old stack still builds, for example that adopt and the legacy Dockerfile image still resolve.
- Alternatives: Forward-fix only: rejected, no tested way back, Keep a Java 17 workflow beside the 21 one: rejected, the 21 pom cannot build on 17

### ADR-PLAT-7

- Title: Compose, Kubernetes and the cluster workflow stay untouched
- Decision: Do not edit docker-compose.yml, k8s/petclinic.yml or deploy-and-test-cluster.yml. The Compose images (mysql:9.7, postgres:18.4) have no Java. Testcontainers and Docker Compose tests run on ubuntu-latest, which has a container runtime. The MySQL and PostgreSQL results are shown as separate lines in the totals summary, and a skipped Testcontainers test is reported as 'not run', never as a pass.
- Rationale: The upgrade does not reach these files, and the k8s image dsyer/petclinic is not built from this repo. Changing them would be scope creep and a risk to a must-not-break item.
- Alternatives: Point k8s at an image built from this repo: rejected, a new deployment path outside the brief
