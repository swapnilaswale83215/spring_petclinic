**Generated-at**: 2026-11-25T06:46:03Z
**Baseline-commit**: unknown
# Spring PetClinic Constitution

## Core Principles

### I. Modular Monolith Architecture
The application MUST remain a single deployable unit structured as a modular monolith. Server-side rendering via Thymeleaf MUST remain tightly coupled to controller-returned model attributes; the application MUST NOT be refactored into a pure REST API without an explicit architectural decision and amendment to this constitution. Package boundaries (`owner`, `vet`, `system`) MUST be respected; cross-package dependencies MUST be introduced only through well-defined interfaces. The caching layer applied at the service layer for veterinarian lookups MUST be maintained in `CacheConfiguration` and MUST NOT be silently removed or bypassed.

### II. Business-Rule Integrity (NON-NEGOTIABLE)
All domain business rules derived from the clinic's operational model MUST be enforced at all times:
- A Pet MUST always belong to exactly one Owner.
- A Visit MUST always be associated with exactly one Pet.
- A Pet MUST have a valid PetType at registration.
- An Owner MUST have a valid telephone number and address before registration is complete.
- Veterinarian specialty assignments are optional (zero or many per vet); this MUST NOT be changed to a required constraint.
- Visit records MUST NOT be deleted once created; visit history MUST be append-only.

Validation MUST be enforced via Jakarta Bean Validation annotations embedded directly in domain classes. Separate standalone validator classes MUST NOT replace this mechanism unless this constitution is amended.

### III. Sensitive Data Handling
Owner records — specifically the `name`, `address`, and `telephone number` fields — are designated sensitive zones. Any code, query, log statement, test fixture, or configuration that reads, writes, displays, or transmits Owner PII MUST handle those fields with care: they MUST NOT be written to application logs, MUST NOT appear in error messages surfaced to the UI, and MUST NOT be stored in any location outside the configured persistent data store. Credentials and connection strings for the data store MUST be read from the project's configured secret store and MUST NOT be committed to source, config files, tests, or fixtures.

### IV. Profile-Based Database Strategy
The application MUST support profile-based database selection: H2 in-memory for development/test, MySQL and PostgreSQL for production-equivalent environments. The default H2 in-memory profile MUST be treated as non-representative of production data volume or persistence behavior; it MUST NOT be used as the basis for performance claims or data-migration decisions. Database schema MUST be managed via Spring Data JPA/Hibernate DDL or versioned scripts consistent with the active Spring profile. Any schema change MUST be validated against all three supported database profiles before merging.

### V. Bounded Scope Enforcement
The application scope MUST be limited to: Owner registration and management; Pet registration and record-keeping; Veterinarian directory and specialty management; Visit scheduling and visit history tracking; Basic clinic administration via web UI.

The following capabilities are explicitly out of scope and MUST NOT be introduced into this codebase:
- Billing or invoicing
- Payment processing
- Inventory or pharmacy management
- Insurance claims processing
- Multi-clinic or multi-location management

Any feature request touching an out-of-scope area MUST be rejected or redirected to a separate system. This clause MUST be checked during PR review.

### VI. Observability and Health
The application MUST expose health checks and metrics via Spring Boot Actuator and Micrometer. Actuator endpoints MUST remain enabled and MUST NOT be removed or disabled. Structured application events that affect clinic data (Owner creation, Pet registration, Visit creation) MUST be traceable through the Actuator metrics surface. Any new service-layer operation MUST be considered for metric instrumentation before merging.

---

## Domain Model

### Entities
| Entity | Aliases | Description |
|---|---|---|
| Owner | Pet's registered guardian; customer of the clinic | The person responsible for one or more pets registered at the clinic |
| Pet | — | An animal registered under an Owner with a designated PetType |
| Visit | Veterinary appointment; encounter | A recorded veterinary appointment for a Pet |
| Veterinarian | Vet | A medical professional listed in the clinic directory |
| PetType | Species/category classification | Classification of a pet (e.g., dog, cat, bird, hamster) |
| Specialty | Area of medical expertise | A veterinarian's area of expertise (e.g., radiology, surgery, dentistry) |

### Relationships
- Owner OWNS Pet (one-to-many; a Pet MUST belong to exactly one Owner)
- Pet HAS Visit (one-to-many; a Visit MUST be associated with exactly one Pet)
- Visit ASSIGNED_TO Veterinarian (many-to-one)
- Pet HAS PetType (many-to-one; PetType MUST be valid at registration)
- Veterinarian HAS Specialty (many-to-many; zero or many Specialties per Veterinarian)

### Processes
1. **Owner Registration** — Captures Owner name, address, and telephone number; all three MUST be present and valid before registration completes.
2. **Pet Registration** — Associates a Pet with an Owner and assigns a valid PetType.
3. **Veterinarian Directory Management** — Maintains the list of Vets and their optional Specialty assignments.
4. **Visit Scheduling** — Creates a new Visit record linked to a Pet; MUST be append-only.
5. **Visit History Tracking** — Retrieves the ordered history of Visits for a Pet; deletion MUST NOT be permitted.

### Vocabulary
- **Vet** — Abbreviation for Veterinarian; used interchangeably in code and UI.
- **Clinic** — The single veterinary practice location modeled by this application; multi-location concepts are out of scope.

---

## Technology Stack and Dependencies

The following dependencies are load-bearing and MUST NOT be removed without an architectural amendment:

| Dependency | Role |
|---|---|
| Spring Boot (Java 21+) | Application framework and runtime |
| Spring Data JPA | Repository interfaces; implementations MUST NOT be hand-written where auto-generation suffices |
| Thymeleaf | Server-side view rendering; MUST remain the sole templating engine |
| Jakarta Bean Validation | Domain-level validation; MUST be co-located with domain classes |
| H2 / MySQL / PostgreSQL | Profile-based persistent storage |
| Spring Boot Actuator + Micrometer | Health checks and metrics; MUST remain active |
| GitHub Actions CI | Automated build and test pipeline; pipeline definitions MUST NOT be modified by feature branches |
| Docker Compose | Containerized local runs; MUST remain functional for MySQL and PostgreSQL profiles |
| Maven Wrapper / Gradle Wrapper | Build tooling; wrapper scripts MUST be committed and kept current |

The following paths MUST NOT be modified by any automated agent or feature branch without explicit human approval:
- `.github/workflows/**`
- `**/security/**`, `**/Security/**`
- `**/*Authentication*`, `**/*Authorization*`
- `**/*.pem`, `**/*.p12`, `**/*.pfx`, `**/id_rsa`, `**/credentials`

---

## Development Workflow and Quality Gates

### Branching and Commits
- All changes MUST be made on a feature branch following the naming convention `type/issue-id-short-description` (e.g., `feat/PROJ-123-add-pet-birthdate-validation`).
- Direct commits to `main` or `develop` MUST NOT occur.
- Every commit message MUST follow Conventional Commits: `type(scope): short description` and MUST reference the issue or ticket ID (e.g., `Closes #123`).
- Squash-merge is the default policy; linear history MUST be maintained on `main`.

### Pull Requests
- Every PR description MUST include four sections: **What changed**, **Why** (business or technical reason), **How tested** (manual steps or test names), and **Risks and rollback plan**.
- A PR MUST NOT be merged without all four sections present.
- An automated agent MUST NOT merge its own PR; a human reviewer MUST approve first.
- Every PR MUST be verified against the out-of-scope clause (Principle V) before approval.

### Testing Requirements
- Tests MUST ship in the same change as the code they cover.
- Every change MUST cover: the happy path, at least one failure path, and any business-rule boundary the change touches (see Principle II).
- Integration tests MUST be provided for: changes to repository interfaces, changes to the caching configuration, schema migrations, and any new controller endpoint.
- Profile-specific integration tests (MySqlIntegrationTests, PostgresIntegrationTests) MUST remain green before any schema or JPA mapping change is merged.
- The I18nPropertiesSyncTest MUST remain green; any new UI string MUST be added to all supported locale property files.
- Concurrency tests (PetClinicConcurrencyTests) MUST remain green for any change touching shared mutable state.
- The H2 in-memory profile MUST be used for unit and slice tests; Testcontainers-backed MySQL/PostgreSQL profiles MUST be used for database integration tests.

### CI Pipeline
- The GitHub Actions CI pipeline MUST pass on all PRs before merge.
- No `--no-verify`, skip-hook flags, disabled failing tests, or lint suppressions for errors introduced by the change MUST be used.
- Force-push, rebase of a shared branch, amend of a pushed commit, and destructive git commands MUST NOT be executed without explicit human instruction in the current session.

### Code Quality
- Comments MUST only be added when the **why** is non-obvious; comments explaining **what** the code does MUST NOT be added.
- Change scope MUST be limited to the approved plan; any discovery requiring out-of-scope file changes MUST be surfaced to a human reviewer before proceeding.
- Before writing code, the implementer MUST capture the request, ask all clarifying questions at once, state assumptions, and produce a written plan. Implementation MUST NOT begin before human approval of the plan.

---

## Governance

This constitution is an **unratified draft** and MUST be ratified by a designated human maintainer before it is treated as non-negotiable. Until ratified, it serves as the authoritative working baseline for all development decisions on this repository.

Once ratified:
- This constitution supersedes all other development practices, README conventions, and informal agreements for this repository.
- All PRs and code reviews MUST verify compliance with every principle in this document before approval.
- Any violation of a Core Principle MUST block merge; violations MUST be documented in the PR and resolved, not suppressed.

**Amendments**:
- Any amendment MUST include: a description of the change, the business or technical justification, a migration plan for existing code affected by the change, and approval from at least one designated maintainer.
- Amendments to Principle II (Business-Rule Integrity) and Principle V (Bounded Scope Enforcement) MUST additionally include sign-off from the product owner or domain SME.
- The version number MUST be incremented (MINOR for additive changes, MAJOR for breaking changes to existing principles) and the Last Amended date MUST be updated on every amendment.
- Complexity MUST be justified against YAGNI; no new dependency or architectural layer MUST be introduced without a documented rationale in the amendment.

For runtime development guidance, consult `.coderig/guide.py` and the CodeRig Launchpad workspace `proj-d9b4cf`.

---

**Version**: 1.0.0 | **Status**: Unratified draft | **Generated**: 2026-11-25 | **Last Amended**: 2026-11-25

**Status**: Needs ratification in the baseline pull request review.
