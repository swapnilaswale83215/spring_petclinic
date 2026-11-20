# Spring PetClinic Constitution

## Core Principles

### I. Modular Monolith Architecture
The application MUST remain a modular monolith with server-side rendering via Thymeleaf. Controllers MUST return model attributes consumed directly by Thymeleaf templates; the application MUST NOT be refactored into a pure REST API without explicit ratification of this constitution. Package boundaries (`owner`, `vet`, `system`, `model`) MUST be respected: cross-package dependencies MUST flow only through well-defined interfaces, never by reaching directly into another package's internal classes. The `system` package MUST contain only infrastructure and configuration concerns; domain logic MUST NOT reside there.

### II. Domain Integrity and Business Rule Enforcement
All six business rules from the domain model MUST be enforced at all times and MUST NOT be relaxed without a ratified amendment:

- A Pet MUST always belong to exactly one Owner.
- A Visit MUST always be associated with exactly one Pet.
- A Pet MUST have a valid PetType at registration.
- An Owner MUST have a valid telephone number and address before registration is complete.
- Veterinarian specialty assignments are optional (zero or many per vet).
- Visit records MUST NOT be deleted once created; visit history MUST only be appended to.

Validation MUST be enforced via Jakarta Bean Validation annotations embedded directly in domain classes. Separate standalone validator classes MUST NOT be introduced for constraints already expressible as Bean Validation annotations. The `PetValidator` pattern (Spring `Validator` interface) is permitted only for multi-field or cross-entity validation that cannot be expressed as a single Bean Validation annotation.

### III. Profile-Aware Persistence
The application MUST support profile-based database selection: H2 (default/test), MySQL, and PostgreSQL. Database schema MUST be managed via Spring Data JPA/Hibernate DDL or versioned migration scripts, with the active strategy determined by the active Spring profile. Code MUST NOT hardcode a specific database dialect or assume H2 semantics in production paths. The default H2 in-memory profile MUST be treated as a development and testing convenience only; it MUST NOT be used to validate production data volume or persistence behavior. Integration tests targeting MySQL and PostgreSQL MUST be maintained (see `MySqlIntegrationTests`, `PostgresIntegrationTests`) and MUST pass in CI before any persistence-layer change is merged.

### IV. Service-Layer Caching Contract
Veterinarian lookups MUST be served through the caching layer configured in `CacheConfiguration`. Any new query path that retrieves the full veterinarian list MUST use the established `@Cacheable` annotation on `VetRepository`. Cache configuration changes MUST be accompanied by a test that verifies cache hit/miss behavior. Controllers MUST NOT bypass the service/repository caching layer by issuing direct, uncached queries for data that is already covered by the cache contract.

### V. Sensitive Data Handling
Owner records — specifically the `name`, `address`, and `telephone number` fields — are designated sensitive zones. Code that reads, logs, serializes, or transmits these fields MUST apply appropriate access controls and MUST NOT expose them in unprotected endpoints, debug logs, or error responses. Structured logging MUST redact or omit Owner PII fields. No feature MUST introduce a new exposure path for these fields without explicit review and sign-off documented in the pull request.

### VI. Bounded Scope Enforcement
The application MUST remain within the defined in-scope boundary. The following capabilities are explicitly out of scope and MUST NOT be implemented in this repository:

- Billing and invoicing
- Payment processing
- Inventory and pharmacy management
- Insurance claims processing
- Multi-clinic or multi-location management

Any pull request that introduces functionality in these areas MUST be rejected at review, regardless of implementation quality.

### VII. Internationalisation Completeness
All user-facing strings rendered in Thymeleaf templates MUST be externalised to i18n message property files. Hard-coded display strings in HTML MUST NOT be introduced. Every message key added to the base properties file MUST be translated in all supported language files before the change is merged. The `I18nPropertiesSyncTest` MUST remain green; it MUST NOT be disabled or deleted.

---

## Domain Model

### Entities

| Entity | Aliases / Notes |
|---|---|
| Owner | The pet's registered guardian; customer of the clinic. Sensitive fields: name, address, telephone number. |
| Pet | A pet registered under exactly one Owner. |
| Visit | A recorded veterinary appointment/encounter for a pet. Immutable once created. |
| Veterinarian | A vet employed at the clinic. Abbreviated as "Vet". |
| PetType | Species/category classification of a pet (e.g., dog, cat, bird, hamster). |
| Specialty | A veterinarian's area of medical expertise (e.g., radiology, surgery, dentistry). |

### Relationships

- Owner OWNS Pet (one-to-many; a Pet MUST belong to exactly one Owner)
- Pet HAS Visit (one-to-many; a Visit MUST be associated with exactly one Pet)
- Visit ASSIGNED_TO Veterinarian
- Pet HAS PetType (a Pet MUST have a valid PetType at registration)
- Veterinarian HAS Specialty (zero or many Specialties per Veterinarian)

### Processes

1. Owner Registration
2. Pet Registration
3. Veterinarian Directory Management
4. Visit Scheduling
5. Visit History Tracking

### Vocabulary

| Term | Definition |
|---|---|
| Vet | Abbreviation for Veterinarian |
| Clinic | The single veterinary practice location modelled by this application |

---

## Technology Stack and Dependency Constraints

The following dependencies define the approved technology baseline. Additions or removals MUST be reviewed and ratified:

| Dependency | Role |
|---|---|
| Spring Boot (Spring MVC + Thymeleaf) | Web framework and server-side rendering |
| Spring Data JPA | Repository interfaces; implementations MUST NOT be written by hand where auto-generation suffices |
| H2 / MySQL / PostgreSQL | Profile-based persistent storage |
| Jakarta Bean Validation | Domain-level constraint enforcement |
| Spring Boot Actuator + Micrometer | Health checks and metrics exposure |
| GitHub Actions CI | Automated build and test pipeline |
| Maven Wrapper / Gradle Wrapper | Build tooling; both wrappers MUST remain bundled in the repository |
| Docker Compose | Containerised local runs |
| Kubernetes manifests (`k8s/`) | Deployment descriptors; MUST be kept in sync with application configuration changes |

Spring Data JPA repository interfaces MUST be used for all data access; explicit DAO implementation classes MUST NOT be introduced. No ORM framework other than Hibernate (via Spring Data JPA) MUST be added without a ratified amendment.

---

## Development Workflow and Quality Gates

### Automated Testing Requirements

Every pull request MUST pass all of the following before merge:

1. **Unit tests** — Controller slice tests (`@WebMvcTest`) and domain/validator unit tests MUST cover all new or modified controller mappings and validation rules.
2. **Service integration tests** — `ClinicServiceTests` MUST be extended to cover any new repository or service behaviour.
3. **Database integration tests** — Changes to the persistence layer MUST be validated against both MySQL (`MySqlIntegrationTests`) and PostgreSQL (`PostgresIntegrationTests`) profiles via Testcontainers.
4. **Concurrency tests** — Changes to shared mutable state or caching MUST not regress `PetClinicConcurrencyTests`.
5. **i18n sync test** — `I18nPropertiesSyncTest` MUST remain green; all new UI strings MUST be fully translated.
6. **Full application integration test** — `PetClinicIntegrationTests` MUST pass against the default H2 profile.

Test coverage MUST not decrease for the `owner`, `vet`, and `model` packages as measured by the CI pipeline.

### Code Review Requirements

All pull requests MUST receive at least one approving review from a maintainer before merge. Reviewers MUST explicitly verify:

- Compliance with all Core Principles in this constitution.
- No out-of-scope functionality has been introduced.
- Sensitive Owner fields are not exposed in new code paths.
- Business rules are not weakened or bypassed.
- No hard-coded UI strings have been introduced without i18n keys.

### CI/CD Pipeline

The GitHub Actions CI pipeline MUST run on every pull request and every push to the main branch. The pipeline MUST include: compilation, unit tests, integration tests (H2 profile minimum), static analysis, and Docker image build verification. Merges to the main branch MUST NOT occur if any CI stage is failing.

### Observability Standards

Spring Boot Actuator endpoints MUST remain enabled for health and metrics. Micrometer metrics MUST be preserved for all existing instrumentation points. New service-layer operations of significant cost (e.g., bulk queries, cache-bypassing lookups) MUST be instrumented with a Micrometer timer or counter before the change is merged.

---

## Governance

This constitution is the authoritative specification for the `swapnilaswale83215/spring_petclinic` repository. It supersedes all informal conventions, README guidance, and prior undocumented practices wherever they conflict.

**Compliance**: All pull requests and code reviews MUST verify compliance with this constitution. Non-compliant changes MUST be rejected regardless of functional correctness.

**Amendments**: Any amendment to this constitution MUST include: (a) a written description of the change and its rationale, (b) identification of any Core Principle, Domain Model entry, or Governance rule being modified, (c) a migration plan if existing code must be updated, and (d) explicit human ratification by a designated maintainer before the amendment takes effect.

**Human Ratification Required**: This document is an unratified draft. It MUST NOT be treated as non-negotiable until it has been reviewed and ratified by the project maintainer(s). Until ratified, it serves as a strong baseline recommendation.

**Out-of-Scope Clause**: Any contribution that implements billing/invoicing, payment processing, inventory/pharmacy management, insurance claims processing, or multi-clinic/multi-location management MUST be rejected at review. This clause MUST NOT be amended without a full ratification cycle.

**Conflict Resolution**: In the event of a conflict between this constitution and a source file, the constitution governs intent; the source file governs current implementation. Discrepancies MUST be resolved by either updating the code to comply or raising a formal amendment.

---

**Version**: 1.0.0 | **Status**: Unratified draft | **Generated**: 2026-11-10 | **Last Amended**: 2026-11-10