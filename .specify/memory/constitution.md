# Spring PetClinic Constitution

## Core Principles

### I. Modular Monolith Architecture
The application MUST remain a modular monolith with server-side rendering via Thymeleaf. Controllers MUST return model attributes consumed directly by Thymeleaf templates; the application MUST NOT be refactored into a pure REST API without an explicit architectural decision and ratification. Package boundaries (`owner`, `vet`, `system`) MUST be respected: cross-package dependencies MUST go through defined interfaces, not by reaching directly into another package's internal classes. The caching layer applied at the service layer for veterinarian lookups MUST be maintained via `CacheConfiguration` and `@Cacheable` on `VetRepository`; controllers MUST NOT bypass this cache by calling the repository directly.

### II. Business-Rule Integrity
All domain invariants derived from the clinic's operational rules MUST be enforced at the earliest possible layer (domain model or validator) and MUST NOT be silently bypassed by any controller, service, or test fixture:
- A `Pet` MUST always belong to exactly one `Owner`.
- A `Visit` MUST always be associated with exactly one `Pet`.
- A `Pet` MUST have a valid `PetType` at registration.
- An `Owner` MUST have a valid telephone number and address before registration is complete.
- Veterinarian specialty assignments are optional (zero or many per vet); code MUST NOT enforce a minimum specialty count.
- Visit records MUST NOT be deleted once created; the persistence layer MUST only support append operations on `Visit` entities. No `delete` or `remove` method MUST be exposed on any visit repository or service.

### III. Validation Strategy
Validation MUST rely on Jakarta Bean Validation annotations embedded directly in domain classes (`@NotBlank`, `@NotNull`, etc.) rather than in separate standalone validator classes, except where Spring's `Validator` interface is required for multi-field or cross-field rules (e.g., `PetValidator`). New domain fields MUST carry appropriate Bean Validation constraints at the point of declaration. Validation MUST be exercised by `ValidatorTests` and the relevant controller slice tests before a change is merged.

### IV. Profile-Aware Persistence
The application MUST support three database profiles: H2 (default, in-memory), MySQL, and PostgreSQL. Schema management MUST be handled via Spring Data JPA/Hibernate DDL or versioned scripts appropriate to the active profile. The H2 in-memory profile resets all data on restart and MUST NOT be used as evidence of production data volume or persistence behaviour. Integration tests targeting MySQL MUST use `MySqlIntegrationTests` (Testcontainers); integration tests targeting PostgreSQL MUST use `PostgresIntegrationTests`. No profile-specific SQL or DDL MUST be introduced without a corresponding migration or schema script for all three profiles.

### V. Sensitive Data Handling
Owner records — specifically the `name`, `address`, and `telephone number` fields — are designated sensitive zones. These fields MUST NOT be written to logs, metrics output, error responses, or any externally observable surface. Credentials, API keys, tokens, passwords, and connection strings MUST NEVER be written into source, config, tests, or fixtures; they MUST be read from the project's configured secret store. Any change touching `Owner` persistence, serialisation, or display MUST be reviewed for inadvertent exposure of these fields.

### VI. Out-of-Scope Boundary Enforcement
The following capabilities are explicitly out of scope and MUST NOT be introduced into this codebase: billing or invoicing, payment processing, inventory or pharmacy management, insurance claims processing, and multi-clinic or multi-location management. Any pull request that introduces domain classes, controllers, views, or database schema supporting these capabilities MUST be rejected regardless of implementation quality.

### VII. Observability and Health
The application MUST expose health checks and metrics via Spring Boot Actuator and Micrometer. Actuator endpoints MUST remain enabled and MUST NOT be removed or disabled. New service-layer operations of significant cost (e.g., bulk queries, cache-affecting operations) MUST be instrumented or at minimum verifiable via existing Actuator metrics. Structured operational visibility MUST be preserved across all active profiles.

---

## Domain Model

### Entities
| Entity | Aliases | Description |
|---|---|---|
| `Owner` | Pet's registered guardian; customer of the clinic | Holds personal contact details (name, address, telephone) and owns one or more pets |
| `Pet` | — | A registered animal belonging to exactly one Owner, with a PetType and visit history |
| `Visit` | Veterinary appointment; encounter | A recorded veterinary encounter for a Pet; append-only |
| `Veterinarian` | Vet | A clinic staff member with zero or more Specialties |
| `PetType` | Species; category classification | Classification of a Pet (e.g., dog, cat, bird, hamster) |
| `Specialty` | Area of medical expertise | A Veterinarian's domain of practice (e.g., radiology, surgery, dentistry) |

### Relationships
- `Owner` OWNS one or more `Pet` entities; each `Pet` MUST belong to exactly one `Owner`.
- `Pet` HAS zero or more `Visit` records; each `Visit` MUST be associated with exactly one `Pet`.
- `Visit` MAY be ASSIGNED_TO a `Veterinarian`.
- `Pet` HAS exactly one `PetType`; this MUST be set at registration.
- `Veterinarian` HAS zero or more `Specialty` entries.

### Processes
- **Owner Registration**: Capture and persist Owner with mandatory telephone and address.
- **Pet Registration**: Associate a new Pet with an existing Owner; PetType MUST be selected.
- **Veterinarian Directory Management**: Add, update, and list Veterinarians and their Specialties.
- **Visit Scheduling**: Create a new Visit record for a Pet; existing Visit records MUST NOT be deleted.
- **Visit History Tracking**: Display the chronological list of Visit records for a Pet.

### Vocabulary
- **Vet**: Abbreviation for Veterinarian; used interchangeably in code and UI.
- **Clinic**: The single veterinary practice location modelled by this application. Multi-location support is out of scope.

---

## Technology Stack and Dependency Constraints

The following dependencies define the approved technology surface. Additions or removals MUST be justified in the pull request and approved by a human reviewer before merging.

| Concern | Approved Technology |
|---|---|
| Runtime framework | Spring Boot (latest project-pinned version) |
| Persistence | Spring Data JPA; Hibernate DDL or versioned scripts |
| Databases | H2 (dev/test), MySQL, PostgreSQL (profile-selected) |
| View layer | Thymeleaf (server-side rendering) |
| Validation | Jakarta Bean Validation; Spring `Validator` for cross-field rules |
| Observability | Spring Boot Actuator + Micrometer |
| Build | Maven Wrapper (`mvnw`) or Gradle Wrapper (`gradlew`); both MUST remain functional |
| CI | GitHub Actions; pipeline definitions MUST NOT be modified by automated agents |
| Containerisation | Docker; Docker Compose for local containerised runs; Kubernetes manifests in `k8s/` |
| Testing | JUnit 5, AssertJ, Mockito, Spring Boot Test, Testcontainers (MySQL, PostgreSQL) |
| Internationalisation | Spring i18n message properties; `I18nPropertiesSyncTest` MUST pass; no hard-coded UI strings |

Native image hints MUST be maintained in `PetClinicRuntimeHints` when new reflection-dependent types are introduced.

---

## Development Workflow and Quality Gates

### Branch and Commit Discipline
All changes MUST be made on a feature branch following the naming convention `type/issue-id-short-description` (e.g., `feat/PROJ-123-add-pet-weight`). Direct commits to `main` or `develop` are prohibited. Squash-merge is the default merge policy; linear history MUST be maintained on `main`. Every commit message MUST follow Conventional Commits: `type(scope): short description` and MUST reference the issue or ticket ID (e.g., `Closes #123`). Vague messages such as `fix`, `update`, or `WIP` are prohibited.

### Pull Request Requirements
Every pull request description MUST include all four of the following sections: **What changed**, **Why** (business or technical reason), **How tested** (manual steps or test names), and **Risks and rollback plan**. A pull request missing any section MUST NOT be merged. An automated agent MUST NOT merge its own pull request; a human reviewer MUST approve first.

### Planning Before Implementation
Before writing code, the implementer MUST capture the request, ask all clarifying questions at once, state assumptions, and produce a written plan. The plan MUST be committed first and await human approval. Implementation MUST NOT begin before that approval exists.

### Test Requirements
Tests MUST ship in the same change as the code they cover. Each change MUST cover: the happy path, at least one failure path, and any domain-rule boundary the change touches (e.g., the append-only Visit constraint, mandatory PetType, Owner telephone validation). Tests MUST NOT be disabled or deleted to make a build pass. The following test suites MUST remain green on every merge:
- Unit tests: `ValidatorTests`, `OwnerTests`, `VetTests`, `PetValidatorTests`, `PetTypeFormatterTests`
- Controller slice tests: `OwnerControllerTests`, `PetControllerTests`, `VisitControllerTests`, `VetControllerTests`, `WelcomeControllerTests`, `CrashControllerTests`
- Integration tests: `PetClinicIntegrationTests`, `CrashControllerIntegrationTests`, `ClinicServiceTests`
- Database integration tests: `MySqlIntegrationTests`, `PostgresIntegrationTests` (Testcontainers)
- Concurrency tests: `PetClinicConcurrencyTests`
- i18n sync test: `I18nPropertiesSyncTest`

### CI and Guard Rails
The GitHub Actions CI pipeline MUST run on every pull request. CI definitions under `.github/workflows/` MUST NOT be modified by automated agents. The CodeRig guard (`guard.py`) and checks (`checks.py`) MUST NOT be bypassed. The following paths MUST NOT be written to by any automated agent or pull request: `.github/workflows/**`, `**/.env`, `**/.env.*`, `**/auth/**`, `**/Auth/**`, `**/security/**`, `**/Security/**`, `**/*Authentication*`, `**/*Authorization*`, `**/*.pem`, `**/*.p12`, `**/*.pfx`, `**/id_rsa`, `**/credentials`.

### Scope Discipline
A change MUST only touch files within the scope of its approved plan. If a fix requires touching files outside the agreed scope, the implementer MUST stop and report this rather than widening the change unilaterally.

### Code Comment Policy
Comments MUST only be added when the *why* is non-obvious. Comments that explain *what* the code does MUST NOT be added; well-named identifiers serve that purpose.

---

## Governance

This constitution is the authoritative specification for the `swapnilaswale83215/spring_petclinic` repository. It supersedes any conflicting local conventions, README guidance, or informal practices.

**Amendments**: Any amendment MUST be proposed as a pull request modifying this document, accompanied by a written rationale, an impact assessment identifying affected code and tests, and a migration plan if existing behaviour must change. Amendments MUST be approved by at least one human maintainer before taking effect. The version number MUST be incremented and the Last Amended date updated on every ratified change.

**Ratification**: This document is an unratified draft. It MUST be reviewed and explicitly ratified by the project maintainer(s) before it is treated as non-negotiable. Until ratified, it serves as a strong working baseline but individual principles may be overridden by explicit maintainer decision.

**Compliance**: All pull requests and code reviews MUST verify compliance with this constitution. Any principle marked MUST is a hard gate; a change that violates it MUST NOT be merged. Complexity beyond what the domain requires MUST be justified in writing.

**Runtime Guidance**: Use `.coderig/guide.py` and the Launchpad workspace `proj-d9b4cf` for session-specific task guidance during development.

---

**Version**: 1.0.0 | **Status**: Unratified draft | **Generated**: 2026-11-24 | **Last Amended**: 2026-11-24