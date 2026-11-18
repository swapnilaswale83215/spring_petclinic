# Feature Specification: Owners Module on Local MySQL (POC)

**Feature Branch**: `shiftforge/owners-to-mysql-2c9b9e7c`

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "Run the PetClinic owner module on local MySQL for this POC."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Cold-Start the App Against Local MySQL (Priority: P1)

A developer starting the POC wants to launch the PetClinic application pointed at the local Docker MySQL 8.4 container and confirm it comes up cleanly, with seed data loaded and no errors, before any owner flows are exercised.

**Why this priority**: Every other scenario depends on a successful, error-free startup. Without this, nothing else can be verified.

**Independent Test**: Start the app with the mysql profile against a fresh petclinic-mysql container. The test is complete when the startup log shows the application started, the seed row counts are correct, and zero ERROR-level log entries appear.

**Acceptance Scenarios**:

1. **Given** the Docker container petclinic-mysql (MySQL 8.4, database petclinic, user petclinic, localhost:3306) is running and its volume is empty, and the app is configured with `SPRING_PROFILES_ACTIVE=mysql` and the matching `MYSQL_*` variables, **When** the app is started cold, **Then** it reports started, the seed data is loaded (10 owners, 13 pets, 4 visits), and there are 0 ERROR-level startup log entries and no startup exception. *(AC-1, KPI-1, SPEC-APP-1)*

2. **Given** the same container and volume from scenario 1 (not wiped), **When** the app is started a second time, **Then** the seed counts remain unchanged (no duplicates), and there are again 0 ERROR-level startup log entries. *(SPEC-DATA-2)*

3. **Given** the app has been stopped and `docker compose down -v` has been run, **When** a new container is started and the app is cold-started again, **Then** the results are identical to scenario 1. *(SPEC-DATA-7)*

---

### User Story 2 — Owner Create, Search and Update Through the UI (Priority: P1)

A clinic receptionist uses the owner web UI to create a new owner, search for owners by last name, and update an existing owner's contact details — all backed by MySQL instead of H2.

**Why this priority**: These three flows are the core owner operations that the POC must demonstrate end-to-end on MySQL. They are the direct subject of KPI-2.

**Independent Test**: With the app running against the seeded MySQL container, exercise the create, search and update flows through the owner UI. All three must succeed and persist correctly in MySQL.

**Acceptance Scenarios**:

1. **Given** the app runs against the seeded MySQL and no owner with last name 'Zzcreate' exists, **When** a new owner with last name 'Zzcreate' and a valid 10-digit telephone is submitted through the owner UI form (`POST /owners/new`), **Then** the creation succeeds, the owner is assigned id 11, the row is present in MySQL, and a following search for 'Zzcreate' finds exactly that owner. *(AC-2, KPI-2, SPEC-APP-2)*

2. **Given** the app runs against the seeded MySQL and owners with different last names exist, **When** an owner search by last-name prefix is run through the UI (`GET /owners?lastName=…`), **Then** the results contain exactly the owners whose last name matches the prefix, in the same order as the before-change recording. *(AC-3, KPI-2, SPEC-APP-3)*

3. **Given** an existing owner exists in the seeded MySQL, **When** the owner's address or telephone is updated through the UI edit form (`POST /owners/{id}/edit`), **Then** the update succeeds, the detail page and the MySQL row show the new values, and the other nine owner rows are unchanged. *(AC-4, KPI-2, SPEC-APP-4)*

4. **Given** the owner detail page is opened for owner 1, **When** the page is rendered, **Then** it shows the owner, the pets ordered by name, and each pet's visits, identical to the before-change recording. *(SPEC-APP-5)*

---

### User Story 3 — Owner Contract Unchanged Before and After the Switch (Priority: P1)

A product owner and QA engineer want proof that switching the owner module from H2 to MySQL 8.4 has not altered any observable owner behaviour: URLs, status codes, redirects, form fields, validation messages and rendered content must all match the before-change recording exactly.

**Why this priority**: The explicit constraint is that owner API and UI behaviour must not change. KPI-3 (0 differences) is a hard acceptance gate for the POC.

**Independent Test**: Record all owner routes on the current H2 app, then replay the same requests against the MySQL app and diff the responses. The test is complete when the difference count is 0.

**Acceptance Scenarios**:

1. **Given** the before-change recording of owner UI requests and responses (taken on the H2 default profile) exists as the reference, **When** the same requests are replayed against the app running on MySQL 8.4 (started on a fresh volume with the date-aligned seed), **Then** the number of differences is 0 across all recorded cases, including case-variant last-name searches and blank-search page order. *(AC-5, KPI-3, SPEC-APP-8)*

2. **Given** the owner screens before the change are recorded, **When** the owner list, find, detail, create and edit screens are opened after the change, **Then** the URLs, form fields, validation messages and visible behaviour match the recording with 0 differences. *(AC-6, KPI-3, SPEC-APP-7)*

3. **Given** the seed parity check (SPEC-DATA-5) has reported 0 differences between the H2 and MySQL seeds, **When** the golden-file replay is run, **Then** no date difference can appear as a MySQL behaviour difference, and the replay is not started unless the seed parity check has passed first. *(SPEC-APP-8, SPEC-DATA-5)*

---

### User Story 4 — Owner Test Suites Stay Green on MySQL 8.4 (Priority: P2)

A developer wants confirmation that the four owner-related test suites all pass when run against MySQL 8.4, proving that the existing test coverage holds on the new database.

**Why this priority**: Green tests on MySQL 8.4 are a required gate before the branch can be merged. They are secondary to the live-app flows only because the live flows are the primary POC demonstration.

**Independent Test**: Run `OwnerControllerTests`, `ClinicServiceTests`, `PetClinicConcurrencyTests` and `MySqlIntegrationTests` and report results per suite, including the database used.

**Acceptance Scenarios**:

1. **Given** the change is on its branch, **When** `OwnerControllerTests` is run (it uses a mocked repository and no database), **Then** all tests pass with 0 failures, and the report notes it is a controller-only check with no database. *(AC-7, SPEC-APP-6)*

2. **Given** a fresh MySQL 8.4 container is running, **When** `ClinicServiceTests` and `MySqlIntegrationTests` are run with `SPRING_PROFILES_ACTIVE=mysql` against that container, **Then** all tests pass with 0 failures and 0 skipped, and the report shows the database as MySQL 8.4 and the server version. *(AC-7, SPEC-APP-6)*

3. **Given** a separate fresh MySQL 8.4 container is running, **When** `PetClinicConcurrencyTests` is run with `SPRING_PROFILES_ACTIVE=mysql`, **Then** all tests pass with 0 failures, and the report shows MySQL 8.4. *(AC-7, SPEC-APP-6)*

4. **Given** `ClinicServiceTests` and `PetClinicConcurrencyTests` are also run on the default H2 profile, **When** results are collected, **Then** both suites pass on H2 as well, and the report lists H2 and MySQL 8.4 results side by side. *(SPEC-APP-6)*

---

### User Story 5 — Seed Dates Aligned Between H2 and MySQL (Priority: P2)

A developer needs the 17 pet birth dates and visit dates in `db/mysql/data.sql` to match the values in `db/h2/data.sql` exactly, so that the before-change (H2) recording and the after-change (MySQL) replay can be compared with 0 differences.

**Why this priority**: Without date alignment the golden-file replay would produce spurious differences, making KPI-3 unachievable. It is a prerequisite for User Story 3 but is a data-only change with no user-facing behaviour.

**Independent Test**: Diff the 17 date values in `db/mysql/data.sql` against `db/h2/data.sql` by natural key. The test is complete when all 17 values are equal and the running MySQL database rows confirm the same dates after seed load.

**Acceptance Scenarios**:

1. **Given** the aligned `db/mysql/data.sql` has been committed, **When** the 13 `pets.birth_date` values and the 4 `visits.visit_date` values in the MySQL seed are compared with the H2 seed by natural key, **Then** all 17 values are equal and the difference count is 0. *(SPEC-DATA-10, SPEC-DATA-9)*

2. **Given** the app has been started on a fresh MySQL 8.4 volume with the aligned seed, **When** the 17 date rows are read back from the running database, **Then** they match the expected values with no timezone or type shift. *(SPEC-DATA-10)*

3. **Given** the diff of `db/mysql/data.sql` is inspected, **When** the changed lines are counted, **Then** only the 17 date values are different; ids, names, owner rows, type ids, descriptions, row counts and `INSERT IGNORE` statements are untouched, and `db/mysql/schema.sql` is unchanged. *(SPEC-DATA-9)*

---

### Edge Cases

- What happens when the MySQL container is not running when the app starts? The app must fail fast with a clear connection error and not silently fall back to H2.
- What happens when the owner create form is submitted with a blank last name, a first name longer than 30 characters, a missing telephone, or a telephone that is not exactly 10 digits? Each must re-display the form with the same validation messages as the before-change recording, and no row must be inserted.
- What happens when an owner id in the edit form does not match the URL path id? The app must redirect to the edit form with the 'Owner ID mismatch' flash message.
- What happens when a search is performed for a last name that matches no owner? The find-owners page must display the 'not found' error on the `lastName` field.
- What happens when a search is performed for a last name with leading or trailing whitespace? The result must be the same as the trimmed search, matching the before-change recording.
- What happens when an out-of-range page number is requested in the owner list? The app must redirect to page 1.
- What happens when the owner detail page is requested for a non-existent id? The app must surface the owner-not-found error with the same status as the before-change recording.
- What happens when the app is started a second time on the same (non-empty) volume? The seed must not be duplicated; all row counts must remain unchanged.
- What happens when `PetClinicConcurrencyTests` runs on MySQL and commits a pet to owner 1? Subsequent runs that depend on the owner 1 detail must start on a fresh volume to avoid interference.
- What happens when a rolled-back insert has consumed an `AUTO_INCREMENT` id? The 'Zzcreate' owner must still receive id 11; each verification run group must start on a fresh volume.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001** *(SPEC-APP-1, AC-1, KPI-1)*: The app MUST start against the local MySQL 8.4 container (`petclinic-mysql`, `localhost:3306`, database/user/password `petclinic`) with `SPRING_PROFILES_ACTIVE=mysql` and the `MYSQL_URL`, `MYSQL_USER` and `MYSQL_PASS` environment variables, and MUST report started with 0 ERROR-level startup log entries and no startup exception on a cold start from an empty volume.

- **FR-002** *(SPEC-APP-1, SPEC-DATA-2, AC-1)*: On a cold start from an empty volume, the app MUST load the seed data through `spring.sql.init` and produce exactly 10 owners, 13 pets, 4 visits, 6 vets, 3 specialties, 5 vet-specialties and 6 types. A second start on the same volume MUST leave all counts unchanged and MUST NOT insert duplicate rows.

- **FR-003** *(SPEC-APP-2, AC-2, KPI-2)*: The owner create flow (`POST /owners/new`) MUST persist a new owner in MySQL and redirect to `/owners/{newId}` with the flash message 'New Owner Created'. The first owner created after the seed MUST receive id 11. An injected id parameter MUST be ignored.

- **FR-004** *(SPEC-APP-2, AC-2)*: The owner create form MUST reject blank last name, a first name longer than 30 characters, a missing telephone and a telephone that is not exactly 10 digits, re-displaying the form with the same validation messages as the before-change recording and inserting no row.

- **FR-005** *(SPEC-APP-3, AC-3, KPI-2)*: The owner search flow (`GET /owners?lastName=…`) MUST return exactly the owners whose last name matches the given prefix, 5 per page, in the same order as the before-change recording. A single match MUST redirect to `/owners/{id}`. A blank search MUST list all 10 owners across 2 pages. A search with no match MUST display the find-owners page with the 'not found' error on `lastName`. An out-of-range page MUST redirect to page 1.

- **FR-006** *(SPEC-APP-3, SPEC-DATA-6)*: Last-name prefix searches MUST be case-insensitive. Searches for 'Davis', 'davis', 'DAVIS' and 'Dav' MUST each return owners 2 and 4 in the same order as the before-change recording.

- **FR-007** *(SPEC-APP-4, AC-4, KPI-2)*: The owner update flow (`POST /owners/{id}/edit`) MUST persist the changed address or telephone in MySQL and redirect to `/owners/{id}` with the flash message 'Owner Values Updated'. A blank first name MUST re-display the form and leave the row unchanged. An id in the form that does not match the URL MUST redirect to the edit form with the 'Owner ID mismatch' flash. An edit of a non-existent owner id MUST surface the owner-not-found error.

- **FR-008** *(SPEC-APP-5, AC-6)*: The owner detail page (`GET /owners/{id}`) MUST display the owner, the pets ordered by name and each pet's visits, read from MySQL, identical to the before-change recording. A non-existent id MUST surface the owner-not-found error with the same status as the recording.

- **FR-009** *(SPEC-APP-7, AC-5, KPI-3)*: Before any configuration or seed change, the owner HTML routes MUST be recorded on the current app (H2 default profile, H2 seed) as the KPI-3 reference. The recording MUST cover every route and case listed in ADR-APP-2, including case-variant last-name searches, blank search, pages 1 and 2, in the order returned. Each entry MUST hold method, URL and params, status, redirect target, flash message, validation messages and rendered body, with only CSRF tokens and timestamps normalised. The KPI-1 and KPI-2 baselines MUST be captured in the same run and labelled as reference readings.

- **FR-010** *(SPEC-APP-8, AC-5, KPI-3)*: After the seed date alignment, the same requests MUST be replayed against the app on MySQL 8.4 (fresh volume) and diffed against the recording. The difference count MUST be 0 for the whole recorded set. The replay MUST NOT start unless SPEC-DATA-5 has first reported 0 seed differences. Every difference MUST be listed with route, case and expected versus actual.

- **FR-011** *(SPEC-DATA-9, SPEC-DATA-10)*: The only change to `db/mysql/data.sql` MUST be the 13 `pets.birth_date` values and the 4 `visits.visit_date` values, set to the values in `db/h2/data.sql` as listed in SPEC-DATA-10. No other line in `db/mysql/data.sql` and no line in `db/mysql/schema.sql` MUST change.

- **FR-012** *(SPEC-APP-6, AC-7)*: `MySqlIntegrationTests` MUST reference `mysql:8.4` and MUST fail if the server does not report version 8.4. `ClinicServiceTests` and `PetClinicConcurrencyTests` MUST each be run once on H2 (default profile) and once with `SPRING_PROFILES_ACTIVE=mysql` against the `petclinic-mysql` 8.4 container, with 0 failures and 0 skipped in both runs. `OwnerControllerTests` MUST run once on the default profile and MUST be reported as a controller-only check with no database. Each MySQL run group MUST start on a fresh volume.

- **FR-013** *(SPEC-APP-10, ADR-APP-1)*: The owner module Java files (`Owner.java`, `OwnerRepository.java`, `OwnerController.java`) and the `Pet`, `Visit` and `PetType` classes, owner Thymeleaf templates and messages files MUST NOT be modified by this change. The diff of the branch MUST show no change under `owner/` or `model/` main sources, and no change to owner templates or messages files.

- **FR-014** *(SPEC-PLAT-1, ADR-APP-5)*: The `docker-compose.yml` mysql service MUST run `mysql:8.4` as container `petclinic-mysql`, publish only `127.0.0.1:3306:3306`, use database, user and password `petclinic`, and report healthy only once the server accepts connections. The postgres service MUST remain byte-identical to the current file.

- **FR-015** *(SPEC-PLAT-2, ADR-APP-5)*: `MySqlIntegrationTests` and `MysqlTestApplication` MUST name `mysql:8.4`. No file under `src/` or in `docker-compose.yml` MUST reference any other MySQL image tag. A CI check MUST enforce this and MUST fail when any of the three locations is changed to a different tag.

- **FR-016** *(SPEC-APP-9, ADR-APP-7)*: A check MUST report whether the committed configuration holds only the local POC credentials (`petclinic`/`petclinic`/`petclinic` and the empty root password) and whether startup and test logs contain owner PII (names, addresses, telephones). The check MUST list every finding. It MUST NOT fix any finding in owner code; any PII hit is reported as a gap against spec 001 FR-011 and is not a pass condition for KPI-1.

- **FR-017** *(SPEC-DATA-1, ADR-DATA-2)*: `db/mysql/schema.sql` MUST run unchanged on an empty `mysql:8.4` database and create the seven tables (`vets`, `specialties`, `vet_specialties`, `types`, `owners`, `pets`, `visits`) with the keys, foreign keys and indexes as defined in the schema. The rehearsal MUST record `SHOW WARNINGS` for each `CREATE TABLE`; MySQL warning 1681 (INT display width) is expected and listed; no other warning or error MUST appear.

- **FR-018** *(SPEC-DATA-4, SPEC-DATA-8)*: Referential integrity MUST hold after the seed and after the owner create and update flows. Orphan counts for `pets.owner_id`, `pets.type_id`, `visits.pet_id` and `vet_specialties` MUST be 0. No owner row MUST have a `last_name`, `first_name`, `address` or `city` exceeding its validation limit (30, 30, 255, 80 characters respectively), or a `telephone` that is not exactly 10 digits.

- **FR-019** *(SPEC-DATA-5, SPEC-DATA-10)*: A seed parity check MUST compare the H2 and MySQL seeds by natural key for every column the owner pages render (owner fields, pet name/birth_date/type/owner, visit pet/date/description). The required result is 0 differences. Any difference MUST be listed with table, key, H2 value and MySQL value, and MUST fail the run. This check MUST run before the golden-file replay.

- **FR-020** *(SPEC-DATA-6, ADR-DATA-4)*: The rehearsal MUST record `@@collation_server`, `@@character_set_server`, the collation of `owners.last_name`, and the contents of `./conf.d` (or that the directory is absent). The expected collation is `utf8mb4_0900_ai_ci`; any other value reopens ADR-DATA-4. The accented-name probe (e.g. 'Rodríquez') MUST be recorded as a named accepted deviation outside the 0-difference count.

---

### Key Entities *(feature involves data)*

- **Owner**: The pet's registered guardian. Key fields: `id` (auto-generated, integer), `first_name` (max 30 chars), `last_name` (max 30 chars), `address` (max 255 chars), `city` (max 80 chars), `telephone` (exactly 10 digits). Sensitive fields: name, address, telephone. Stored in the `owners` table with `PRIMARY KEY(id)` and an index on `last_name`. The next id after the seed is 11.

- **Pet**: A pet registered under exactly one Owner. Key fields: `id`, `name`, `birth_date`, `type` (PetType), `owner` (Owner). Stored in the `pets` table with `UNIQUE(owner_id, name)` and foreign keys to `owners` and `types`. 13 pets in the seed.

- **Visit**: An immutable veterinary encounter for a Pet. Key fields: `id`, `pet` (Pet), `visit_date`, `description`. Stored in the `visits` table with a foreign key to `pets`. 4 visits in the seed. Visit records MUST NOT be deleted once created.

- **PetType**: Species/category classification of a Pet (e.g., dog, cat, bird). 6 types in the seed. Referenced by `pets.type_id`.

- **MySQL Seed (db/mysql/data.sql)**: The SQL seed file for the MySQL profile. After this change, the 13 `pets.birth_date` values and 4 `visits.visit_date` values are aligned to `db/h2/data.sql`. All other content is unchanged.

- **Owner Contract Recording**: The golden-file set of owner route interactions recorded on the H2 default profile before any change. Serves as the KPI-3 reference for the replay comparison.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **KPI-1** — App starts against local MySQL with 0 startup errors: The count of ERROR-level startup log entries and failed startup exceptions on a cold start from an empty `petclinic-mysql` volume is **0**. Baseline: captured before work starts by cold-starting the current app on H2 and counting ERROR lines. Target: 0 on MySQL 8.4.

- **KPI-2** — Owner create, search and update each succeed: The count of owner operations that succeed out of 3 (create, search, update), checked through the owner UI, is **3 of 3**. Baseline: captured before work starts by running all three flows on the current H2 app and recording results. Target: 3 of 3 on MySQL 8.4.

- **KPI-3** — 0 differences in the owner contract before and after: The count of differences between the before-change owner route recording (H2) and the after-change replay (MySQL 8.4) is **0**, covering URLs, status codes, redirects, form field names, validation messages, rendered content and page order for all recorded cases. Baseline: the before-change recording itself. Target: 0 differences.

- **SC-001**: All four owner test suites (`OwnerControllerTests`, `ClinicServiceTests`, `PetClinicConcurrencyTests`, `MySqlIntegrationTests`) pass with **0 failures and 0 skipped** when run against MySQL 8.4 (where applicable), and also on H2 for the suites that run on both profiles.

- **SC-002**: The seed parity check reports **0 differences** between the H2 and MySQL seeds for all 17 date values and all owner-page-rendered columns.

- **SC-003**: The diff of `db/mysql/data.sql` touches **exactly 17 values** (13 `pets.birth_date`, 4 `visits.visit_date`) and no other content. The diff of `db/mysql/schema.sql` is **empty**. The diff of all owner module Java files and templates is **empty**.

- **SC-004**: The credentials and PII check reports that the repository contains **only** the local POC credentials (`petclinic`/`petclinic`/`petclinic` and the empty root password) and lists every startup or test log line that contains owner PII (target: 0 PII hits, reported as a gap if any are found).

---

## Assumptions

- The local Docker MySQL 8.4 container (`petclinic-mysql`, `localhost:3306`, database/user/password `petclinic`) is the only MySQL target for this POC. No other MySQL instance, version or environment is in scope.
- The database starts empty on each fresh-volume run; no production data, no pre-existing owner data and no externally provided data set is loaded. The app's own `spring.sql.init` (schema.sql then data.sql) is the sole data source.
- The `SPRING_PROFILES_ACTIVE=mysql` profile and the `MYSQL_URL`, `MYSQL_USER` and `MYSQL_PASS` environment variables are the only mechanism used to switch the app to MySQL. No Java code change is required or permitted to achieve this.
- The five existing indexed baseline specs (001–005) are used as they are. Spec 001 (Owners) is the functional baseline for owner behaviour. No baseline spec is regenerated as part of this change.
- The before-change recording (KPI-3 reference) is taken on the current app running on the H2 default profile with the H2 seed. The 'before' and 'after' are compared across profiles, not across deployments.
- The seed date alignment (Option A, decided by the product owner) is the only permitted change to `db/mysql/data.sql`. No other seed content, schema, Java code or template change is made.
- `OwnerControllerTests` uses a mocked `OwnerRepository` and has no database dependency; it runs once on the default profile and is not run against MySQL.
- Each MySQL verification run group starts on a fresh volume to avoid `AUTO_INCREMENT` drift from rolled-back inserts and state left by `PetClinicConcurrencyTests`.
- The accented-name probe (e.g. 'Rodríquez') is a named accepted deviation outside the 0-difference contract set, confirmed by the product owner.
- No JSON owner API exists in the current application; the owner contract is defined entirely by the HTML routes. If a JSON owner API is discovered during recording, it is included in the contract set.
- The `./conf.d` directory contents of the MySQL container could not be verified from the available context; this is recorded as a blocking open issue for the platform track (see SPEC-DATA-6, ADR-DATA-4).
- Delivery follows the Jira project PETC and the Synapse coding agent workflow. Branches follow the `shiftforge/owners-to-mysql-2c9b9e7c` naming convention. This spec does not govern the CI/CD pipeline configuration beyond what is stated in the functional requirements.
- The Spring PetClinic constitution (v1.0.0, unratified draft) is treated as the authoritative governance baseline for this change. All requirements in this spec are consistent with its Core Principles, in particular: no owner Java code is modified (Principle I, II), the mysql profile is used as designed (Principle III), and owner PII handling is checked but not altered (Principle V).
