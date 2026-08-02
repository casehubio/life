# casehub-life -- Consumer Guide

> Personal life automation application on the CaseHub harness -- household management, health, finance, family obligations, elder care with formal SLA enforcement and tamper-evident audit.

**GitHub:** [casehubio/life](https://github.com/casehubio/life)
**Tier:** Application

---

## What It Is

Personal life automation application on the CaseHub harness. Coordinates household management, health, finance, family obligations, elder care -- producing a formally tracked, SLA-enforced, optionally tamper-evident record of life obligations. Field showcase and tutorial for developers evaluating CaseHub for personal automation.

## Tutorial Layers

The tutorial structure emerges from the natural adoption sequence. Each layer adds one foundation module and makes its value tangible relative to the previous layer.

| Layer | Adds | Gap it closes | Status |
|-------|------|---------------|--------|
| 1 | Domain baseline -- household domain model | Baseline: direct service calls, no SLA, no audit | **complete** (casehubio/life#2) |
| 2 | casehub-work | No formal SLA on household tasks | **complete** (casehubio/life#3, 2026-05-27) |
| 3 | casehub-qhorus | No commitment tracking; no oversight gates | **complete** (casehubio/life#4) |
| 4 | casehub-ledger | No tamper-evident audit for health/financial decisions | **complete** (casehubio/life#5) |
| 5 | casehub-engine | No multi-step workflow orchestration | **in progress** |
| 6 | Trust routing | No trust model for agent routing | **complete** (casehubio/life#11) |
| 7 | casehub-openclaw | OpenClaw as WorkerProvisioner; pre-built skill ecosystem | **complete** |
| 8 | Auth (casehub-platform-oidc) | No RBAC enforcement; risk thresholds role-agnostic | **complete** (casehubio/life#40) |

## What It Owns

### Domain Model

- **`LifeDomain` enum:** `HEALTH`, `FINANCE`, `HOUSEHOLD`, `LEGAL`, `CARE`, `TRAVEL`
- **Domain model:** `ExternalActor`, `LifeTaskContext` (domain supplement: `domain`, `priority`, `externalActorId`, `jurisdiction` (ISO 3166-1/2), deadline context -- held alongside the foundation `WorkItem`)
- **Capability tags:** `household-management`, `health-coordination`, `financial-planning`, `family-scheduling`, `travel-planning`, `legal-deadline`, `contractor-coordination`
- **Trust dimensions:** `deadline-reliability`, `cost-accuracy`, `factual-accuracy`, `proactive-alerting`

### CasePlanModel Definitions

- `appointment-cycle` -- book appointment, confirm, send pre-visit prep, follow-up after, record outcome
- `home-maintenance-cycle` -- schedule inspection, get quotes, approve contractor, monitor job, verify completion
- `financial-review` -- monthly budget review, flag anomalies, escalate major decisions to oversight channel
- `travel-plan` -- research options, approval gate for bookings above threshold, confirm itinerary
- `contractor-coordination` -- issue COMMAND for quote by date, Watchdog follow-up if no ETA, payment gate on completion
- `care-coordination` -- recurring care schedule, health status monitoring, family delegation for availability

### Household Permission Topology

- `household-admin` -- full authority: approve major financial decisions, delegate tasks, configure SLAs
- `household-member` -- standard member: view all, action assigned tasks, request new tasks
- `household-junior` -- restricted: view own tasks only, cannot approve financial decisions

M-of-N quorum configuration for joint decisions (e.g. 2-of-3 adults required for purchases above threshold).

### Flyway Path

`classpath:db/life/migration/` (PP-20260525-607b33)

## Layer 2 -- casehub-work Integration

- `POST /life-tasks` -- creates `WorkItem` + `LifeTaskContext` atomically via `WorkItemTemplate` lookup
- `LifeSlaBreachPolicy` -- implements `casehub-work` `SlaBreachPolicy` SPI; stateless two-tier escalation: first breach escalates to `household-admin`, second breach fails

## Layer 3 -- casehub-qhorus Integration

- `LifeCommitmentRecord` entity -- persists commitment context (task id, actor, channel, message correlation)
- `LifeCommitmentStrategy` SPI -- maps household task type to channel and speech-act selection
- Channel topology: `life/delegation` (task assignment), `life/oversight` (human gates), `life/actor/{id}` (per-actor channel)
- `LifeOversightResponseObserver` -- `MessageObserver` SPI implementation; bridges oversight RESPONSE/DECLINE to task lifecycle
- REST: `POST /life-tasks/{id}/commit`, `POST /life-oversight-gates`

## CBR Integration

6 domain feature schemas registered at startup by `LifeCbrFeatureSchemaRegistrar` (`@Observes StartupEvent`): `contractor-coordination`, `home-maintenance`, `appointment-cycle`, `care-coordination`, `financial-review`, `travel-plan`.

Dual-path outcome recording: `LifeRoutingOutcomeRecorder` (implements `RoutingOutcomeRecorder`) records agent-routing outcomes; `LifeCaseOutcomeCbrWriter` (implements `CaseOutcomeObserver`) records case-level outcomes. Both write to `CbrCaseMemoryStore`.

Dual-path architecture in `LifeCaseService.startCase()`: calls `cbrSuggestionService.retrieveForAdaptation()`, injects `cbrCalibration` and `adaptedPlan` into initial context, fires `CbrAdaptationRecorded` event.

## Read-Side API

**Analytics** (`LifeAnalyticsResource`, `/analytics`):
- `GET /analytics/cases` -- `CaseStatisticsResponse` (per-type stats, resolution time percentiles, completion rate)
- `GET /analytics/sla` -- `SlaComplianceResponse` (breach count, compliance rate, avg breach latency)
- `GET /analytics/trust` -- `TrustAnalyticsResponse` (actor trust score summaries, dimension averages, lowest-scoring actors)

**Pending actions** (`PendingActionsResource`, `/pending-actions`):
- `GET /pending-actions` -- paged, filterable by domain/candidateGroup/dueSoonHours, urgency-classified

**Actor search** (`ExternalActorResource`, `/external-actors`):
- `GET /external-actors` -- search by name, actorType, contactMethod, erasedOnly; paged
- `GET /external-actors/{id}/trust-history` -- actor trust score history
- `GET /external-actors/{id}/activity` -- actor activity timeline

## LifeTaskVisibilityPolicy SPI

`LifeTaskVisibilityPolicy` interface (`api/spi/`): `boolean isVisible(LifeTaskResponse task, String actorId, Set<String> groups)`.

**`DefaultLifeTaskVisibilityPolicy`** (`@DefaultBean`): always returns `true` (permissive).

**`JuniorLifeTaskVisibilityPolicy`** (`@Alternative @Priority(1)`): non-junior principals pass unconditionally; junior principals (`HouseholdGroups.JUNIOR`) visible only if assigned or in candidate pool. Implements household-junior scoping.

## Frontend -- life-ui

Lit 3.x single-page application served via Quarkus Quinoa (2.8.3). Hash-routed app shell with two views.

| View | What it shows |
|------|--------------|
| Dashboard (`home-view`) | KPI metrics (active cases by domain, SLA compliance, pending actions), case statistics |
| Inbox (`inbox-view`) | Work-item-workbench composition -- filterable inbox with detail pane |

**Build:** Vite with aliases resolving `@casehubio/blocks-ui-*` and `@casehubio/pages-*` from Maven SNAPSHOT artifacts (extracted to `.casehub-packages/` via `mvn initialize`). No npm cross-repo `file:` references -- see [casehub-pages ADR-0001](https://github.com/casehubio/casehub-pages/blob/main/docs/adr/0001-cross-repo-frontend-dependency-management.md).

**Profiles:** Quinoa disabled by default (`quarkus.quinoa.enabled=false`) and in tests. Enabled in `dev` and `demo` profiles only.

**Demo mode:** `quarkus.profile=demo` -- H2 in-memory, Flyway demo seeds at `db/life/demo/` (V9000+ range), OIDC disabled.

## Dependencies

```
casehub-life
  -> casehub-platform-oidc     (Layer 8: OidcCurrentPrincipal, @RolesAllowed enforcement)
  -> casehub-openclaw          (Layer 7: OpenClaw WorkerProvisioner, ChannelContextWindow)
  -> casehub-engine            (Layer 5: CasePlanModel orchestration)
  -> casehub-engine-work-adapter (HumanTaskScheduleHandler + WorkItemLifecycleAdapter)
  -> casehub-engine-scheduler-quartz (Quartz worker execution)
  -> casehub-ledger            (Layer 4: Merkle audit, GDPR erasure, trust scoring)
  -> casehub-work              (Layer 2: WorkItems with SLA and escalation)
  -> casehub-qhorus            (Layer 3: commitment lifecycle, oversight channel)
  -> casehub-connectors-core   (household notifications)
  -> casehub-neocortex         (CBR: CbrCaseMemoryStore, CbrFeatureSchema -- 6 domain schemas)
  -> casehub-blocks            (CBR: RoutingOutcomeRecorder, PlanAdapter SPIs)
```

## What It Does NOT Own

Foundation capabilities that casehub-life consumes but does not implement:

- Trust scoring -- casehub-ledger
- Commitment lifecycle -- casehub-qhorus
- Case engine and `CasePlanModel` execution -- casehub-engine
- WorkItem inbox with SLA -- casehub-work
- Notification delivery -- casehub-connectors
- Skill execution -- casehub-openclaw
