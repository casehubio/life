# casehub-life -- Contributor Guide

> Internal architecture, SPIs, extension points, and structural patterns for platform builders working on casehub-life internals.

**GitHub:** [casehubio/life](https://github.com/casehubio/life)

---

## Internal Architecture

### LifeTypedCaseHub Template Method Pattern

`LifeTypedCaseHub` is the abstract base class for all primary case types. It extends `YamlCaseHub` and provides:

- **Constructor:** takes a YAML path and a `LifeAgent` enum value
- **`augment(CaseDefinition)`:** final method that calls `configureCase()` then registers the agent descriptor
- **`configureCase(CaseDefinition)`:** abstract; subclasses add workers and bindings
- **`agentWorker(String, String, Class)`:** standard factory for LLM-backed workers -- builds an `Agent` with OpenClaw chat model, system prompt (with CBR suffix appended), and response schema
- **CBR input transformer:** `CbrInputTransformer` initialized in `@PostConstruct`, injected into every agent worker via `LifeCbrExperienceFormatter`

Subclasses are minimal: declare the YAML path, the agent persona, and override `configureCase()` to register workers. Example: `AppointmentCycleCaseHub` registers 5 agent workers (book-appointment, find-alternative, confirm-appointment, pre-visit-prep, record-health-decision).

**Child cases** (`CareEpisodeCaseHub`, `FamilyVoteCaseHub`) extend `YamlCaseHub` directly (not `LifeTypedCaseHub`) because they are not startable via `LifeCaseService` -- they are spawned as sub-cases by parent case bindings.

### LifeCaseService Three-Phase Pattern

`LifeCaseService.startCase()` uses a three-phase pattern to avoid Agroal connection pool deadlock (PP-20260529-3ffe28):

1. **Phase 1** (`@Transactional`): validate request, create `LifeCaseTracker` with ACTIVE status, build initial context map
2. **Phase 2** (no transaction): CBR retrieval and adaptation, then `caseHub.startCase(initialContext)` -- engine creates case instance outside the JPA transaction
3. **Phase 3** (`@Transactional`): persist `engineCaseId` on the tracker, signal `caseId` into context

Error recovery: on any exception, `markFailed()` sets tracker to FAILED status. Each phase is a separate method with method-level transaction boundaries via CDI proxy self-injection.

CBR integration in Phase 2:
- `cbrSuggestionService.retrieveForAdaptation()` retrieves similar past cases
- `trustFeatureEnricher.enrich()` adds trust-score-derived features
- `planAdapter.adapt()` produces an `AdaptedPlan` with steps
- `cbrCalibration` and `adaptedPlan` injected into initial context
- `CbrAdaptationRecorded` event fired for observability

Case resolution: `Instance<LifeTypedCaseHub>` CDI lookup, filtered by `hub.lifeCaseType() == type`.

### Layer 4 -- Ledger Integration

#### LedgerEntry Subclasses

4 `LedgerEntry` subclasses (JOINED inheritance in `io.casehub.life.app.ledger`, qhorus PU):

- `HealthDecisionLedgerEntry` -- health decision audit trail with `appointmentType`, `eventType` (LifeDecisionEventType), `actionTaken`
- `FinancialDecisionLedgerEntry` -- financial decision audit trail with `amount`, `currency`, `eventType`, `actionTaken`
- `LegalActionLedgerEntry` -- with `jurisdiction` field (`@Column(name = "jurisdiction", length = 10)`, ISO 3166-1/2 format) alongside `workItemId`, `legalObligation`, `filingDeadline`, `eventType`, `actionTaken`. Jurisdiction included in `domainContentBytes()` for Merkle digest integrity.
- `ExternalActorErasureLedgerEntry` -- with `ledgerEntriesAffected` and `memoryRecordsErased` fields for self-contained Merkle-chained erasure proof

#### DomainLedgerHandler SPI

`DomainLedgerHandler` interface: per-domain ledger write strategy. Each handler:

- Returns its `domain()` (used for dispatch)
- Implements `writeEntry(LifeDecisionEventType, UUID workItemId, WorkItem)` for WorkItem-based events
- Optionally overrides `writeEntry(LifeDecisionEventType, LifeCommitmentRecord)` for commitment-based events (default no-op)
- Uses `DomainLedgerHandler.nextSequenceNumber()` static helper for sequence computation

3 implementations: `HealthDomainLedgerHandler`, `FinanceDomainLedgerHandler`, `LegalDomainLedgerHandler`.

`LifeDecisionLedgerObserver` dispatches to handlers: observes `SlaBreachEvent` and `WorkItemLifecycleEvent` (COMPLETED only), resolves domain from scope or `LifeTaskContext`, delegates to matching handler. Uses `@Transactional(REQUIRES_NEW)` for isolation.

#### LifeOutcomeAttestationWriter

`LifeOutcomeAttestationWriter` converts WorkItem outcomes into `LedgerAttestation` records for trust scoring:

- COMPLETED events produce `SOUND` verdicts
- SLA_BREACH events produce `FLAGGED` verdicts
- Tasks with deadlines get an additional `deadline-reliability` dimension attestation with a score computed as `clamp(1.0 - daysLate / 7.0, 0.0, 1.0)`
- Capability tag resolved from `LifeTaskContext.domain` descriptor or WorkItem scope

#### LifeGdprErasureService

Dedicated GDPR erasure pipeline (life#49):

1. `ExternalActor.erasedAt` set (PII nullification)
2. `CaseMemoryStore.eraseEntity()` called (CBR memory erasure)
3. `LedgerErasureService.erase()` called (tokenisation-based ledger actor ID erasure)
4. `ExternalActorErasureLedgerEntry` written with `ledgerEntriesAffected` and `memoryRecordsErased` counts
5. Returns `ErasureResponse` for compliance reporting

### Sentinel Heartbeat System

#### Architecture

The sentinel system provides persistent monitoring for long-running cases. Each case definition declares a sentinel capability binding in its YAML. When the case starts and the sentinel binding fires, the engine schedules a Quartz job.

**`LifeSentinelConfig`** (`@ConfigMapping(prefix = "casehub.life.sentinel")`): maps sentinel capability names to agent personas and heartbeat intervals. 7 sentinels configured in application.properties.

**`LifeSentinelRegistry`**: tracks active sentinel jobs per case ID for cleanup on case termination.

**`LifeHeartbeatJob`** (Quartz `Job`):
1. Reads `LifeAgent`, `caseId`, `capabilityName` from `JobDataMap`
2. Queries case context via `caseHubRuntime.query(caseId, ".")`
3. Enriches with channel context via `LifeChannelContextProvider.gatherContext()`
4. Builds sentinel `Agent` with typed response schema (7 sentinel report classes)
5. Executes agent and signals `sentinelReport` back into case context
6. Case YAML bindings react to `sentinelReport.escalationRequired` with human task escalation

**`LifeChannelContextProvider`** (life#61): merges recent qhorus channel messages (delegation, oversight, per-actor) into heartbeat context. Config: `casehub.life.channel-context.message-limit` (default 10).

#### Sentinel Response Schemas

7 typed sentinel reports in `io.casehub.life.app.engine.agent`:

- `ContractorSentinelReport` -- status (on-track/delayed/stalled), concerns, recommended actions
- `MaintenanceSentinelReport` -- sensor readings, status, concerns (uses `iot_get_state`)
- `FollowUpSentinelReport` -- pending actions, days overdue, escalation needed
- `CareQualitySentinelReport` -- scheduled vs completed sessions, missed sessions
- `PatientStatusSentinelReport` -- condition summary, trend (improving/stable/declining), sensor readings
- `AnomalySentinelReport` -- anomalies found, severity, escalation needed
- `BookingSentinelReport` -- booking status, price changes, alerts

### CBR Internals

#### Feature Schema Registration

`LifeCbrFeatureSchemaRegistrar` (`@Observes StartupEvent`): registers 6 domain feature schemas by reading the `spec.cbr` section from each YAML case definition. Each schema includes:
- Feature extractors (JQ paths)
- Feature weights
- topK and minSimilarity thresholds
- Domain scope and case type

#### Description Provider SPI

`LifeCbrDescriptionProvider` interface: `caseType()`, `describeProblem()`, `describeSolution()`, `extractEntityId()`. 6 implementations in `cbr/describe/`:

- `AppointmentCycleDescriptionProvider`
- `CareCoordinationDescriptionProvider`
- `ContractorCoordinationDescriptionProvider`
- `FinancialReviewDescriptionProvider`
- `HomeMaintenanceDescriptionProvider`
- `TravelPlanDescriptionProvider`

#### Adaptation Rules

`LifeAdaptationRule` interface in `cbr/adapt/`. 6 domain-specific implementations plus `SeverityScaling` utility:

- `AppointmentCycleAdaptationRule`
- `ContractorAdaptationRule`
- `FinancialAdaptationRule`
- `HealthAdaptationRule`
- `HomeMaintenanceAdaptationRule`
- `TravelPlanAdaptationRule`

`LifePlanAdapter` (implements `PlanAdapter`, selected via `quarkus.arc.selected-alternatives`): applies adaptation rules to produce `AdaptedPlan` with steps. `LifeTrustFeatureEnricher`: enriches CBR features with trust-score-derived features from `LifeDomainDescriptor` routing policies.

#### Dual-Path Outcome Recording

- `LifeRoutingOutcomeRecorder` (implements `RoutingOutcomeRecorder`): records agent-routing outcomes per worker execution
- `LifeCaseOutcomeCbrWriter` (implements `CaseOutcomeObserver`): records case-level outcomes on terminal state (COMPLETED/FAILED)
- Both write to `CbrCaseMemoryStore`

#### Feature Extraction

`LifeCbrFeatureExtractor`: JQ-based feature extraction from case context. Uses `jackson-jq` for evaluation.

`LifeCbrSuggestionService`: retrieves similar past cases and formats them as `LifeCbrRetrievalResult` with suggestions and raw cases.

`LifeCbrExperienceFormatter`: formats past case experiences into context strings for LLM consumption. Used by `CbrInputTransformer` to inject `_cbrContext` sections into agent input.

### Event Infrastructure

CDI event bridge for SSE push to the frontend:

- `LifeEventType` enum: `WORK_ITEM_CREATED`, `WORK_ITEM_UPDATED`, `WORK_ITEM_COMPLETED`, `SLA_BREACH`, `CASE_STARTED`, `CASE_COMPLETED`, `CASE_FAULTED`
- `LifeSseEvent` record: carries type, payload, timestamp
- `LifeEventBridge`: observes CDI events from work/engine subsystems, converts to `LifeSseEvent`
- `LifeEventBroadcaster`: fan-out broadcaster; subscribers register callbacks
- `LifeEventSseResource`: exposes two SSE streams (`/events/inbox`, `/events/cases`) filtered by event type, with 30-second keepalive heartbeat

### Routing and Risk Classification

#### LifeActionRiskClassifier

`LifeActionRiskClassifier` (`@ApplicationScoped @RiskClassifier`): classifies consequential agent actions.

Resolution flow:
1. Parse `PlannedAction.actionType()` to `HouseholdActionType` via `fromActionType()`
2. Switch on `GatePolicy`: ALWAYS -> gate, NEVER -> autonomous, AMOUNT_THRESHOLD -> RBAC-aware threshold check
3. RBAC differentiation: admin gets elevated thresholds from risk-policy.yaml; junior always gates (fail-secure); unknown roles gate
4. Build `RiskDecision.GateRequired` with reason template, reversibility, candidate groups, expiry, scope

Key design: `isJunior()` uses negative definition -- non-admin AND non-member. Fail-secure: unrecognised identities never act autonomously.

#### Trust Routing Configuration

`LifeTrustRoutingPolicyProvider`: maps `LifeDomainDescriptor.routingPolicy()` (threshold, minimumObservations, borderlineMargin, fallbackType, rationale) to engine trust routing configuration. Per-scope YAML overlay in `trust-routing.yaml` provides blend-factor and trust floors.

### Persistence Architecture

Two datasources with XA transactions:

| Datasource | Tables | Flyway Locations |
|------------|--------|-----------------|
| default | WorkItem, LifeTaskContext, ExternalActor, LifeCommitmentRecord, LifeCaseTracker, WorkItemTemplate | `db/life/migration`, `db/work/migration` |
| qhorus | Channel, Message, Watchdog, LedgerEntry (all subtypes), LedgerAttestation, ActorTrustScore | `db/qhorus/migration`, `db/ledger/migration`, `db/life/ledger/migration`, `db/engine-ledger/migration` |

Engine case instances use in-memory storage (`casehub-engine-persistence-memory`).

CDI alternatives selected:
- `JpaLedgerEntryRepository`, `JpaActorTrustScoreRepository` -- JPA implementations for ledger
- `InMemoryCaseInstanceRepository` (and all related in-memory engine repos) -- engine persistence
- `LifePlanAdapter` -- CBR plan adaptation
- `JuniorLifeCaseVisibilityPolicy` -- junior visibility filtering

CDI exclusions: heartbeat-mode OpenClaw beans (`OpenClawWorkerProvisioner`, `ReactiveOpenClawWorkerProvisioner`, etc.) and `MockGroupMembershipProvider`.

### Observer Pattern

3 CDI observers in `io.casehub.life.app.observer`:

| Observer | Event | Transport | Purpose |
|----------|-------|-----------|---------|
| `LifeDecisionLedgerObserver` | `SlaBreachEvent`, `WorkItemLifecycleEvent` | `@Observes` (sync) | Dispatch to `DomainLedgerHandler` for ledger writes |
| `LifeOversightResponseObserver` | `MessageReceivedEvent` | `@Observes` (sync, `MessageObserver` SPI) | Bridge oversight RESPONSE to task creation |
| `LifeWatchdogAlertObserver` | `WatchdogAlertEvent` | `@ObservesAsync` (async) | Create escalation tasks on expired approvals |

All use `@Transactional(REQUIRES_NEW)` for isolation.

`LifeOversightResponseObserver` guards: `RESPONSE` type only, `life/oversight` channel only, non-null correlationId. Deserializes `pendingTaskJson` from `LifeCommitmentRecord` to create the approved task.

`LifeWatchdogAlertObserver` guards: `APPROVAL_PENDING` condition only. Queries all expired `PENDING_RESPONSE` records on the notification channel. For OVERSIGHT records with domain, writes a ledger entry before creating the escalation task.

### OpenClaw Integration (Direct-Call Mode)

casehub-life uses OpenClaw's `/hooks/agent` direct-call integration, not heartbeat mode. All heartbeat-mode beans from `casehub-openclaw-casehub` are CDI-excluded.

`LifeOpenClawChatModelFactory`: creates OpenClaw chat model instances for each `LifeAgent`. Used by `LifeTypedCaseHub.agentWorker()` and `LifeHeartbeatJob`.

`LifeAgentDescriptorFactory`: creates `AgentDescriptor` instances from `LifeAgent` enum values for engine registration.

### Commitment Strategy Hierarchy

4 commitment strategies in `io.casehub.life.app.commitment`:

| Strategy | Mode | Channel | Use Case |
|----------|------|---------|----------|
| `DelegationCommitmentStrategy` | DELEGATION | life/delegation | Assign tasks to household members |
| `ContractorCommitmentStrategy` | DELEGATION | life/actor/ext-{id} | Issue COMMANDs to contractors |
| `OversightGateStrategy` | OVERSIGHT | life/oversight | Request approval for consequential actions |

`LifeCommitmentService` coordinates: resolves strategy from `CommitmentRequest.mode()`, creates `LifeCommitmentRecord`, sends qhorus message, starts Watchdog timer.

`CommitmentMode` enum carries escalation templates used by `LifeWatchdogAlertObserver` for escalation task titles.

## Key Epics

1. Project scaffold -- Maven structure, CLAUDE.md, CI (life#1)
2. Domain model -- `LifeDomain`, `ExternalActor`, capability tags (life#2)
3. casehub-work integration -- household task WorkItems with SLA and escalation (life#3)
4. casehub-qhorus integration -- commitment tracking and oversight gates (life#4)
5. casehub-ledger integration -- Merkle audit, trust scoring, GDPR erasure (life#5)
6. casehub-engine integration -- CasePlanModel definitions, multi-step workflows (life#6)
7. Trust routing -- agent routing by trust dimensions, risk classification (life#11)
8. casehub-openclaw integration -- direct-call agents, household skill pack (life#25, life#38, life#60)
9. CBR integration -- adaptive life automation from past-case calibration (life#52)
10. Household Hub UI -- Lit SPA, dashboard, inbox, SSE events (life#74)

Issues: https://github.com/casehubio/life/issues

## Design Documents

- `docs/specs/life-automation.md` -- life automation domain, use case analysis, key domains
- `docs/specs/life-actor-model.md` -- actor model: ExternalActor types, trust dimensions, agent routing
- `docs/specs/2026-05-30-layer4-casehub-ledger-design.md` -- Layer 4 ledger design spec
- `docs/specs/2026-05-31-layer5-casehub-engine-design.md` -- Layer 5 engine design spec
- `docs/specs/2026-06-03-layer6-trust-routing.md` -- Layer 6 trust routing design spec
- `docs/specs/2026-06-07-action-risk-classifier-design.md` -- ActionRiskClassifier design spec
- `docs/specs/2026-06-17-openclaw-agent-worker-design.md` -- OpenClaw agent worker design spec
- `docs/specs/2026-06-24-hooks-agent-direct-call-design.md` -- Direct-call integration design spec
- `docs/specs/2026-06-27-layer7-worker-provisioner-heartbeat.md` -- Layer 7 sentinel heartbeat design spec
- `docs/specs/2026-07-09-cbr-adaptive-life-design.md` -- CBR adaptive life automation design spec
- `docs/specs/2026-07-14-cbr-engine-integration-design.md` -- CBR engine integration design spec
- `docs/specs/2026-07-19-household-hub-ui-design.md` -- Household Hub UI design spec
