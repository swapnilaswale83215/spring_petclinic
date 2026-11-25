# Design: transformation-request-java-17-5d6e9213

## Application track

Rework for the specification-engineer's 26 uncovered items. Every spec now cites, in traces_to, the Brief acceptance criteria (AC-1 to AC-15) it satisfies, and each AC is cited by at least one spec. Spec ids, ADRs, components, KPI coverage and all other content are unchanged. No new committed test is added.

| Component | Change | Responsibility |
|---|---|---|
| pom.xml (java.version, enforcer, build-info) | changed | Change `<java.version>` 17 to 21 only. The enforcer `requireJavaVersion` and the build-info `java.source`/`java.target` read it. The Boot parent stays 4.1.0 and the plugin versions are unchanged (checkstyle 12.3.1 stays). |
| build.gradle (toolchain) | changed | Change `JavaLanguageVersion.of(17)` to `of(21)`. The Boot plugin stays 4.1.0 and nothing else changes. |
| .github/workflows/maven-build.yml | changed | Change matrix `java: ['17']` to `['21']`. Keep `distribution: 'adopt'` and `cache: maven`. This is the gating build and the Docker-capable evidence runner (ubuntu-latest). |
| .github/workflows/gradle-build.yml | changed | Change matrix `java: ['17']` to `['21']`. Keep `distribution: 'adopt'`. `cache: maven` is not a Java pin and stays. The run is reported and does not gate. |
| .devcontainer/Dockerfile (Gitpod image) | changed | Three pin lines only: `ARG VARIANT=21-bullseye`, `FROM mcr.microsoft.com/vscode/devcontainers/java:1-${VARIANT}` (prefix 0- to 1-), and `ARG JAVA_VERSION=<JDK 21 sdkman id>`. The id is recorded from `sdk list java` in the 1-21-bullseye image before the edit. No other line changes. |
| .coderig/harness.yaml and .coderig/sensors.json (sensor images) | changed | Change 4 image pins from `maven:3.9-eclipse-temurin-17` to `maven:3.9-eclipse-temurin-21`: the `format` and `tests` sensors, in each of the two files. They are set in the Launchpad editor and regenerated, never edited by hand. |
| coderig-gate.yml | unchanged | Runs `coderig gate` on every PR. The `blocked-scope` gate is blocking and will see the `.github/workflows/**` edits. It is not edited. Its outcome is recorded and needs a release-manager decision. |
| .gitpod.yml, .devcontainer/devcontainer.json, deploy/k8s/compose | unchanged | Left alone by constraint. `.gitpod.yml` points at the Dockerfile and picks up the new JDK with no edit. `devcontainer.json` already pins Java through the 21-oracle feature. |
| Deployed runtime (JDK on the deploy target) | unchanged | No deploy file for the runtime JDK is in the repository or in scope. The deploy target must provide JDK 21 before the release is authorized. Redeploying on 17 is the rollback. |
| Docker-capable evidence environment | new | The ubuntu-latest runner of maven-build.yml (preferred) or local Docker Desktop with the host name recorded. It needs a Docker daemon (Testcontainers for MySqlIntegrationTests) and Docker Compose with the existing compose file and its `postgres` service (PostgresIntegrationTests uses spring.docker.compose.*). The compose file is read, not edited. It runs the JDK 17 baseline and the JDK 21 verification so both classes actually execute. |

**Specifications**

### SPEC-PLAT-1

- Module: pom.xml and build.gradle Java pins
- Statement: `<java.version>` in pom.xml and `JavaLanguageVersion` in build.gradle are 21. The Boot version and every other line are unchanged.
- Accepted when: `./mvnw validate` on JDK 21 passes the enforcer rule `requireJavaVersion` with `<java.version>` 21, `META-INF/build-info.properties` shows `build.java.source=21` and `build.java.target=21`, build.gradle toolchain reads `of(21)`, and `org.springframework.boot` is still `4.1.0` in build.gradle and the pom parent, The diff of both files contains only the Java pin lines
- Traces to: [KPI-1](brief.md#kpi-1), [KPI-3](brief.md#kpi-3), [ADR-PLAT-1](design.md#adr-plat-1), [ADR-PLAT-7](design.md#adr-plat-7), [AC-1](brief.md#ac-1), [AC-3](brief.md#ac-3)

### SPEC-PLAT-2

- Module: .github/workflows/maven-build.yml
- Statement: The Maven workflow builds and verifies on JDK 21 with the same distribution and the same command.
- Accepted when: The matrix reads `java: [ '21' ]`, `distribution: 'adopt'`, `cache: maven` and `./mvnw -B verify` are unchanged, The run on the PR is green and its log shows JDK 21, The edit is a commit by the named human (ADR-PLAT-2), and the workflow diff is one line
- Traces to: [KPI-2](brief.md#kpi-2), [KPI-3](brief.md#kpi-3), [ADR-PLAT-2](design.md#adr-plat-2), [ADR-PLAT-9](design.md#adr-plat-9), [AC-2](brief.md#ac-2)

### SPEC-PLAT-3

- Module: .github/workflows/gradle-build.yml
- Statement: The Gradle workflow runs on JDK 21 and reports its result without gating the merge.
- Accepted when: The matrix reads `java: [ '21' ]`; `cache: maven` and `distribution: 'adopt'` are unchanged, and the workflow diff is one line, The Gradle run URL and exit code are recorded in the PR evidence, Unverified fact, owner repo admin: whether `gradle-build` is in the required status checks of main is listed in the PR body as 'unverified' until the repo admin confirms it in writing with the date, If the admin has not confirmed by merge time, SPEC-PLAT-15 applies (Gradle must be green on 21 or the PR does not merge), Branch protection and the Gradle workflow logic are not edited by this change
- Traces to: [KPI-5](brief.md#kpi-5), [KPI-3](brief.md#kpi-3), [ADR-PLAT-6](design.md#adr-plat-6), [AC-5](brief.md#ac-5)

### SPEC-PLAT-4

- Module: .devcontainer/Dockerfile
- Statement: The Gitpod image uses a JDK 21 base and a verified JDK 21 sdkman id, with no other change.
- Accepted when: The diff is exactly three lines: `ARG VARIANT=21-bullseye`, the FROM tag `java:1-${VARIANT}`, and `ARG JAVA_VERSION=<id>`, The id appears in the `sdk list java` output recorded from `1-21-bullseye` before the edit, `docker build` of the file succeeds and `java -version` in the image reports 21, `.gitpod.yml`, `devcontainer.json` and README are not in the diff
- Traces to: [KPI-3](brief.md#kpi-3), [ADR-PLAT-4](design.md#adr-plat-4), [AC-3](brief.md#ac-3)

### SPEC-PLAT-5

- Module: .coderig sensor images
- Statement: All 4 sensor image tags are `maven:3.9-eclipse-temurin-21`, set through the Launchpad editor so harness.yaml and sensors.json agree.
- Accepted when: `docker manifest inspect maven:3.9-eclipse-temurin-21` succeeds before the change, An anchored check finds 0 `temurin-17` occurrences and 4 `temurin-21` occurrences across the two files, The sensor definitions in harness.yaml and sensors.json are identical apart from the image tag, The `format` and `tests` sensors run on the 21 image (`python .coderig/verify.py --only format --only tests`) and pass, Order: the images are set in the Launchpad editor first, then swapnilaswale83215 commits the regenerated files to the PR branch (or another named person with Launchpad editor rights, recorded in the PR body), then the pom moves in the same PR. A drift check compares the live editor values with the committed files at the final gate.
- Traces to: [KPI-3](brief.md#kpi-3), [ADR-PLAT-3](design.md#adr-plat-3), [AC-3](brief.md#ac-3)

### SPEC-PLAT-6

- Module: Atomic change and pin check
- Statement: The change is one PR in which every in-scope pin moves together and nothing outside the allow-list changes.
- Accepted when: The anchored pin check on main after `git pull --ff-only origin main` (sha recorded) finds all in-scope pins at 17 (positive control), On the branch the same check finds 0 in-scope pins at 17. The pin lines changed are 11 in 7 files (pom 1, build.gradle 1, workflows 1 each, Dockerfile 3, sensors 4)., In .coderig/* the only semantic change is the 4 image tags. Any other line the Launchpad editor regenerates (for example the pack version reference, meta fields or root path) is listed in the PR body and accepted by the approver, and does not count as a pin failure., The PR file list is only pom.xml, build.gradle, maven-build.yml, gradle-build.yml, .devcontainer/Dockerfile, .coderig/harness.yaml and .coderig/sensors.json. coderig-gate.yml, .gitpod.yml, devcontainer.json, deploy/k8s/compose, README and PetClinicRuntimeHints.java are not in the diff, and nothing under src/main/java/**/owner, src/main/java/**/vet, src/main/resources/templates or src/main/resources/db is in the diff., The PR is squash-merged into one commit on main
- Traces to: [KPI-3](brief.md#kpi-3), [KPI-8](brief.md#kpi-8), [ADR-PLAT-1](design.md#adr-plat-1), AOT/native runtime hints stay unchanged, [AC-3](brief.md#ac-3), [AC-14](brief.md#ac-14)

### SPEC-PLAT-7

- Module: Docker-capable baseline and verification
- Statement: The JDK 17 baseline and the JDK 21 verification run on a Docker-capable environment (Docker daemon and Docker Compose) so the MySQL and PostgreSQL integration tests execute. 'Executed' means tests run minus skipped.
- Accepted when: The baseline records the sha after `git pull --ff-only origin main`, the exit code, and the run, passed, failed and skipped counts on JDK 17, The JDK 21 run has 0 failures and 0 errors. Executed (run minus skipped) on 21 is >= executed on the baseline, and skipped on 21 is <= skipped on the baseline. Both counts are recorded, and MySqlIntegrationTests and PostgresIntegrationTests are executed., The environment has a Docker daemon, Testcontainers for MySQL, and Docker Compose with the existing compose file and its `postgres` service. The compose file is untouched. The host (ubuntu-latest runner or the named Docker Desktop host) is recorded in the gate evidence., Without Docker, both classes are listed 'not run (no Docker)', not counted as passed, and the MySQL/PostgreSQL criteria are stated as not verified, No schema, migration or data file is in the diff
- Traces to: [KPI-4](brief.md#kpi-4), [KPI-2](brief.md#kpi-2), H2, MySQL and PostgreSQL profiles with no schema or data change, Existing tests: 0 failures/errors, executed count not below the JDK 17 baseline, [ADR-PLAT-5](design.md#adr-plat-5), [AC-2](brief.md#ac-2), [AC-4](brief.md#ac-4), [AC-10](brief.md#ac-10)

### SPEC-PLAT-8

- Module: Boot 4.1.0 support check
- Statement: Boot is verified to be 4.1.0 and in open-source support on the check date and again on the merge commit.
- Accepted when: The PR body records the check date (the final gate evidence date), the Spring Boot support policy source and the open-source end date, The end date is after the check date. The second read is made by the release manager on the PR head commit just before merge, and its date is recorded., The parent version in pom.xml and the plugin version in build.gradle read 4.1.0 on both dates, If the end date is on or before the check date, KPI-6 is marked failed, a separate Boot-upgrade track is raised, and no bump is in the diff, If a failure is found only after the merge, the Boot-upgrade track starts, not the rollback, unless the approver decides otherwise
- Traces to: [KPI-6](brief.md#kpi-6), [ADR-PLAT-7](design.md#adr-plat-7), [AC-6](brief.md#ac-6)

### SPEC-PLAT-9

- Module: Startup and runtime-level observability
- Statement: A cold start on JDK 21 with the default H2 profile proves the runtime level and health, and the evidence is recorded for close-out.
- Accepted when: GET / returns 200 and `/actuator/health` reports UP after one cold start on JDK 21, The startup log line 'Starting PetClinicApplication ... using Java 21' (or the JVM version line) is captured in the evidence, `/actuator/info` build properties show `java.source` and `java.target` 21 where the endpoint is exposed, The same values are captured from the JDK 17 baseline startup so close-out can compare them
- Traces to: [KPI-7](brief.md#kpi-7), [KPI-1](brief.md#kpi-1), Startup on the default H2 profile, [AC-7](brief.md#ac-7), [AC-1](brief.md#ac-1)

### SPEC-PLAT-10

- Module: Rollback
- Statement: The change is reversible by one revert, a rebuild on 17 and a redeploy, with no data restore.
- Accepted when: On a branch, one `git revert` of the squash commit returns every pin to 17: pom, build.gradle, both workflows, the Dockerfile and the harness files, `./mvnw clean verify` on JDK 17 passes after the revert, 0 data restores are performed and 0 schema or data files are in the revert, The 4 sensor images are reset to 17 in the Launchpad editor and match the reverted files, The elapsed time of the rehearsal (revert, CI build on 17) is recorded; the deploy time is added from the deploy pipeline
- Traces to: [KPI-8](brief.md#kpi-8), [ADR-PLAT-8](design.md#adr-plat-8), [AC-8](brief.md#ac-8)

### SPEC-PLAT-11

- Module: Release authorization and gate outcomes
- Statement: The release is authorized by a named human after the evidence is complete. The run and the agent do not merge or deploy.
- Accepted when: The PR body has the four required sections (What changed, Why, How tested, Risks and rollback plan) and the 'Gate evidence / exception' section of SPEC-PLAT-14, The branch name, every commit message (Conventional Commits with a ticket id) and the plan-commit gate meet the blocking harness rules. The profile branch pattern `shiftforge/{transformation_id}` differs from the rule `git-branch-naming` pattern `type/issue-id-short-description`. The release manager states which pattern applies, and the branch name that passes the local dry run of SPEC-PLAT-14 is recorded in the PR body., The PR is approved and merged by a human, never by the agent, The deploy pipeline runs only after that approval, The run and the agent do not merge, deploy or edit branch protection
- Traces to: [ADR-PLAT-2](design.md#adr-plat-2), [ADR-PLAT-1](design.md#adr-plat-1), [KPI-3](brief.md#kpi-3)

### SPEC-PLAT-12

- Module: Runtime target readiness
- Statement: The deploy target provides a JDK 21 runtime before the release is authorized, and its JDK is not changed by files in this change.
- Accepted when: Open item: the deploy-target JDK is not known from this repository. deploy-and-test-cluster.yml runs only on k8s/** and deploys the external image dsyer/petclinic. The named owner is the platform/ops owner of the k8s demo deploy and of that image. The release manager supplies the person's name before the architecture approval, because the repository does not give it. The owner states in writing, with the date, the JDK of the target and that it can run JDK 21., The same named owner confirms in writing that a JDK 17 rebuild can be redeployed. The approver explicitly accepts that deploy time is not covered by the rollback rehearsal, which covers revert and CI build only, so KPI-8 is shown end to end only after this confirmation., No file under deploy/k8s/compose appears in the diff, After the deploy, health UP and the startup log show Java 21 on the target, The previous JDK 17 runtime stays available until the rollback window closes
- Traces to: [KPI-7](brief.md#kpi-7), [KPI-8](brief.md#kpi-8), [ADR-PLAT-8](design.md#adr-plat-8), [AC-8](brief.md#ac-8), [AC-7](brief.md#ac-7)

### SPEC-PLAT-13

- Module: CI evidence for close-out
- Statement: The PR evidence carries the CI facts that close-out reads for the KPIs measured in CI.
- Accepted when: The final maven-build.yml run URL, exit code and surefire/failsafe totals (executed, failures, errors, skipped) are recorded for KPI-2 and KPI-4, The gradle-build.yml run URL and exit code are recorded for KPI-5, The check date and the pin check output are recorded, A failing maven-build.yml run on main after merge is treated as an alert: the revert in SPEC-PLAT-10 starts
- Traces to: [KPI-2](brief.md#kpi-2), [KPI-4](brief.md#kpi-4), [KPI-5](brief.md#kpi-5), [KPI-3](brief.md#kpi-3), [AC-2](brief.md#ac-2), [AC-4](brief.md#ac-4), [AC-5](brief.md#ac-5)

### SPEC-PLAT-14

- Module: coderig blocked-scope gate: pass condition
- Statement: The coderig `blocked-scope` gate is treated as required and is never bypassed. The PR passes it only through a named one-PR exception for pin-only edits to maven-build.yml and gradle-build.yml, with the two edits made by a named person.
- Accepted when: The PR body 'Gate evidence / exception' section names: the scope (pin-only edits to maven-build.yml and gradle-build.yml, this PR only), the exception author swapnilaswale83215, and the approver Sandeep Paithankar. The approver's name is confirmed in writing before merge, and the approver is not the author., Dry run before the PR is opened: swapnilaswale83215 runs `coderig gate blocked-scope --base <main sha> --branch <branch> --pr-body <file>` locally with the draft exception text. The result and the exact PR-body wording the gate accepts are recorded in the PR body. The repo admin confirms that the repository variable CODERIG_PIP_SPEC is set, because coderig-gate.yml exits 1 without it., The dry run also shows whether the regenerated .coderig/* files trip `no-ci-edits`. If they do, the release manager and approver decide, in writing, whether to list them in the exception. This is not assumed., Both workflow diffs are one changed line (the `java` matrix value 17 to 21). The blocked list, guard.py and coderig-gate.yml are not in the diff. Both workflow commits are authored by swapnilaswale83215 and none by the agent synapse, with Conventional Commit messages that carry a ticket id., PASS condition: the `coderig-gate` run on the PR head ends with the job `gate` successful, including `coderig gate blocked-scope`, and its run URL is in the PR body. A merge without this, or with a bypass or admin override, fails this spec. If the gate still fails, the PR is not merged. The red result and run URL are recorded, and the case goes to the release manager and approver. The blocked list, guard.py and coderig-gate.yml stay unchanged, and main stays green because the move is one atomic PR.
- Traces to: [KPI-3](brief.md#kpi-3), [KPI-2](brief.md#kpi-2), [ADR-PLAT-2](design.md#adr-plat-2), [ADR-PLAT-1](design.md#adr-plat-1), Release manager answer 1-3, [AC-3](brief.md#ac-3)

### SPEC-PLAT-15

- Module: Gradle required-check status and KPI-5 fallback
- Statement: Whether gradle-build.yml is a required check is recorded as an unverified fact for the repo admin to confirm. A fallback keeps KPI-5 true in either case.
- Accepted when: The admin's dated written answer on whether `gradle-build` is a required check is obtained before the first JDK 21 run where possible, so the fallback is not needed. Until then the PR body lists 'Gradle required check in branch protection: unverified', with the repo admin as owner., The Gradle build is run on JDK 17 on main (baseline) and on JDK 21 on the branch before merge. Both exit codes and run URLs are recorded., If Gradle is green on 21, KPI-5 holds whether or not the check is required, If Gradle is red on 21 and was green on 17, it is treated as a JDK 21 failure. Only a proven fix in build.gradle with the failing log cited is allowed. Otherwise the approval states explicitly that, until the admin confirms the check is not required, the PR waits. Gradle then acts as a de facto gate, which departs from the brief ('does not gate'), and the approver accepts that departure in writing., If Gradle was already red on 17, that is recorded as a pre-existing failure and the exit code is still reported. Branch protection and the Gradle workflow logic are not edited.
- Traces to: [KPI-5](brief.md#kpi-5), [ADR-PLAT-6](design.md#adr-plat-6), [SPEC-PLAT-3](design.md#spec-plat-3), [AC-5](brief.md#ac-5)

### SPEC-PLAT-16

- Module: Must-not-break: pet create and edit
- Statement: Pet create and edit, including the duplicate-name and future-birth-date checks, behave as on JDK 17. The existing tests prove it, and none is added.
- Accepted when: On JDK 21 the existing pet test classes (PetControllerTests, PetValidatorTests, ClinicServiceTests as named in the Assessment) appear in the surefire report with 0 failures and 0 errors. The class names are checked against the report on main, and the baseline run lists the same classes., The duplicate-name and future-birth-date cases are in those reports as passed, No Pet, PetController, PetValidator or PetTypeFormatter file and no new test file is in the diff
- Traces to: Pet create and edit (AC-9), [KPI-4](brief.md#kpi-4), [SPEC-PLAT-7](design.md#spec-plat-7), [AC-9](brief.md#ac-9)

### SPEC-PLAT-17

- Module: Must-not-break: owner register, edit, search
- Statement: Owner register, edit and search behave as on JDK 17. The existing tests prove only the blank or missing telephone cases (@NotBlank), the valid case and the last-name search. The @Pattern(\d{10}) rule is proved by a recorded, non-committed manual check, or the approver accepts the gap.
- Accepted when: On JDK 21 OwnerControllerTests, OwnerTests and PetClinicConcurrencyTests run with 0 failures and 0 errors, and the same classes ran in the 17 baseline, Real existing coverage: processCreationFormHasErrors (missing telephone), processUpdateOwnerFormHasErrors (blank telephone), processCreationFormSuccess (valid owner) and ClinicServiceTests.shouldFindOwnersByLastName (search). These exercise @NotBlank, the valid case and the search only. OwnerTests covers addPet and ValidatorTests covers Person names, so no existing test sends a non-blank malformed telephone., Recorded manual check, non-committed, on a cold start on 21 (default H2) and on the 17 baseline: submit the owner form with telephone '12345' and 'abcdefghij', and expect rejection with the telephone validation error. Submit '1316761638' and expect it saved and found by last-name search. Status codes, messages and the person who ran it are recorded in the PR evidence. The test owners live only in in-memory H2., If the manual check is not run, the approver explicitly accepts 'telephone @Pattern rule not verified' in the PR body, and AC-11 is not claimed as fully verified, No Owner, OwnerController or OwnerRepository file and no new test file is in the diff
- Traces to: Owner register, edit and search (AC-11), [KPI-4](brief.md#kpi-4), [SPEC-PLAT-7](design.md#spec-plat-7), [AC-11](brief.md#ac-11)

### SPEC-PLAT-18

- Module: Must-not-break: visit booking
- Statement: Visit booking and the visit-date rule behave as on JDK 17, proved by the existing tests.
- Accepted when: On JDK 21 VisitControllerTests (named in the Assessment) runs with 0 failures and 0 errors and was in the 17 baseline, The existing assertions show the boundary of the current VisitController code: a date that is not after today is rejected, and a later date is accepted. The boundary is read from the code and not changed., No Visit or VisitController file is in the diff
- Traces to: Visit booking (AC-12), [KPI-4](brief.md#kpi-4), [SPEC-PLAT-7](design.md#spec-plat-7), [AC-12](brief.md#ac-12)

### SPEC-PLAT-19

- Module: Must-not-break: vet list, JSON and cache
- Statement: The vet list and JSON resource behave as on JDK 17. The existing tests prove only that those paths run. The vets cache has two entries, one for findAll(Pageable) behind GET /vets.html and one for the no-argument findAll() behind GET /vets. It is verified for both endpoints by a recorded, non-committed check, or the approver explicitly accepts 'vets cache not verified'.
- Accepted when: VetControllerTests (VetRepository mocked with @MockitoBean, so the cache is bypassed) and VetTests run on JDK 21 with 0 failures and 0 errors and were in the 17 baseline. They prove the list and JSON paths run, not the cache., MySqlIntegrationTests, PostgresIntegrationTests and PetClinicIntegrationTests call vets.findAll() twice with only the comment '// served from cache' and assert nothing, and ClinicServiceTests.shouldFindVets is a plain @DataJpaTest read. They prove findAll does not throw. They do not prove caching, and the PR does not say they do., Recorded cache check, non-committed, run by swapnilaswale83215 (a named person; if someone else runs it, that name is recorded in the PR evidence). The two endpoints are checked separately, because they hit different cache entries: GET /vets.html (VetRepository.findAll(Pageable), which loads a Page) and GET /vets (JSON, no-argument findAll()). On JDK 17 (baseline) and on JDK 21, start the app cold with SQL logging enabled at run time only (for example logging.level.sql=DEBUG on the command line). Use a fresh cold start per endpoint, so one endpoint does not warm the other. Call each endpoint twice, giving four calls per JDK. For each call, count the vet-related SQL statements (the vet page select, any count query and any specialty selects). Pass, for each endpoint separately: the first call issues N vet-related statements, the second call issues 0, and N on JDK 21 equals N on JDK 17. N is read from the 17 baseline and recorded. It is not assumed to be 1. The command, the log excerpts, the per-call counts for both endpoints on both JDKs and the person who ran it are recorded in the PR evidence., If the check is not run, the approver explicitly accepts 'vets cache not verified' in the PR body, and the vets-cache must-not-break item (AC-13) is not claimed as verified, No Vet, VetController, VetRepository or CacheConfiguration file, no new test file and no committed logging setting is in the diff
- Traces to: Vet list, JSON resource and vets cache (AC-13), [KPI-4](brief.md#kpi-4), [SPEC-PLAT-7](design.md#spec-plat-7), [AC-13](brief.md#ac-13)

### SPEC-PLAT-20

- Module: Must-not-break: H2, MySQL and PostgreSQL profiles
- Statement: The three data profiles run on JDK 21 with no schema or data change. The MySQL and PostgreSQL profiles are claimed verified only when their tests ran.
- Accepted when: H2: the default-profile start and tests pass on 21 (SPEC-PLAT-9), MySQL and PostgreSQL: MySqlIntegrationTests and PostgresIntegrationTests ran on a Docker-capable host with the host recorded, and passed. Without Docker they are 'not run (no Docker)' and the criteria are not claimed (SPEC-PLAT-7)., No file under src/main/resources/db or application-mysql/postgres.properties is in the diff
- Traces to: H2, MySQL and PostgreSQL profiles (AC-10), [KPI-4](brief.md#kpi-4), [SPEC-PLAT-7](design.md#spec-plat-7), [ADR-PLAT-5](design.md#adr-plat-5), [AC-10](brief.md#ac-10)

### SPEC-PLAT-21

- Module: Must-not-break: AOT/native runtime hints
- Statement: PetClinicRuntimeHints is unchanged. The proof is the diff check, and a process-aot run on 21 is reported evidence only.
- Accepted when: PetClinicRuntimeHints.java is not in the diff and still registers BaseEntity, Person and Vet, The PR states that the diff check is the acceptance. `./mvnw clean verify` has no AOT step, so AOT behaviour on 21 is not otherwise tested., An optional `./mvnw process-aot` run on 21 is recorded as reported, not required
- Traces to: AOT/native runtime hints stay unchanged (AC-14), [SPEC-PLAT-6](design.md#spec-plat-6), [AC-14](brief.md#ac-14)

### SPEC-PLAT-22

- Module: Must-not-break: owner records
- Statement: Owner records (name, address, telephone) are neither changed nor exposed differently by this change.
- Accepted when: No owner entity, repository, controller or template file and no new field or endpoint is in the diff, The owner tests of SPEC-PLAT-17 pass on 21 with the same expected values as on the 17 baseline, `secret-scan` is run by swapnilaswale83215 with `python .coderig/verify.py --only secret-scan` on the PR branch. The exit code is recorded in the PR body., `pii-exposure` is an inferential Launchpad review, run by a person with Launchpad editor rights on the PR diff. The verdict is recorded in the PR body. CI runs only `coderig gate`, so it does not run these sensors., If a sensor cannot be run, the PR body says so and the diff check ('no owner file in the diff') is the only evidence for this spec
- Traces to: Owner records not exposed differently (AC-15), [SPEC-PLAT-6](design.md#spec-plat-6), [SPEC-PLAT-17](design.md#spec-plat-17), [AC-15](brief.md#ac-15)

### SPEC-PLAT-23

- Module: Inventory: no other file pins Java 17
- Statement: The pin check runs over a repo-wide inventory of 17 pins on main, so that no pin outside the 7 files exists.
- Accepted when: Evidence already read: deploy-and-test-cluster.yml runs only on k8s/**, creates a kind cluster and runs kubectl apply, with no setup-java or JDK version. coderig-gate.yml sets up only Python 3.12. harness.yaml has `temurin-17` only in the `format` and `tests` sensor images. devcontainer.json pins Java through the 21-oracle feature, and .gitpod.yml only points at the Dockerfile (product owner answer 1)., A repo-wide search on main (after `git pull --ff-only origin main`, sha recorded) for `17`, `temurin-17`, `VERSION_17`, `release 17`, `JavaLanguageVersion`, `java.version`, `.sdkmanrc`, `.tool-versions` and `JAVA_VERSION` lists every hit as in scope, excluded with a reason (README, devcontainer.json, .gitpod.yml, deploy/k8s/compose), or a conflict raised to the brief owner, The enumeration of the same 7 files that retrieval returned rests on the deleted, unverified spec 001, so it is not counted as proof. The search above is the proof., The anchored pin check of SPEC-PLAT-6 runs over this inventory, with the patterns recorded in the PR
- Traces to: [KPI-3](brief.md#kpi-3), [SPEC-PLAT-6](design.md#spec-plat-6), [ADR-PLAT-1](design.md#adr-plat-1), [AC-3](brief.md#ac-3)

**Decisions**

### ADR-PLAT-1

- Title: One atomic PR, squash-merged, all pins together
- Decision: All 11 pin lines in 7 files move in one PR from one branch, squash-merged into one commit on main. The pom is not moved before the workflows and harness.
- Rationale: The enforcer fails a build whose JDK is below `java.version`, and `maven-build.yml` runs on every push to main. A partial move turns main red. The harness already requires squash-merge and linear history on main, so one commit gives a one-step revert (KPI-3, KPI-8).
- Alternatives: Staged PRs per file: the pom at 21 with the CI at 17 breaks the enforcer., CI first and pom last: this is the only safe order, but it gives two commits to revert and an intermediate state to test.

### ADR-PLAT-2

- Title: Workflow pin edits are made by a named human, not by an agent
- Decision: The two `.github/workflows/maven-build.yml` and `gradle-build.yml` Java pin edits are covered by a named, one-PR exception recorded in the PR body (section 'Gate evidence / exception'). The exception author is swapnilaswale83215, a person, who makes both edits as commits on the PR branch. The approver is a different person, Sandeep Paithankar, whose name is confirmed before merge. The exception covers pin-only edits to those two files. The blocked list, guard.py and `coderig-gate.yml` are unchanged. The `coderig-gate` workflow stays enabled and is treated as required, and it is never bypassed. The coding agent (synapse) does not edit `.github/workflows/**`. Before the PR is opened, `coderig gate blocked-scope` is run locally with the draft exception text, and the accepted wording is recorded. The same dry run shows whether the regenerated `.coderig/*` files, which the rule `no-ci-edits` also names, trip the gate. If they do, the release manager and approver decide whether to add them to the exception. This is not assumed. The pass condition and the fallback are in SPEC-PLAT-14.
- Rationale: `boundaries.blocked` lists `.github/workflows/**` ('CI config defines the gates; agents must not edit it'), rule `no-ci-edits` is blocking, and the `blocked-scope` gate is `blocking: true` and runs on every PR (coderig-gate.yml triggers on `pull_request` and passes `--pr-body`). The brief requires both pins to move, so the release manager chose a recorded exception over weakening the guardrails. A different approver keeps the author from approving their own exception. The gate's logic lives in the CodeRig package, not in the repository. Whether a human-authored commit passes it is therefore unproven and is handled by SPEC-PLAT-14.
- Alternatives: Let the agent edit the workflows: this violates a blocking rule and the gate., Remove the blocked scope in harness.yaml: this weakens the guardrails and edits the CodeRig configuration., Merge over a red gate, or use an admin override: the gate is treated as required and is never bypassed.

### ADR-PLAT-3

- Title: Harness sensor images are set in the Launchpad editor and both files regenerated
- Decision: The 4 sensor image tags (`format` and `tests` in harness.yaml and in sensors.json) are changed to `maven:3.9-eclipse-temurin-21` in the Launchpad harness editor, which regenerates both files. Neither file is edited by hand. Before the change, `docker manifest inspect maven:3.9-eclipse-temurin-21` must succeed. After it, a check confirms 0 occurrences of `temurin-17` in both files and that the `format` and `tests` sensors run.
- Rationale: The config is launchpad-first, and `no-ci-edits` covers the CodeRig configuration. The two files carry identical sensor definitions, so a hand edit of one risks drift. The tests sensor excludes MySqlIntegrationTests and PostgresIntegrationTests, so it is not evidence for the MySQL and PostgreSQL profiles.
- Alternatives: Hand-edit both files: drift risk and a breach of the constraint., Leave the sensors on 17: this breaks KPI-3, and sensors would check the code on a JDK the build no longer targets.

### ADR-PLAT-4

- Title: Dockerfile: three pin lines, with the sdkman id verified before the edit
- Decision: Edit only `ARG VARIANT=21-bullseye`, the FROM tag prefix `0-` to `1-` (`java:1-${VARIANT}`), and `ARG JAVA_VERSION=<id>`. The id is copied from `sdk list java` run in `mcr.microsoft.com/vscode/devcontainers/java:1-21-bullseye`. It is recorded in the PR before editing, and it keeps the existing `-ms` vendor family if that family lists a JDK 21. The image is built once with `docker build` as the rehearsal. `.gitpod.yml` and `devcontainer.json` are untouched.
- Rationale: No workflow builds this image, so a wrong id breaks Gitpod without turning CI red. The file already runs `sdk install java $JAVA_VERSION`, so the id must be a real candidate. `0-21-bullseye` does not exist and `1-21-bullseye` does, per the brief. Keeping the same vendor family avoids a distribution change.
- Alternatives: Guess the id from the old format (21.0.x-ms): unverified., Add a CI job to build the image: this edits workflows beyond the pin and is out of scope.

### ADR-PLAT-5

- Title: Baseline and verification run on a Docker-capable environment
- Decision: The JDK 17 baseline (after `git pull --ff-only origin main`, sha recorded) and the JDK 21 verification both run on the ubuntu-latest runner of maven-build.yml. Local Docker Desktop is accepted if CI evidence is unavailable, with the host name recorded. The environment needs a Docker daemon (Testcontainers for MySQL) and Docker Compose with the untouched compose file (the PostgreSQL test uses a compose service named `postgres`). Without Docker, MySqlIntegrationTests and PostgresIntegrationTests are reported 'not run (no Docker)', never counted as passed, and the MySQL and PostgreSQL criteria are not claimed as verified. 'Executed' means tests run minus skipped. It is compared like for like (Docker with Docker), and skipped on 21 must not exceed skipped on the baseline.
- Rationale: MySqlIntegrationTests has `@Testcontainers(disabledWithoutDocker = true)` and PostgresIntegrationTests uses `assumeTrue(isDockerAvailable())`. Without Docker the build is green while the two profiles are unverified and the baseline is understated. The product owner decided this.
- Alternatives: Run only the harness tests sensor: it excludes both classes and runs in a container with no Docker., Run without Docker and count the skips as passed: this violates the brief.

### ADR-PLAT-6

- Title: Gradle build is reported, not gating
- Decision: gradle-build.yml runs on 21 on every PR with no gating change, and its exit code is copied into the PR evidence. Whether it is a required status check in branch protection is an unverified fact. The release manager does not know it, and the repo admin confirms it (Settings, Branches or Rulesets). No workflow edit beyond the pin is made, and branch protection is never edited by the run. The fallback is SPEC-PLAT-15: run Gradle on 21 before merge, so KPI-5 holds whether or not the check is required.
- Rationale: The brief says Gradle 'runs on 21 and is reported; it does not gate'. maven-build.yml is the gate. Making Gradle a gate, or adding workflow logic, is out of scope, and a workflow edit beyond the pin is a further blocked-scope change.
- Alternatives: Make Gradle a required check: out of scope., Add `continue-on-error: true`: an extra workflow edit that hides failures instead of reporting them.

### ADR-PLAT-7

- Title: Boot 4.1.0 support is checked at the final gate evidence date and on the merge commit
- Decision: Boot stays 4.1.0 (pom parent and the build.gradle plugin). The check date is the date of the final gate evidence. The Spring Boot support policy page is read on that date, and the date and end date are written in the PR body. The second read is made on the PR head commit just before merge, run by the release manager, because a read 'on the merge commit' can only happen after the merge. If the end date is on or before the check date, KPI-6 fails and a separate Boot-upgrade track is raised. A failure found after the merge starts the Boot-upgrade track, not the rollback, unless the approver decides otherwise. Boot is never bumped inside this change.
- Rationale: The product owner agreed this. The support end date is not established in the repository, and a silent bump would break the 'Boot stays 4.1.0' constraint and the 'no dependency bump' constraint.
- Alternatives: Bump Boot if support is short: out of scope and breaks must-not-break assumptions., Check once at the start: the date can pass before the merge.

### ADR-PLAT-8

- Title: Rollback is revert, rebuild on 17, redeploy, with no data restore
- Decision: Rollback is: revert the one squash commit on main, let maven-build.yml rebuild on 17, redeploy the rebuilt artifact on a JDK 17 runtime, and reset the 4 sensor images to 17 in the Launchpad editor so the live harness matches the reverted files. No data restore is needed. It is rehearsed once on a branch before merge. The JDK 21 build is never redeployed on 17.
- Rationale: A JDK 21 build compiled at `java.version` 21 produces class files that JDK 17 cannot run, so the artifact must be rebuilt. No schema or data file changes, so no restore is needed. The harness is launchpad-first, so a git revert alone can be overwritten by the next regeneration.
- Alternatives: Hot-patch the pom on main: breaks the single-revert guarantee., Keep the 21 artifact and run it on 17: it will not run.

### ADR-PLAT-9

- Title: CI distribution stays 'adopt'; a failure there is escalated, not worked around
- Decision: `distribution: 'adopt'` stays in both workflows with `actions/setup-java@v4` and only the version moves. If the first 21 run cannot resolve JDK 21 under `adopt`, the run stops and goes back to the brief owner, because a distribution change is out of scope.
- Rationale: 'No distribution change in CI (adopt stays)' is an agreed constraint. Whether `adopt` resolves a JDK 21 is not shown in the repository, so the first CI run is the proof and no workaround is assumed.
- Alternatives: Switch to temurin: breaks the constraint without agreement., Use a local JDK 21 only: skips the evidence that CI can build.
