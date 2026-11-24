# Brief: java-17-to-21-upgrade-5d8272c2

**Goal**: Run on Java 21 and Spring Boot 4.2.x.

**Why now**: Adopt the current LTS Java (21) and the current supported Spring Boot line (4.2.x). Spring Boot 4.1.0 is not at end of support, so the earlier 'end of support' reason is removed (product owner's answer).

## Scope

In scope:

- Build files: pom.xml and build.gradle, both currently pinning Java 17 and Spring Boot 4.1.0, kept in sync (request; product owner's answer)
- Dependency versions (request)
- Any code that breaks because of the upgrade to Java 21 and Spring Boot 4.2.x (request)

Out of scope:

- Changing the PetController behaviour, including the future-birth-date rejection and the duplicate-name check (product owner's answer)
- Gradle as the acceptance gate: build.gradle stays in the repository and in sync, but Maven decides acceptance (product owner's answer)
- (proposed) New features or behaviour changes to owner, pet, visit or vet functionality; the upgrade must preserve behaviour
- (proposed) Database engine or schema changes (the H2, MySQL and PostgreSQL profiles stay as they are). The open ShiftForge work on owners-to-MySQL and MySQL-to-PostgreSQL is separate.
- (proposed) Refactoring or upgrades that are not needed to build, start and pass tests on Java 21 and Spring Boot 4.2.x

## Must not break

- Pet create and edit flows, including rejecting future birth dates and the duplicate-name check. PetController behaviour stays as it is.
- Pet validation rules in PetValidator (required name, name length at most 30, required birth date)
- Owner registration, edit, search by last name and detail flows (owner records are a sensitive zone)
- Visit scheduling for a pet and visit history
- Vet directory page, JSON resource and cached vet list
- Persistence on the H2 (default), MySQL and PostgreSQL profiles through Spring Data JPA
- CI build and test pipeline, and the Maven/Gradle wrappers and Docker Compose files bundled in the repo
- Actuator health and metrics endpoints

## Constraints

- [scope] In scope: the build files (pom.xml and build.gradle) plus code that breaks on the upgrade.
- [technical] Target is Java 21 and Spring Boot 4.2.x. Keep pom.xml and build.gradle in sync after the upgrade.
- [policy] Maven is the acceptance gate for 'all tests pass' (./mvnw test, ./mvnw spring-boot:run). Gradle stays in the repo but is not the gate.
- [policy] PetController behaviour stays as it is: future birth dates are rejected on create and edit, and the duplicate-name check stays. The baseline Pets spec in PR #144 that says future dates are accepted is wrong and must not drive a change.
- [policy] Capture the baselines on the current stack (Java 17 + Boot 4.1.0: ./mvnw -B verify totals and app start) before any change, then repeat on Java 21 + Boot 4.2.x.
- [policy] (proposed) Test counts must not be reduced: no test removed or disabled to reach 0 failing tests.
- [compliance] Not a regulated client.

## Success criteria

### KPI-1

- Criterion: The app builds and starts on Java 21 and Spring Boot 4.2.x.
- Metric: Failures of ./mvnw -B verify on a Java 21 JDK (count), plus startup failures of ./mvnw spring-boot:run on Java 21 (count)
- Target: 0 build failures; 0 startup failures; the startup log shows Java 21 and Spring Boot 4.2.x
- Baseline: unknown. Capture before any change on Java 17 + Boot 4.1.0: run ./mvnw -B verify and record the result, then start the app with ./mvnw spring-boot:run and record success or failure.
- Window: Every CI run on the upgrade branch, and a final run on the branch to be merged

### KPI-2

- Criterion: ./mvnw test passes with 0 failing tests on Java 21 and Spring Boot 4.2.x. Maven is the acceptance gate.
- Metric: Failing tests (count) and executed tests (count) in the ./mvnw test run on Java 21
- Target: 0 failing tests; executed tests at least the baseline total; 0 tests newly skipped or disabled
- Baseline: unknown. Capture before any change on Java 17 + Boot 4.1.0: run ./mvnw -B verify and record the test totals (run, failures, errors, skipped).
- Window: Every CI run on the upgrade branch, and a final run on the branch to be merged

### KPI-3

- Criterion: pom.xml and build.gradle are kept in sync on the target versions.
- Metric: Mismatches between pom.xml and build.gradle in the Java version and Spring Boot version (count)
- Target: 0 mismatches; both name Java 21 and the same Spring Boot 4.2.x version
- Baseline: 0 today in the sense that both pin Java 17 and Boot 4.1.0 (product owner's answer). Confirm by reading both files before any change.
- Window: At the final review of the upgrade pull request


## Acceptance criteria

### AC-1

- Given: The upgrade branch with pom.xml on Java 21 and Spring Boot 4.2.x, and a clean checkout on a Java 21 JDK
- When: ./mvnw -B verify runs
- Then: The build exits 0 with 0 failures

### AC-2

- Given: The upgrade branch
- When: The Java version and the Spring Boot version are read from pom.xml and build.gradle
- Then: Both files name Java 21 and the same Spring Boot 4.2.x version, with 0 mismatches

### AC-3

- Given: The built application on a Java 21 runtime with the default H2 profile
- When: The application starts with ./mvnw spring-boot:run and /actuator/health is requested
- Then: Startup completes with 0 errors in the log, the log shows Java 21 and Spring Boot 4.2.x, and the endpoint returns HTTP 200

### AC-4

- Given: The upgrade branch on Java 21
- When: ./mvnw test runs
- Then: 0 tests fail, the number of executed tests is at least the baseline count captured on Java 17 + Boot 4.1.0, and 0 tests are newly skipped or disabled

### AC-5

- Given: An owner with an existing pet named 'Leo'
- When: A new pet named 'Leo' is submitted for the same owner through the create form
- Then: The submission is rejected with a 'duplicate' error on the name field, the form is redisplayed (HTTP 200) and no second pet is saved

### AC-6

- Given: An owner exists and the create pet form is open
- When: A pet with a valid unique name and a birth date in the future is submitted
- Then: The submission is rejected with a validation error on the birth date field, the form is redisplayed (HTTP 200) and no pet is saved, as on the Java 17 baseline

### AC-7

- Given: An existing pet with another pet belonging to the same owner
- When: The edit form is submitted with the pet's own current name, then with the name of the owner's other pet, then with a future birth date
- Then: The first submission redirects (3xx) and saves; the second is rejected with a 'duplicate' error on the name field; the third is rejected with a validation error on the birth date field

### AC-8

- Given: A new pet form
- When: A pet with a valid unique name, a valid past birth date and a pet type is submitted
- Then: The pet is saved, the user is redirected to the owner page, and the pet appears in the owner's pet list

### AC-9

- Given: The seeded sample data
- When: An owner is created, edited and found by last name, and a visit is booked for a pet
- Then: Each action returns the same HTTP status and visible result as on the Java 17 baseline, and the owner and visit tests pass

### AC-10

- Given: The seeded vet data
- When: The vet list page and the JSON resource are requested twice
- Then: Both return the 6 seeded vets with their specialties as on the baseline, and the vet tests pass

### AC-11

- Given: The upgrade branch
- When: The MySQL and PostgreSQL integration tests run where a container runtime is available
- Then: They pass with 0 failures on Java 21, and the H2 profile still starts

### AC-12

- Given: The upgrade branch with its pull request
- When: The CI pipeline runs on the pull request
- Then: All Maven-based CI jobs finish green on Java 21, and Docker Compose and the Maven and Gradle wrappers still work

### AC-13

- Given: The upgrade branch on a Java 21 JDK
- When: ./gradlew build runs as a sync check (not the acceptance gate)
- Then: The build exits 0 and uses the same Java and Spring Boot versions as pom.xml
