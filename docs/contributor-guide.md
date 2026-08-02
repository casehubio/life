# casehub-life -- Contributor Guide

> Internal architecture, SPIs, and extension points for platform builders working on casehub-life internals.

**GitHub:** [casehubio/life](https://github.com/casehubio/life)

---

## Internal Architecture

### Layer 4 -- Ledger Integration

4 `LedgerEntry` subclasses (JOINED inheritance in `io.casehub.life.app.ledger`, qhorus PU):

- `HealthLedgerEntry` -- health decision audit trail
- `FinancialLedgerEntry` -- financial decision audit trail
- `LegalLedgerEntry` -- with `jurisdiction` field (ISO 3166-1/2); prefers task-level jurisdiction over tenant-wide config (life#48)
- `ExternalActorErasureLedgerEntry` -- with `ledgerEntriesAffected` field for self-contained Merkle-chained erasure proof

**`LifeLedgerWriter`** -- unified writer service; single injection point for all ledger writes. Owns `sequenceNumber` computation and base field assembly.

**`LifeGdprErasureService`** (life#49) -- dedicated GDPR erasure pipeline: PII nullification, memory erasure, `LedgerErasureService.erase()` tokenisation, `ExternalActorErasureLedgerEntry` write. Replaces inline `ExternalActorService.erase()`.

**Per-action jurisdiction:** `LegalActionLedgerEntry` carries `@Column(name = "jurisdiction", length = 10)` (ISO 3166-1/2 format) alongside `workItemId`, `legalObligation`, `filingDeadline`, `eventType` (LifeDecisionEventType), `actionTaken`. Jurisdiction included in `domainContentBytes()` for Merkle digest integrity.

### WorkerProvisioner Heartbeat Integration

**`LifeReactiveWorkerProvisioner`** (implements `ReactiveWorkerProvisioner`): `provision()` resolves agent, reads `heartbeatInterval` from `LifeSentinelConfig`, calls `scheduleHeartbeat()` via Quartz scheduler, registers in `LifeSentinelRegistry`. `terminateAllForCase()` cancels heartbeat jobs and removes from registry.

**`LifeHeartbeatJob`** (Quartz `Job`): queries case context, gathers channel context via `LifeChannelContextProvider.gatherContext()`, builds sentinel Agent, executes, signals `sentinelReport` back into the case.

**7 sentinel types:** contractor, maintenance, follow-up, care-quality, patient-status, anomaly, booking.

### CBR Internals

**`LifeCbrDescriptionProvider` SPI** -- interface with `caseType()`, `describeProblem()`, `describeSolution()`, `extractEntityId()`. 6 implementations in `cbr/describe/`: `AppointmentCycleDescriptionProvider`, `CareCoordinationDescriptionProvider`, `ContractorCoordinationDescriptionProvider`, `FinancialReviewDescriptionProvider`, `HomeMaintenanceDescriptionProvider`, `TravelPlanDescriptionProvider`.

**Dual-path outcome recording:** `LifeRoutingOutcomeRecorder` (implements `RoutingOutcomeRecorder`) records agent-routing outcomes per worker execution; `LifeCaseOutcomeCbrWriter` (implements `CaseOutcomeObserver`) records case-level outcomes on terminal state. Both write to `CbrCaseMemoryStore`.

**Dual-path architecture in `LifeCaseService.startCase()`:** calls `cbrSuggestionService.retrieveForAdaptation()`, injects `cbrCalibration` and `adaptedPlan` into initial context, fires `CbrAdaptationRecorded` event. `LifePlanAdapter` (implements `PlanAdapter`) and `LifeTrustFeatureEnricher` support CBR-adapted case plans. 6 adaptation rules in `cbr/adapt/`. Feature extraction via `LifeCbrFeatureExtractor` (JQ-based).

### LifeChannelContextProvider

`LifeChannelContextProvider` (life#61) -- merges recent Qhorus channel messages (delegation, oversight, per-actor) into heartbeat sentinel context for cross-agent coordination. Config: `casehub.life.channel-context.message-limit` (default 10).

## Key Epics

1. Project scaffold -- Maven structure, CLAUDE.md, CI
2. Domain model -- `LifeDomain`, `HouseholdTask`, `LifeGoal`, `LifeEvent`, `ExternalActor`, capability tags
3. casehub-work integration -- household task WorkItems with SLA and escalation
4. casehub-qhorus integration -- commitment tracking and oversight gates
5. casehub-ledger integration -- Merkle audit and trust scoring for health/financial decisions
6. casehub-engine integration -- `CasePlanModel` definitions and multi-step workflow orchestration
7. Trust routing -- agent routing by `deadline-reliability`, `cost-accuracy`, and `factual-accuracy`
8. casehub-openclaw integration -- OpenClaw as `WorkerProvisioner`; household skill pack

Issues: https://github.com/casehubio/life/issues?label=epic

## Current State

Household tasks are now formal `WorkItem`s: SLA-enforced, delegable, auditable. `LifeTaskContext` supplements each task with life-specific fields. `LifeSlaBreachPolicy` escalates to `household-admin` on first breach, fails on second. Domain model correction in Layer 2: `HouseholdTask`, `LifeGoal`, `LifeEvent` removed -- they duplicated `WorkItem`, case definitions, and ledger entries respectively.

Engine deps temporarily removed from `pom.xml` -- SNAPSHOT build broken (engine#379, engine#380). Will be restored in Layer 5 branch. Layers 5-7 remain pending.

## Design Documents

- `docs/specs/life-automation.md` -- life automation domain, use case analysis, key domains
- `docs/specs/life-actor-model.md` -- actor model: ExternalActor types, trust dimensions, agent routing
- `docs/specs/2026-05-30-layer4-casehub-ledger-design.md` -- Layer 4 design spec
