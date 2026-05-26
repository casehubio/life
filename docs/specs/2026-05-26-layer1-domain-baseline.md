# Layer 1 — Domain Baseline Spec

**Issue:** casehubio/life#2  
**Branch:** `issue-2-layer1-naive-domain`  
**Date:** 2026-05-26  
**Deferred to later layers:** life#10 (actorId string for ledger), life#11 (trust scores), life#12 (DTO layer)

---

## Purpose

Layer 1 establishes the casehub-life domain model with production-quality code and no
foundation integration. REST resources are operational. Tests demonstrate domain correctness
and — via `ShowcaseScenarioTest` — make accountability gaps visible as observable state.

Gap commentary lives in `LAYER-LOG.md`, not in production code.

---

## Module Structure

```
api/    io.casehub.life.api.*
        Pure Java. Zero framework imports. Zero JPA. Zero foundation deps.
        Domain vocabulary: enums, constants. No SPIs, no @DefaultBean/@Alternative.

app/    io.casehub.life.app.*
        Quarkus 3.32.2. Panache Active Record. JPA. Flyway. REST.
        Direct Panache — no Store SPI. This absence is the Layer 1 gap.
```

The Store SPI pattern (prescribed by `module-tier-structure.md`) is intentionally absent.
Layer 2+ grows into it. The DTO layer (hexagonal protocol) arrives with the Store SPI in
Layer 2 (tracked: life#12).

---

## api/ Module

### `LifeDomain` enum

```
HOUSEHOLD, HEALTH, FINANCE, FAMILY_SCHEDULING, TRAVEL,
LEGAL, CONTRACTOR_COORDINATION, ELDER_CARE
```

### `LifeCapabilities` — String constants

```java
HOUSEHOLD_MANAGEMENT   = "household-management"
HEALTH_COORDINATION    = "health-coordination"
FINANCIAL_PLANNING     = "financial-planning"
FAMILY_SCHEDULING      = "family-scheduling"
TRAVEL_PLANNING        = "travel-planning"
LEGAL_DEADLINE         = "legal-deadline"
CONTRACTOR_COORDINATION = "contractor-coordination"
ELDER_CARE             = "elder-care"
```

### `LifeTrustDimensions` — String constants

```java
DEADLINE_RELIABILITY  = "deadline-reliability"
COST_ACCURACY         = "cost-accuracy"
FACTUAL_ACCURACY      = "factual-accuracy"
PROACTIVE_ALERTING    = "proactive-alerting"
```

Trust dimension scoring is Layer 6 (tracked: life#11). These constants define the
vocabulary for later layers; no scores are stored in Layer 1.

### `LifeActorType` enum

```
AI_AGENT, HOUSEHOLD_PRINCIPAL, EXTERNAL_HUMAN
```

Named `LifeActorType` — not `ActorType` — to avoid collision with
`io.casehub.platform.api.identity.ActorType` (HUMAN, AGENT, SYSTEM), which will be on
the classpath once foundation modules are integrated.

### `model` package

| Type | Kind | Values |
|---|---|---|
| `HouseholdTaskStatus` | enum | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `LifeGoalStatus` | enum | `ACTIVE`, `PAUSED`, `COMPLETED`, `ABANDONED` |

---

## app/ Module — Entities

All entities: `PanacheEntityBase`, UUID PK assigned in `@PrePersist`, `@PrePersist`
timestamps. Enum columns stored as `STRING`. No `@ManyToOne` — cross-entity references
use raw UUID columns (clinical pattern; consistent with future Store SPI introduction).

### `ExternalActor`

Table: `external_actor`

| Field | Java type | Column | Constraints |
|---|---|---|---|
| `id` | `UUID` | `id` | PK, NOT NULL |
| `name` | `String` | `name` | NOT NULL |
| `actorType` | `LifeActorType` | `actor_type` | NOT NULL, STRING enum |
| `contactMethod` | `String` | `contact_method` | NOT NULL — "email", "phone", "sms" |
| `contactValue` | `String` | `contact_value` | NOT NULL — address or number |
| `createdAt` | `Instant` | `created_at` | NOT NULL, set in @PrePersist |

No `trustDimensions` — deferred to Layer 6 (life#11).  
No dedicated `actorId` String — `id.toString()` serves as the platform actor reference
string for future ledger integration; string convention designed in Layer 4 (life#10).

### `HouseholdTask`

Table: `household_task`

| Field | Java type | Column | Constraints |
|---|---|---|---|
| `id` | `UUID` | `id` | PK, NOT NULL |
| `domain` | `LifeDomain` | `domain` | NOT NULL, STRING enum |
| `title` | `String` | `title` | NOT NULL |
| `description` | `String` | `description` | nullable |
| `deadline` | `Instant` | `deadline` | nullable |
| `slaHours` | `Integer` | `sla_hours` | nullable — stored, unenforced until Layer 2 |
| `status` | `HouseholdTaskStatus` | `status` | NOT NULL, default PENDING |
| `assignedTo` | `String` | `assigned_to` | nullable — opaque household principal string |
| `externalActorId` | `UUID` | `external_actor_id` | nullable — raw UUID ref to ExternalActor; no DB FK constraint |
| `recurrence` | `String` | `recurrence` | nullable — stored recurrence rule (unenforced until Layer 5) |
| `createdAt` | `Instant` | `created_at` | NOT NULL |
| `updatedAt` | `Instant` | `updated_at` | NOT NULL, refreshed in @PreUpdate |

**`assignedTo` semantics:** opaque string identifying a household principal (e.g. `"alice"`).
Follows `WorkItem.assigneeId` platform pattern — no FK to a HouseholdMember entity.

**`externalActorId` semantics:** typed reference to a registered `ExternalActor`. Nullable
because not all tasks involve an external actor. No `@ManyToOne` — raw UUID column.
Service layer refuses `ExternalActor` deletion when tasks reference it (GE-20260428-096e90).

**`slaHours` semantics:** domain fact — how many hours this task class should take. Stored
in Layer 1 even though unenforced: domain-correct modelling precedes enforcement. Layer 2
computes `WorkItem.claimDeadline = createdAt + slaHours` when creating the WorkItem for
SLA tracking.

**`recurrence` semantics:** nullable free-text or RRULE string. Stored in Layer 1 so the
domain model is complete; evaluation/triggering arrives with the CasePlanModel in Layer 5.

### `LifeGoal`

Table: `life_goal`

| Field | Java type | Column | Constraints |
|---|---|---|---|
| `id` | `UUID` | `id` | PK, NOT NULL |
| `domain` | `LifeDomain` | `domain` | NOT NULL |
| `title` | `String` | `title` | NOT NULL |
| `description` | `String` | `description` | nullable |
| `targetDate` | `Instant` | `target_date` | nullable |
| `status` | `LifeGoalStatus` | `status` | NOT NULL, default ACTIVE |
| `createdAt` | `Instant` | `created_at` | NOT NULL |
| `updatedAt` | `Instant` | `updated_at` | NOT NULL |

### `LifeEvent`

Table: `life_event`

| Field | Java type | Column | Constraints |
|---|---|---|---|
| `id` | `UUID` | `id` | PK, NOT NULL |
| `domain` | `LifeDomain` | `domain` | NOT NULL |
| `title` | `String` | `title` | NOT NULL |
| `description` | `String` | `description` | nullable |
| `occurredAt` | `Instant` | `occurred_at` | NOT NULL |
| `createdAt` | `Instant` | `created_at` | NOT NULL |

Events are facts — no `updatedAt`, no `PUT` endpoint.

---

## Flyway Migrations

Location: `app/src/main/resources/db/migration/`  
Range: V100–V199 (casehub-work: V1–V21+; ledger: V1000+).

All DDL must be ANSI SQL — no `DOUBLE`, `SERIAL`, or H2-only types
(GE-20260512-2c2eff: non-ANSI types pass H2 silently, fail Postgres at deployment).

| Version | File | Table |
|---|---|---|
| V100 | `V100__create_external_actor.sql` | `external_actor` |
| V101 | `V101__create_household_task.sql` | `household_task` |
| V102 | `V102__create_life_goal.sql` | `life_goal` |
| V103 | `V103__create_life_event.sql` | `life_event` |

`external_actor` is created before `household_task` — logical ordering for readability
and to enable a future FK constraint addition without reordering migrations. No DB-level
FK in Layer 1.

---

## REST API

Auth-retrofit-readiness applies throughout: thin resources, no auth or principal types in
service or domain layers, injectable filter seam on all list endpoints for future
`@RolesAllowed` / tenancy scoping.

Layer 1 REST resources return JPA entities directly — no DTO layer. DTO records in `api/`
arrive with the Store SPI pattern in Layer 2 (tracked: life#12).

### `HouseholdTaskResource` — `/household-tasks`

| Method | Path | Query params | Response |
|---|---|---|---|
| POST | `/household-tasks` | — | 201 + entity |
| GET | `/household-tasks` | `domain`, `status`, `assignedTo`, `externalActorId` (all optional) | 200 list |
| GET | `/household-tasks/{id}` | — | 200 or 404 |
| PUT | `/household-tasks/{id}` | — | 200 or 404 |
| DELETE | `/household-tasks/{id}` | — | 204 or 404 |

### `ExternalActorResource` — `/external-actors`

| Method | Path | Query params | Response |
|---|---|---|---|
| POST | `/external-actors` | — | 201 + entity |
| GET | `/external-actors` | `actorType` (optional) | 200 list |
| GET | `/external-actors/{id}` | — | 200 or 404 |
| PUT | `/external-actors/{id}` | — | 200 or 404 |
| DELETE | `/external-actors/{id}` | — | 204, 404, or 409 if tasks reference actor |
| GET | `/external-actors/{id}/tasks` | `status` (optional) | 200 list of HouseholdTask |

The `/tasks` sub-resource is the primary showcase query path: makes the contractor
accountability gap queryable and visible.

### `LifeGoalResource` — `/life-goals`

CRUD: POST, GET (list with `domain`/`status` filters), GET by id, PUT, DELETE.

### `LifeEventResource` — `/life-events`

POST, GET (list with `domain` filter), GET by id, DELETE. No PUT — events are immutable facts.

---

## Service Layer

Four `@ApplicationScoped` beans in `app/`:

- `HouseholdTaskService` — wraps Panache; list method accepts filter parameters
- `ExternalActorService` — wraps Panache; `delete()` checks for referencing tasks, throws
  409-mapped exception if found (GE-20260428-096e90: without CASCADE, orphan refs must
  be prevented at service layer)
- `LifeGoalService` — wraps Panache
- `LifeEventService` — wraps Panache

All services are intentionally thin in Layer 1. The absence of SLA enforcement,
escalation, commitment tracking, and audit calls in `HouseholdTaskService.create()` is
the gap that Layer 2–4 fill.

---

## Tests

### Pure-Java unit tests (`api/` module)

| Class | What |
|---|---|
| `HouseholdTaskStatusTest` | enum values present and stable |
| `LifeDomainTest` | enum values present and stable |
| `LifeCapabilitiesTest` | all constants non-null, non-blank, no duplicates |

Only stateless domain logic. `api/` has no behaviour beyond constants and enums —
do not manufacture tests.

### REST integration tests (`app/` module — `@QuarkusTest`)

Named `*ResourceTest.java` — not `*IT.java` (GE-20260512-493c90: failsafe silently
collects `*IT.java`; zero tests reported by surefire). UUID-suffix business keys in test
data (GE-20260508-ce2285: prevents H2 shared-state conflicts between test classes).

| Class | Covers |
|---|---|
| `HouseholdTaskResourceTest` | CRUD happy path; 404 on missing; list filters (domain, status, assignedTo, externalActorId) |
| `ExternalActorResourceTest` | CRUD happy path; 404 on missing; 409 on delete with referencing tasks; `/tasks` sub-resource |
| `LifeGoalResourceTest` | CRUD happy path; list filters (domain, status) |
| `LifeEventResourceTest` | create, get, list, delete; no PUT |

### `ShowcaseScenarioTest` (`@QuarkusTest`)

`@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` — ordered narrative, state carries
between methods intentionally. Tests a complete household week.

| Order | Method | Scenario step |
|---|---|---|
| 1 | `contractorTaskCreated` | Create ExternalActor (Bob's Plumbing, EXTERNAL_HUMAN, contactMethod=phone). Create HouseholdTask (fix boiler, CONTRACTOR_COORDINATION, deadline=Friday+, slaHours=48, externalActorId→Bob). Assert task persisted and linked via `/external-actors/{id}/tasks`. |
| 2 | `contractorDeadlinePassedNoEscalation` | Update task deadline to past instant. Query Bob's tasks. Assert status=PENDING, deadline in past. Assert no escalation record exists anywhere — observable gap: deadline passed, nothing happened. |
| 3 | `healthAppointmentCreated` | Create LifeEvent (GP appointment, HEALTH, occurredAt=next week). Create HouseholdTask (GP follow-up call, HEALTH, slaHours=24, deadline=day after appointment). Assert both persist. |
| 4 | `healthFollowUpSlaBreachedSilently` | Update task deadline to past. Assert task still PENDING. Assert no WorkItem exists, no audit entry, no notification record — observable gap: slaHours stored, breach invisible. |
| 5 | `financialDecisionNoApprovalGate` | Create HouseholdTask (boiler replacement quote approval, FINANCE, description="£3,000 decision required"). Assert task created. Assert no approval gate, no oversight channel, no COMMAND issued — gap: major financial decision proceeds without human sign-off. |
| 6 | `weekSummaryNoAccountabilityTrail` | Query all tasks. Assert multiple tasks overdue or PENDING. Assert domain has no consolidated SLA report, no commitment history, no audit trail — the full accountability gap in one assertion. |

Gap commentary for each step lives in `LAYER-LOG.md` accountability gaps table, not in test code.

### Test configuration (`app/src/test/resources/application.properties`)

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:life_test_${surefire.forkNumber:0};MODE=PostgreSQL;DB_CLOSE_DELAY=-1
quarkus.flyway.migrate-at-start=true
```

H2 `MODE=PostgreSQL` required (GE-20260420-d99177: same URL shared across test classes
contaminates data; UUID-suffix test data mitigates).

---

## Deferred Concerns (GitHub issues)

| Issue | Layer | What |
|---|---|---|
| life#10 | Layer 4 | ExternalActor actorId string convention for LedgerEntry integration |
| life#11 | Layer 6 | ExternalActor trust dimension score fields (Beta α,β per dimension) |
| life#12 | Layer 2 | DTO layer — api/ response records, REST resources stop returning JPA entities |
