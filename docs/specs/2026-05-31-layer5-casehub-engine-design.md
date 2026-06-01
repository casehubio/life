# Layer 5 Design — casehub-engine CasePlanModel Workflows

**Issue:** casehubio/life#6
**Layer:** 5 (+ casehub-engine)
**Date:** 2026-05-31
**Status:** Design approved

---

## Context

Layers 1–4 established the life-domain vocabulary (Layer 1), WorkItem SLA enforcement
(Layer 2), qhorus commitment lifecycle (Layer 3), and tamper-evident Merkle audit
(Layer 4). Each layer added one foundation module and demonstrated its accountability
properties through standalone operations — a single REST call creates a task, commitment,
or ledger entry.

Layer 5 adds casehub-engine to orchestrate **multi-step workflows** where the sequence of
operations, adaptive gates, and cross-step coordination are managed by the engine rather
than imperative service code. This is the layer where CaseHub's value becomes most visible:
household coordination that would otherwise be a series of disconnected tasks becomes a
formally tracked, auditable case with conditional paths and human approval gates.

### Platform features demonstrated

| Feature | Cases that show it |
|---------|-------------------|
| Parallel execution | travel-plan (flight + hotel search) |
| Adaptive gate (conditional binding) | travel-plan, contractor-coordination, financial-review |
| M-of-N SubCase quorum | travel-plan (2-of-3 family vote) |
| SubCase lifecycle | care-coordination (care episode child case) |
| DECLINE + recovery | travel-plan (rebooking), appointment-cycle (alternative provider) |
| Qhorus COMMAND + QhorusMessageSignalBridge | home-maintenance, contractor-coordination |
| Ledger writes from workers | home-maintenance, appointment-cycle, contractor-coordination, financial-review |
| Cross-case signals | contractor-coordination → financial-review |
| Milestones with SLA tracking | care-coordination |
| quarkus-flow FuncDSL worker execution | All cases |

---

## Dependencies

### Maven (app/pom.xml additions)

| Artifact | Scope | Why |
|----------|-------|-----|
| `casehub-engine` | compile | Full runtime — YamlCaseHub, choreography, orchestration |
| `casehub-engine-scheduler-quartz` | compile | Quartz worker execution (RAM store) |
| `casehub-engine-work-adapter` | compile | HumanTaskScheduleHandler + WorkItemLifecycleAdapter — bridges humanTask bindings to casehub-work WorkItems |
| `casehub-engine-blackboard` | compile | SubCaseCompletionService, PlanItem lifecycle, BlackboardRegistry — required for SubCase and M-of-N |
| `casehub-engine-persistence-memory` | compile | In-memory CaseInstanceRepository, EventLogRepository — no Docker needed |
| `casehub-engine-testing` | test | @Priority(1) auto-selected test SPIs |

**Not included (deferred to Layer 6):** `casehub-engine-ledger` — provides `CaseLedgerEntry`
(tamper-evident case lifecycle compliance trail). Layer 5 uses domain-level ledger entries
via `LifeLedgerWriter` for compliance and engine `EventLog` for operational trail. Adding
engine-level compliance trail is Layer 6 scope (matches AML precedent).

Already present from earlier layers: `casehub-platform` (runtime), `casehub-platform-expression`,
`casehub-work`, `casehub-qhorus`, `casehub-ledger`, `casehub-connectors-core`.

### Test application.properties additions

Jandex index entries for engine modules:
- `casehub-engine-common`
- `casehub-engine-blackboard`
- `casehub-engine-work-adapter`
- `casehub-engine-scheduler-quartz`
- `casehub-engine-persistence-memory`
- `casehub-engine-testing`
- `casehub-platform` (already present)

CDI exclude-types unchanged — casehub-work scheduler beans already excluded
(GE-20260523-4ca5e7). Quartz RAM store already configured.

No new Flyway migrations for engine tables — engine uses Hibernate drop-and-create.

---

## Case Definitions — 7 YAML + 7 Fluent DSL Companions

Per PP-20260518-case-definition-layers: every YAML case definition has a companion
fluent Java DSL builder class. Workers use quarkus-flow FuncDSL per
PP-20260531-worker-func-exec.

YAML files live at `app/src/main/resources/life/`. YamlCaseHub subclasses and DSL
companions live in `io.casehub.life.app.engine`.

### Case 1 — travel-plan

**Shows:** Parallel execution, adaptive gate, M-of-N SubCase quorum, DECLINE + recovery

**Workflow:**
1. `destination-research` — FuncDSL worker. Returns 3 destination options with costs.
2. `flight-search` + `hotel-search` — **fire in parallel** (both need `.selectedDestination != null`). FuncDSL workers.
3. `budget-assessment` — fires when both flight AND hotel results exist. Worker sets `requiresApproval` and `isHighValue`.
4. `family-vote` — **adaptive, M-of-N SubCase**: fires only when `isHighValue == true`. Three `subCase` bindings spawn child `family-vote` cases (one per voter). `groupId="family-vote", totalInGroup=3, requiredCount=2, onThresholdReached=KEEP`.
5. `approval-gate` — **adaptive**: fires for medium-cost trips (`requiresApproval == true and isHighValue == false`). Single humanTask for household-admin, scope `casehubio/life/finance`.
6. `booking` — fires when budget clears. FuncDSL worker. **Can DECLINE** (availability changed) — sets `booking.declined = true`.
7. `rebooking` — **adaptive DECLINE recovery**: fires when `booking.declined == true and rebooking == null`. Worker tries alternative dates.
8. `confirmation` — fires when booking succeeded.

**Goals:** `trip-booked` — `.confirmation != null`

### Case 2 — home-maintenance-cycle

**Shows:** Qhorus commitment integration, QhorusMessageSignalBridge (WAITING), ledger write on completion

**Workflow:**
1. `schedule-inspection` — FuncDSL worker.
2. `get-quotes` — FuncDSL worker returns quote options.
3. `approve-contractor` — humanTask: household-admin selects a quote, scope `casehubio/life/household`.
4. `issue-commitment` — FuncDSL worker creates a **qhorus COMMAND** on `case-{caseId}/contractor` channel with deadline and Watchdog via `MessageService.dispatch()`. Case enters **WAITING**.
5. **QhorusMessageSignalBridge** — contractor RESPONSE on `case-{caseId}/contractor` fires `CaseHubRuntime.signal()`. Case resumes with contractor's acceptance in context at `.channelMessage`.
6. `monitor-job` — fires after contractor accepts. FuncDSL worker.
7. `verify-completion` — humanTask: household-member confirms job done, scope `casehubio/life/household`.
8. `record-completion` — FuncDSL worker calls `LifeLedgerWriter` to write tamper-evident financial ledger entry.

**Goals:** `job-complete` — `.completionRecord != null`

**Prerequisite:** `LifeCaseService` puts caseId into initial context at case start (Phase 3 signal). Workers access it via `inputMapping`.

### Case 3 — care-coordination

**Shows:** SubCase lifecycle, milestones with SLA tracking, cross-case signal (escalation → appointment-cycle)

**Workflow:**
1. `needs-assessment` — FuncDSL worker.
2. `care-plan` — FuncDSL worker produces care schedule.
3. `assign-carer` — humanTask: household-member accepts delegation, scope `casehubio/life/elder-care`.
4. `care-episode` — **subCase binding**: spawns a child `care-episode` case. Parent PlanItem stays DELEGATED until child completes. Child case is a mini-workflow: assess → provide care → record notes.
5. `health-check` — fires after episode sub-case completes. FuncDSL worker analyses care notes. If concern detected, sets `healthConcern = true`.
6. `escalate-concern` — **adaptive**: fires when `healthConcern == true`. humanTask for household-admin. **Also signals** any active appointment-cycle case via `CaseHubRuntime.signal()` to trigger a GP follow-up.
7. `care-review` — humanTask: household-admin reviews care quality, scope `casehubio/life/elder-care`.

**Milestones:**
- `assessment-complete` — completionCriteria `.assessment != null`
- `carer-assigned` — completionCriteria `.carerAssignment != null`

**Goals:** `review-complete` — `.careReview != null`

### Case 4 — appointment-cycle

**Shows:** Ledger integration (health decision), DECLINE → alternative provider

**Workflow:**
1. `book-appointment` — FuncDSL worker. **Can DECLINE** (no availability).
2. `find-alternative` — **adaptive DECLINE recovery**: fires when `booking.declined == true`. Worker tries alternative provider.
3. `confirm-appointment` — fires when booking succeeded. FuncDSL worker sends reminder.
4. `pre-visit-prep` — FuncDSL worker sends checklist.
5. `attend-and-record` — humanTask: household-member records post-visit notes, scope `casehubio/life/health`.
6. `record-health-decision` — FuncDSL worker calls `LifeLedgerWriter.writeHealthEntry()` to write tamper-evident health decision ledger entry.

**Goals:** `appointment-complete` — `.healthDecisionRecorded != null`

### Case 5 — contractor-coordination

**Shows:** Full qhorus lifecycle (COMMAND → Watchdog → RESPONSE/DECLINE), cross-case signal to financial-review

**Workflow:**
1. `request-quote` — FuncDSL worker creates qhorus COMMAND on `case-{caseId}/contractor-quote` with deadline. Watchdog monitors.
2. `watchdog-escalation` — **adaptive**: fires when context signal indicates Watchdog fired (no contractor response by deadline). Worker sends reminder.
3. `quote-received` — **QhorusMessageSignalBridge**: contractor RESPONSE unblocks the case with quote in context.
4. `approve-quote` — humanTask: household-admin approves price, scope `casehubio/life/household`.
5. `job-monitoring` — FuncDSL worker.
6. `payment-gate` — humanTask: household-admin confirms payment, scope `casehubio/life/finance`.
7. `record-payment` — FuncDSL worker writes **financial ledger entry** via `LifeLedgerWriter`. **Also signals** any active financial-review case via `CaseHubRuntime.signal()` with the payment amount.

**Goals:** `contractor-paid` — `.paymentRecorded != null`

### Case 6 — financial-review

**Shows:** Cross-case signal reception, aggregation, qhorus oversight gate

**Workflow:**
1. `gather-data` — FuncDSL worker collects budget data. **Also receives** cross-case signals from contractor-coordination and travel-plan completions — each signal adds to accumulated spending context.
2. `analyse-anomalies` — fires when data gathered. Worker analyses accumulated spending plus budget data. Sets `hasAnomalies`, `anomalyDetails`.
3. `escalate-anomalies` — **adaptive**: fires when `hasAnomalies == true`. FuncDSL worker sends qhorus COMMAND to `case-{caseId}/oversight` channel. Case enters **WAITING** for household-admin RESPONSE.
4. `produce-report` — fires when analysis clean or oversight RESPONSE received. Worker generates financial summary with **legal ledger entry** via `LifeLedgerWriter`.

**Goals:** `review-complete` — `.report != null`

### Case 7 — family-vote (child case for M-of-N)

**Shows:** Single-humanTask child case used as a vote in M-of-N SubCase quorum

**Workflow:**
1. `cast-vote` — humanTask: specific household-member votes approve/reject.

**Goals:** `vote-cast` — `.vote != null`

This is the child case spawned 3 times by travel-plan's family-vote bindings.
`requiredCount=2, totalInGroup=3` — parent resumes when 2 votes are in.

---

## Service Layer

### LifeCaseTracker entity

New JPA entity on the default datasource. Tracks active engine cases for cross-case signal lookup.

```
LifeCaseTracker {
    id: UUID (PK)
    caseType: String          // e.g. "travel-plan", "financial-review"
    engineCaseId: UUID        // from startCase() return
    status: LifeCaseStatus    // ACTIVE, COMPLETED, FAILED
    createdAt: Instant
    completedAt: Instant      // nullable
}
```

`LifeCaseStatus` enum in `api/`. Entity in `app/entity/`. Migration at `V107__create_life_case_tracker.sql`
(V105–V106 taken by Layer 4).

### LifeCaseService (three-phase case start with error recovery)

Follows PP-20260529-3ffe28 (reference: clinical `AeEscalationCaseService`):

- **Phase 1** (`@Transactional`): validate request, create LifeCaseTracker(ACTIVE), build initial context, resolve CaseHub bean by type
- **Phase 2** (no transaction): `caseHub.startCase(initialContext).toCompletableFuture().join()`
- **Phase 3** (`@Transactional`): persist engineCaseId on LifeCaseTracker. Signal caseId into context via `CaseHubRuntime.signal(caseId, "caseId", caseId.toString())` so workers can access it for channel creation.
- **Error recovery** (`@Transactional`, in catch around Phase 2–3): markFailed() on LifeCaseTracker. Wrapped in own try-catch to prevent masking the original failure.

CaseHub resolution: direct injection of each YamlCaseHub bean, switch on `LifeCaseType`.
Matches clinical pattern — type-safe, compile-time verified, no string-based lookup.

### LifeCaseTrackerObserver (infrastructure)

CDI observer on `CaseLifecycleEvent("CaseCompleted")`. Updates LifeCaseTracker status:
1. Look up LifeCaseTracker by engineCaseId
2. Set status=COMPLETED, completedAt=now

`@Transactional(REQUIRES_NEW)`. Pure infrastructure — no domain logic.

### Cross-case signal service (domain logic)

Cross-case signals live in the completing worker itself (not in the lifecycle observer).
The `record-payment` worker in contractor-coordination already has the payment context —
it queries `LifeCaseTracker` for an active `financial-review` case and calls
`CaseHubRuntime.signal()`. Same for care-coordination's `escalate-concern` worker
signalling an appointment-cycle case.

This matches clinical: `TrialSafetySignalService` is a standalone service called by the
AE escalation completion observer — not baked into the case lifecycle observer.

### Cross-case signal failure modes

- **No active target case:** Log and continue. The signal is not stored for later delivery.
  This is acceptable — financial-review is a monthly cycle; the next review will query
  completed payments from the database directly.
- **Multiple active target cases:** Signal all. Only one financial-review should be active
  at a time, but the design does not enforce this constraint at the DB level.
- **Late signal (target past gather-data):** Engine context is append-only — the signal
  lands in context even if no binding currently watches for it. No data loss.

### LifeCaseResource

`POST /life-cases` — `@Blocking @ApplicationScoped` (PP-20260526-d0b921).

Request: `{ caseType: "TRAVEL_PLAN", context: { ... } }`
Response: 201 Created `{ caseId, caseType, status }`
Validation: 422 for unknown case type.

`LifeCaseType` enum in `api/`: TRAVEL_PLAN, HOME_MAINTENANCE, CARE_COORDINATION,
APPOINTMENT_CYCLE, CONTRACTOR_COORDINATION, FINANCIAL_REVIEW.

Note: FAMILY_VOTE is not a LifeCaseType — it is only spawned as a sub-case by
travel-plan's M-of-N bindings, never started via the REST API.

---

## Integration with Existing Infrastructure

### Scope format retrofit — LifeTaskService

`LifeTaskService` currently sets WorkItem scope to `"life"` (single segment). Engine
humanTask bindings will use hierarchical scope `casehubio/life/{domain}`. For consistency,
retrofit `LifeTaskService.create()` to use `"casehubio/life/" + domain.name().toLowerCase()`
— one format everywhere.

This is a breaking change to the scope value in WorkItems created via `POST /life-tasks`.
No external callers — the break is free.

### Observer adaptation — LifeDecisionLedgerObserver

Engine-created WorkItems (via humanTask bindings) have no LifeTaskContext supplement.
They carry domain in the WorkItem's `scope` Path (e.g. `casehubio/life/health`).

Change: domain resolution uses scope Path for ALL WorkItems (both standalone and engine):
1. Parse WorkItem.scope — extract third segment (e.g. `casehubio/life/health` → `HEALTH`)
2. Map to `LifeDomain`
3. If scope format unrecognised, fall back to LifeTaskContext query (defensive)

After the scope retrofit, both standalone tasks and engine tasks use the same scope format.
The LifeTaskContext fallback is defensive only — it should never be needed.

### LifeSlaBreachPolicy — no change

Uses `candidateGroups.contains("household-admin")` for tier detection. Engine humanTask
bindings set candidateGroups in the YAML. Policy works unchanged.

---

## Worker Execution Model

All workers use quarkus-flow FuncDSL per PP-20260531-worker-func-exec:

```java
Worker.builder()
    .name("destination-researcher")
    .capabilities(researchCap)
    .function(
        workflow("destination-research")
            .tasks(
                function(s -> {
                    Map<String, Object> ctx = (Map<String, Object>) s;
                    // stub logic
                    return Map.of("destinations", List.of(...));
                }, Map.class))
            .build())
    .build();
```

Workers that call CDI services (LifeLedgerWriter, MessageService, ChannelService)
use `FuncDSL.function()` wrapping the CDI proxy call. Workers run on Quartz threads —
JPA calls are safe (AML reference: SAR-drafting worker calls ComplianceReviewLifecycle).

---

## CaseHub Beans

Seven `@ApplicationScoped` YamlCaseHub subclasses in `io.casehub.life.app.engine`:

| Class | YAML resource |
|-------|--------------|
| `TravelPlanCaseHub` | `life/travel-plan.yaml` |
| `HomeMaintenanceCaseHub` | `life/home-maintenance.yaml` |
| `CareCoordinationCaseHub` | `life/care-coordination.yaml` |
| `AppointmentCycleCaseHub` | `life/appointment-cycle.yaml` |
| `ContractorCoordinationCaseHub` | `life/contractor-coordination.yaml` |
| `FinancialReviewCaseHub` | `life/financial-review.yaml` |
| `FamilyVoteCaseHub` | `life/family-vote.yaml` |

Six augment with domain-specific workers via `getDefinition()` override + double-checked
locking (AML pattern). `FamilyVoteCaseHub` has no workers (its only binding is a
humanTask) — minimal bean with no augmentation ceremony.

Seven companion fluent DSL classes in the same package:
`TravelPlanCaseDefinitions`, `HomeMaintenanceCaseDefinitions`, etc. — each with a
static `build()` method returning `CaseDefinition`.

---

## Error Handling and Timeouts

Layer 5 uses engine defaults. No custom DLQ or PoisonPill configuration.

- **Worker failure:** If a FuncDSL worker throws, the engine marks the PlanItem as FAULTED.
  The case remains RUNNING with unfulfilled goals. No automatic retry in this layer.
- **Case timeout:** Not configured in YAML for Layer 5. Cases can stall indefinitely if a
  humanTask is never actioned. This is acceptable for a harness — production deployments
  would add `caseTimeout` and `bindingTimeout` to the YAML definitions.
- **M-of-N quorum stall:** If the third voter never responds, the parent waits indefinitely.
  `onThresholdReached=KEEP` means the third child case is not cancelled. The 2-of-3
  threshold is already met — the parent has already resumed. No stall risk.

---

## Worker Execution Guarantee

Workers execute **at-most-once** on Quartz threads. The engine does not retry failed workers
in the default configuration (no resilience module activation). This means:

- `LifeLedgerWriter` calls are not retried — a failed write leaves no ledger entry.
  The operational trail (EventLog) records the FAULTED state. No duplicate ledger entries.
- `MessageService.dispatch()` calls are not retried — a failed COMMAND is not re-sent.
  The case stalls (no WAITING → RESPONSE path activates).

If retry is needed in the future, workers must add idempotency guards (e.g. check if a
ledger entry already exists for the workItemId before writing).

---

## Implementation Phases

Same branch, same issue (#6), two commits:

### Phase 1 — Infrastructure + 3 core cases

Dependencies, Jandex, LifeCaseTracker, LifeCaseService, LifeCaseResource,
LifeCaseTrackerObserver, scope retrofit, observer adaptation.
Plus 3 simpler cases: appointment-cycle (ledger + DECLINE), home-maintenance
(qhorus bridge), family-vote (child case for M-of-N).
Build, test, verify engine CDI wiring works.

### Phase 2 — 4 advanced cases

travel-plan (parallel + M-of-N + DECLINE), care-coordination (SubCase + milestones),
contractor-coordination (full qhorus + cross-case signal), financial-review (signal
reception + aggregation + oversight gate). These build on proven Phase 1 infrastructure.

---

## QhorusMessageSignalBridge Integration

Cases that need contractor/oversight responses use engine-managed channels following
the `case-{caseId}/{purpose}` naming convention (via `CaseChannel.channelName(UUID, String)`).

Worker flow:
1. Extract caseId from case context (set by LifeCaseService Phase 3 signal)
2. Create channel via `ChannelService.create()` with name `CaseChannel.channelName(caseId, "contractor")`
3. Register in `ChannelGateway` (both calls required — GE-20260526-5247f2)
4. Dispatch COMMAND via `MessageService.dispatch()` with deadline
5. Case runs out of eligible bindings → enters WAITING
6. Contractor RESPONSE on the channel → bridge fires `CaseHubRuntime.signal(caseId, "channelMessage", payload)`
7. Next binding condition checks `.channelMessage != null and .channelMessage.messageType == "RESPONSE"`

These are engine-managed channels, separate from the existing Layer 3 life channels
(`life/delegation`, `life/oversight`, `life/actor/{id}`). Both coexist — different
accountability patterns.

---

## Cross-Case Signals

`LifeCaseTracker` enables case-to-case signaling:
- Contractor-coordination completion worker queries `LifeCaseTracker` for active
  `financial-review` case, calls `CaseHubRuntime.signal(financialReviewCaseId, "contractorPayment", paymentData)`
- Care-coordination health concern worker queries `LifeCaseTracker` for active
  `appointment-cycle` case, signals GP follow-up need

Pattern: service creates LifeCaseTracker at case start → workers query active cases
by type → signal via `CaseHubRuntime.signal()`. Clinical reference: `TrialActivationService`
stores `Trial.engineCaseId`, `AeEscalationCaseService` calls
`trialSafetySignalService.signalGrade4Active()`.

---

## Testing Strategy

### Test levels

| Level | What | How |
|-------|------|-----|
| Unit (pure Java) | LifeCaseType/LifeCaseStatus enums, DSL companions produce valid CaseDefinition | JUnit 5, no Quarkus |
| Worker workflow | FuncDSL pipelines produce expected output | Standalone workflow execution test |
| Integration (@QuarkusTest) | Full case lifecycle: start → bindings fire → workers execute → goals met | Inject YamlCaseHub, startCase(), Awaitility |
| Adaptive gate | Conditional bindings fire/skip based on context | Two tests per adaptive case |
| Qhorus bridge | COMMAND → RESPONSE → case resumes | Start case, verify WAITING, dispatch RESPONSE |
| SubCase M-of-N | Parent spawns 3 sub-cases, completes after 2 | Invoke SubCaseCompletionListener directly (engine#315) |
| Ledger integration | Worker writes ledger entry during case execution | Verify entry exists after case completes |
| Cross-case signal | Contractor completion signals financial-review | Start both, complete contractor, verify signal received |
| Observer fallback | LifeDecisionLedgerObserver handles engine WorkItems | WorkItem with scope but no LifeTaskContext |

### Test patterns

- **Direct listener invocation** (engine#315): CDI @ObservesAsync unreliable in @QuarkusTest. Invoke SubCaseCompletionListener, WorkItemLifecycleAdapter directly.
- **Awaitility**: `await().atMost(15, TimeUnit.SECONDS).until(...)` for async case completion.
- **Template seeding**: `LifeTestFixtures.seedStandardTemplates()` in @BeforeEach @Transactional.
- **No @TestTransaction**: engine workers run in REQUIRES_NEW — @TestTransaction blocks them.

### Test files

```
app/src/test/java/io/casehub/life/app/engine/
    TravelPlanCaseHubTest.java
    HomeMaintenanceCaseHubTest.java
    CareCoordinationCaseHubTest.java
    AppointmentCycleCaseHubTest.java
    ContractorCoordinationCaseHubTest.java
    FinancialReviewCaseHubTest.java
    FamilyVoteCaseHubTest.java
    LifeCaseServiceTest.java
    LifeCaseResourceTest.java
    dsl/
        TravelPlanCaseDefinitionsTest.java
        ... (one per DSL companion)
app/src/test/java/io/casehub/life/app/observer/
    LifeDecisionLedgerObserverScopeFallbackTest.java
api/src/test/java/io/casehub/life/api/
    LifeCaseTypeTest.java
    LifeCaseStatusTest.java
```

---

## File Inventory

### api/ additions

- `io.casehub.life.api.LifeCaseType` — enum
- `io.casehub.life.api.LifeCaseStatus` — enum
- `io.casehub.life.api.model.CreateLifeCaseRequest` — record
- `io.casehub.life.api.model.LifeCaseResponse` — record

### app/ additions

**Entities:**
- `io.casehub.life.app.entity.LifeCaseTracker` — JPA entity

**Engine (new package `io.casehub.life.app.engine`):**
- 7 YamlCaseHub subclasses
- 7 fluent DSL companion classes
- `LifeCaseService` — three-phase case start
- `LifeCaseTrackerObserver` — case completion + cross-case signals

**Resources:**
- `io.casehub.life.app.resource.LifeCaseResource` — REST endpoint

**YAML (7 files at `app/src/main/resources/life/`):**
- `travel-plan.yaml`, `home-maintenance.yaml`, `care-coordination.yaml`
- `appointment-cycle.yaml`, `contractor-coordination.yaml`, `financial-review.yaml`
- `family-vote.yaml`

**Migrations:**
- `V107__create_life_case_tracker.sql`

### Existing file changes

- `LifeTaskService` — retrofit scope from `"life"` to `"casehubio/life/" + domain`
- `LifeDecisionLedgerObserver` — scope-based domain resolution (primary), LifeTaskContext (fallback)
- `app/pom.xml` — add engine + blackboard + work-adapter dependencies
- `app/src/test/resources/application.properties` — add Jandex index entries for engine modules

---

## Protocols Consulted

| Protocol | How it applies |
|----------|---------------|
| PP-20260518-case-definition-layers | YAML + fluent DSL pairing rule for all 7 case definitions |
| PP-20260531-worker-func-exec | FuncWorkflowBuilder for all worker functions, not raw lambdas |
| PP-20260529-3ffe28 | Three-phase case start — never join() inside @Transactional |
| PP-20260529-ce2de0 | casehub-engine-api scope rule — life uses full engine runtime (compile) |
| PP-20260524-a8f597 | casehub-platform scope rule — runtime scope in app modules |
| PP-20260526-d0b921 | REST @Blocking @ApplicationScoped |
| PP-20260526-75d9c9 | @Transactional on service methods only |
| dual-trail-audit-pattern | EventLog (operational) vs LedgerEntry (compliance) |
| auth-retrofit-readiness | No auth in domain/service; design for retrofit |

## Garden Entries Relevant

| GE-ID | Relevance |
|-------|-----------|
| GE-20260523-4ca5e7 | casehub-work 5-field cron vs Quartz 6-field — exclude scheduler beans |
| GE-20260523-86ed13 | Engine requires casehub-platform + casehub-platform-expression |
| GE-20260523-fd8725 | Binding `when` ignored for contextChange — use filter |
| GE-20260525-d06282 | casehub-engine-testing must be Jandex-indexed |
| GE-20260526-5247f2 | ChannelService.create() + ChannelGateway register — both required |
| GE-20260529-0c23f1 | arc.exclude-types scope — only exclude what implementing module replaces |
| GE-20260528-d4b81d | Engine SNAPSHOT augmentation issue — now fixed (engine#379/#380 closed) |
