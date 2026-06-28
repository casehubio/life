# Design: Layer 7 (full) — WorkerProvisioner Heartbeat Mode (life#37)

**Date:** 2026-06-27 (rev 4)
**Issue:** casehubio/life#37
**Branch:** `issue-37-layer7-worker-provisioner`
**Depends on:** life#38 (AgentExec wiring — complete), life#46 (LifeAgent consolidation — complete)

---

## 1. What This Delivers

Persistent heartbeat-mode agent monitoring across all 7 active case plans. Each case plan gains a sentinel capability — a periodically invoked agent that monitors external actors, detects anomalies, enforces SLAs, and escalates proactively.

This completes Layer 7: casehub-openclaw is the WorkerProvisioner. Sentinels use the full OpenClaw skill ecosystem (banking APIs, calendar, Home Assistant, WhatsApp/SMS) via the same `/hooks/agent` direct-call path already proven by AgentExec workers.

## 2. What This Does NOT Change

The 32 existing AgentExec workers (life#38) remain unchanged. They handle request/response tasks via the inline worker path. The provisioner handles a different category of work — persistent ambient monitoring — that doesn't fit the reactive binding model.

The engine's execution flow ensures coexistence naturally:
1. Binding fires → engine checks inline workers → match found → AgentExec executes
2. Binding fires → no inline worker matches → engine calls `tryProvision()` → provisioner handles it

**Constraint: sentinel capabilities must NEVER have inline workers registered.** If someone adds a worker with a sentinel capability name to a CaseHub's `augment()`, the inline worker matches first and the provisioner is never called. Sentinel capabilities are reserved for the provisioner path.

## 3. Architecture

### Two execution modes, shared agent infrastructure

```
AgentExec (request/response)          Provisioner (heartbeat monitoring)
────────────────────────────          ──────────────────────────────────
Binding fires                         Binding fires (sentinel capability)
  → inline worker matches               → no inline worker
  → Agent.execute()                      → tryProvision()
  → DirectCallBridge → /hooks/agent      → LifeReactiveWorkerProvisioner.provision()
  → structured WorkerResult               → register in LifeSentinelRegistry
  → case context updated directly          → schedule Quartz heartbeat
                                       Each heartbeat tick:
                                           → Agent.execute() via DirectCallBridge
                                           → per-sentinel response schema
                                           → CaseHubRuntime.signal() → case context
                                           → bindings react to .sentinelReport fields
                                       Case completes:
                                           → LifeProvisionerCleanupObserver
                                           → terminateAllForCase() → cancel heartbeat
```

### Key design choice: Agent.execute() for heartbeat invocations

Sentinels use the SAME `Agent.execute()` → `DirectCallBridge` → `/hooks/agent` path as AgentExec workers. Each heartbeat tick constructs an `Agent` with a sentinel-specific system prompt and per-sentinel response schema, calls `execute()` synchronously (blocks on Quartz thread), gets a structured result, and signals the case directly via `CaseHubRuntime.signal()`.

**Why not `OpenClawHookClient.wake()`:** Verified signature: `wake(String agentId, String message)` posts to `/hooks/wake` and returns void. Fire-and-forget — the agent cannot deliver structured results back. Using `Agent.execute()` gives: structured output via response schema, synchronous result delivery, proven infrastructure (same path as 32 existing workers).

### Result delivery: direct case signaling

Heartbeat results are delivered via `CaseHubRuntime.signal(caseId, "sentinelReport", reportMap)` — a public API in `casehub-engine-api` already used by `LifeCaseService.startCase()`. This sets `.sentinelReport` in case context and triggers binding evaluation.

**Why not channel-based delivery:** `QhorusMessageSignalBridge` only processes commitment-resolving message types (RESPONSE, DONE, DECLINE, FAILURE). STATUS messages are silently dropped (verified: `isCommitmentResolving()` whitelist). Direct signaling bypasses this limitation, eliminates sentinel channel creation, and removes ChannelService/MessageService dependencies.

### Quartz thread pool

Quartz default thread pool is 10 threads. Seven sentinel jobs blocking up to 120s each will occupy 7 threads when heartbeats align. Configure `quarkus.quartz.thread-pool-size` if more concurrent sentinels or other Quartz jobs are needed.

## 4. New Components

### 4.1 `LifeSentinelRegistry`

**Package:** `io.casehub.life.app.engine`
**Type:** `@ApplicationScoped` CDI bean

Life's own sentinel tracking. Supports multiple concurrent cases per agent type — no 1:1 constraint.

```java
@ApplicationScoped
public class LifeSentinelRegistry {

    record SentinelEntry(LifeAgent agent, UUID caseId,
                         String capabilityName, JobKey heartbeatJobKey) {}

    private final ConcurrentHashMap<UUID, List<SentinelEntry>> byCaseId = new ConcurrentHashMap<>();

    public boolean isProvisioned(UUID caseId, String capabilityName) { ... }
    public void register(SentinelEntry entry) { ... }
    public List<SentinelEntry> findByCaseId(UUID caseId) { ... }
    public void removeByCaseId(UUID caseId) { ... }
}
```

**Why not `OpenClawAgentRegistry`:** The openclaw registry has a 1:1 agentId↔caseId constraint (verified: `register()` uses `put()`, not `putIfAbsent()` — silently overwrites). If two health-domain cases run concurrently, the second registration drops the first. Life's registry keys by `(caseId, capabilityName)` — no conflict.

### 4.2 `LifeReactiveWorkerProvisioner`

**Package:** `io.casehub.life.app.engine`
**Type:** `@ApplicationScoped` CDI bean implementing `ReactiveWorkerProvisioner`

```java
@ApplicationScoped
public class LifeReactiveWorkerProvisioner implements ReactiveWorkerProvisioner {

    @Inject LifeSentinelRegistry sentinelRegistry;
    @Inject LifeSentinelConfig sentinelConfig;
    @Inject Scheduler scheduler;

    @Override
    public Uni<ProvisionResult> provision(Set<String> capabilities, ProvisionContext context) {
        return Uni.createFrom().item(() -> {
            String capabilityName = context.taskType();

            // Idempotent: skip if already provisioned for this case + capability
            if (sentinelRegistry.isProvisioned(context.caseId(), capabilityName)) {
                return ProvisionResult.empty();
            }

            LifeAgent agent = resolveAgent(capabilityName);

            // Schedule heartbeat
            Duration interval = sentinelConfig.capabilities()
                .get(capabilityName).heartbeatInterval();
            JobKey jobKey = scheduleHeartbeat(agent, context.caseId(),
                capabilityName, interval);

            // Register in life's sentinel registry
            sentinelRegistry.register(new LifeSentinelRegistry.SentinelEntry(
                agent, context.caseId(), capabilityName, jobKey));

            return ProvisionResult.empty();
        });
    }

    @Override
    public Uni<Void> terminate(String workerId, String tenancyId) {
        return Uni.createFrom().voidItem();
    }

    public void terminateAllForCase(UUID caseId) {
        sentinelRegistry.findByCaseId(caseId).forEach(entry ->
            cancelHeartbeat(entry.heartbeatJobKey()));
        sentinelRegistry.removeByCaseId(caseId);
    }

    @Override
    public Uni<Set<String>> getCapabilities() {
        return Uni.createFrom().item(() ->
            Set.copyOf(sentinelConfig.capabilities().keySet()));
    }
}
```

**Idempotency:** `sentinelRegistry.isProvisioned(caseId, capabilityName)` is checked FIRST. The engine calls `tryProvision()` on every `CaseContextChangedEvent` where the sentinel binding evaluates as eligible. After the first successful provisioning, all subsequent calls are O(1) no-ops.

**Agent resolution:** `resolveAgent(capabilityName)` maps capability → `LifeAgent` via `LifeSentinelConfig`.

**terminate() vs terminateAllForCase():** The SPI `terminate(workerId, tenancyId)` has an impedance mismatch — it takes a `workerId` that the engine never provides (`ProvisionResult` doesn't carry one). Life uses `terminateAllForCase(caseId)` from the cleanup observer.

**NoOpReactiveWorkerProvisioner:** `@DefaultBean` — automatically displaced. No exclusion needed. `ReactiveOpenClawWorkerProvisioner` requires `@IfBuildProperty("casehub.qhorus.reactive.enabled", "true")` which is `false` — wouldn't activate regardless, stays excluded for clarity.

### 4.3 `LifeHeartbeatJob`

**Package:** `io.casehub.life.app.engine`
**Type:** Quarkus-managed Quartz job with CDI injection

```java
@ApplicationScoped
public class LifeHeartbeatJob implements Job {

    @Inject LifeOpenClawChatModelFactory openClawFactory;
    @Inject CaseHubRuntime caseHubRuntime;

    @Override
    @SuppressWarnings("unchecked")
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap data = ctx.getMergedJobDataMap();
        LifeAgent agent = LifeAgent.valueOf(data.getString("agent"));
        UUID caseId = UUID.fromString(data.getString("caseId"));
        String capabilityName = data.getString("capabilityName");

        // Query current case context — fresh each tick, not a stale snapshot
        Map<String, Object> caseContext = (Map<String, Object>)
                caseHubRuntime.query(caseId, ".").toCompletableFuture().join();

        Agent sentinelAgent = Agent.builder()
                .model(openClawFactory.forAgent(agent))
                .systemPrompt(sentinelSystemPrompt(agent, capabilityName))
                .responseSchema(sentinelResponseSchema(capabilityName))
                .build();

        WorkerResult result = sentinelAgent.execute(caseContext);

        caseHubRuntime.signal(caseId, "sentinelReport", result.output())
                .toCompletableFuture().join();
    }
}
```

**Case context querying:** `CaseHubRuntime.query(caseId, ".")` retrieves the current case context before each heartbeat invocation. The agent receives fresh context (contractor name, job details, current status) as its execution input — not a stale snapshot from provisioning time. The agent uses this context alongside OpenClaw's skill ecosystem (messaging, APIs) for independent real-world verification.

**Direct signaling:** `CaseHubRuntime.signal(caseId, "sentinelReport", reportMap)` sets `.sentinelReport` in case context and triggers binding evaluation. No channel indirection needed.

**Per-sentinel response schema:** `sentinelResponseSchema(capabilityName)` returns the appropriate response schema class (one of 7 per-sentinel records). See §4.4.

**Blocking is acceptable:** Quartz jobs run on their own thread pool (separate from Vert.x IO threads). `Agent.execute()` blocks via `DirectCallBridge.call()` with timeout (default 120s). `query()` and `signal()` are fast engine-internal operations.

**CDI injection:** Quarkus-quartz provides native CDI injection for `@ApplicationScoped` Quartz jobs. No `CDI.current()`.

### 4.4 Per-sentinel response schemas

**Package:** `io.casehub.life.app.engine.agent`
**Type:** 7 Java records (one per sentinel type)

Consistent with the 32 existing AgentExec response schemas. Each sentinel gets fields that match its domain.

```java
public record ContractorSentinelReport(
    int progressPercent, String status, String concerns,
    String recommendedAction, boolean escalationRequired) {}

public record MaintenanceSentinelReport(
    int progressPercent, String status, String concerns,
    String recommendedAction, boolean escalationRequired) {}

public record FollowUpSentinelReport(
    List<String> pendingActions, int daysOverdue,
    String concerns, boolean escalationRequired) {}

public record CareQualitySentinelReport(
    int sessionsScheduled, int sessionsCompleted, List<String> missedSessions,
    String concerns, boolean escalationRequired) {}

public record PatientStatusSentinelReport(
    String conditionSummary, String trend,
    List<String> alerts, boolean escalationRequired) {}

public record AnomalySentinelReport(
    List<String> anomalies, String severity,
    String concerns, boolean escalationRequired) {}

public record BookingSentinelReport(
    String bookingStatus, boolean priceChanged, String priceChangeDetail,
    List<String> alerts, boolean escalationRequired) {}
```

### 4.5 `LifeProvisionerCleanupObserver`

**Package:** `io.casehub.life.app.engine`
**Type:** `@ApplicationScoped` CDI observer

Handles provisioner termination — the engine never calls `terminate()` (verified: no call site exists in engine runtime bytecode).

```java
@ApplicationScoped
public class LifeProvisionerCleanupObserver {

    @Inject LifeReactiveWorkerProvisioner provisioner;

    public void onCaseTerminal(@ObservesAsync CaseLifecycleEvent event) {
        if (isTerminal(event.eventType())) {
            provisioner.terminateAllForCase(event.caseId());
        }
    }

    private boolean isTerminal(String eventType) {
        return "CaseCompleted".equals(eventType)
            || "CaseFaulted".equals(eventType)
            || "CaseCancelled".equals(eventType);
    }
}
```

Event type strings verified against engine bytecode (`CaseStatusChangedHandler.resolveEventType()`).

### 4.6 `LifeSentinelConfig`

**Package:** `io.casehub.life.app.engine`
**Type:** `@ConfigMapping` interface

Life-owned configuration. Does NOT use `OpenClawCasehubConfig` — avoids coupling to openclaw-casehub's config format, avoids needing to satisfy its required fields (e.g., `oversight()`), and gives life full control.

```java
@ConfigMapping(prefix = "casehub.life.sentinel")
public interface LifeSentinelConfig {
    Map<String, SentinelCapabilityEntry> capabilities();

    interface SentinelCapabilityEntry {
        String agent();                  // LifeAgent enum name: HEALTH, HOME, FINANCE, TRAVEL
        Duration heartbeatInterval();
    }
}
```

## 5. Sentinel Capabilities — 7 Case Plans

### 5.1 Sentinel capability mapping

| Sentinel capability | LifeAgent | Case plan | What it monitors |
|---|---|---|---|
| `follow-up-sentinel` | HEALTH | appointment-cycle | Prescriptions filled, referrals made, follow-up booked |
| `care-quality-sentinel` | HEALTH | care-coordination | Session adherence, missed visits, escalation of concerns |
| `patient-status-sentinel` | HEALTH | care-episode | Patient condition between episodes, alerts on change |
| `maintenance-sentinel` | HOME | home-maintenance | Job progress, contractor response, completion verification |
| `contractor-sentinel` | HOME | contractor-coordination | Quote response, job progress, payment follow-up |
| `anomaly-sentinel` | FINANCE | financial-review | Transaction anomalies, budget drift, oversight follow-up |
| `booking-sentinel` | TRAVEL | travel-plan | Booking confirmations, price changes, availability |

### 5.2 YAML binding pattern

Each case plan adds a sentinel capability and binding. The binding has NO inline worker — the engine falls through to the provisioner. Example for contractor-coordination:

```yaml
capabilities:
  # ... existing capabilities unchanged ...
  - name: contractor-sentinel
    description: "Persistent heartbeat monitor for contractor progress and follow-up"
    inputSchema: "."
    outputSchema: "."

bindings:
  # ... existing bindings unchanged ...
  - name: contractor-sentinel
    on: { contextChange: {} }
    when: ".contractorRequest != null"
    capability: contractor-sentinel
```

**Re-firing prevention:** The binding condition `.contractorRequest != null` stays true for the case's lifetime. The engine calls `tryProvision()` on every context change where the binding is eligible. The provisioner's idempotency guard (`sentinelRegistry.isProvisioned(caseId, capabilityName)`) ensures only the first call does work — all subsequent calls are O(1) no-ops.

### 5.3 Sentinel result processing

Each heartbeat signals `caseHubRuntime.signal(caseId, "sentinelReport", reportMap)`. This sets `.sentinelReport` in case context. Case plan bindings react to `.sentinelReport` fields directly:

```yaml
  - name: sentinel-escalation
    on: { contextChange: {} }
    when: ".sentinelReport != null and .sentinelReport.escalationRequired == true and (.sentinelEscalation == null or .sentinelEscalation.resolved == true)"
    humanTask:
      title: "Sentinel detected issue — review contractor progress"
      expiresIn: PT24H
      candidateGroups: [household-admin]
      scope: "casehubio/life/household"
      inputMapping: "{ sentinelReport: .sentinelReport }"
      outputMapping: "{ sentinelEscalation: . }"
```

Each heartbeat overwrites `.sentinelReport` with the latest data. The escalation binding fires when `escalationRequired == true` and no unresolved escalation exists. The human task output includes a `resolved` field — when the admin marks the issue as handled (`.sentinelEscalation.resolved = true`), the guard resets. If a later heartbeat finds `escalationRequired == true` again (e.g., contractor was back on track then stalled), the binding fires again. This allows re-escalation for recurring issues while preventing duplicate escalations for the same active issue.

### 5.4 Sentinel system prompts (per capability)

**contractor-sentinel:**
```
You are a contractor progress monitoring agent for a UK household.
Check on the status of the active contractor job for this case.
Report current progress, status (on-track/delayed/stalled),
any concerns, and recommended actions.
```

**anomaly-sentinel:**
```
You are a financial anomaly detection agent for a UK household.
Scan recent transactions for unusual patterns, budget overruns,
or suspicious activity. Report anomalies found, severity, and
whether human review is recommended.
```

**follow-up-sentinel:**
```
You are a health appointment follow-up agent for a UK household.
Check whether post-appointment actions have been completed:
prescriptions collected, referrals booked, test results received.
Report pending actions, days overdue, and whether escalation is needed.
```

## 6. CDI Bean Activation Changes

### All openclaw-casehub beans STAY EXCLUDED

The sentinel architecture uses no openclaw-casehub infrastructure beans. Dependencies:
- `LifeSentinelRegistry` (life's own — no 1:1 constraint)
- `LifeOpenClawChatModelFactory` + `DirectCallBridge` (already active for AgentExec)
- `CaseHubRuntime` (engine-api — already active, used by `LifeCaseService`)
- Quartz `Scheduler` (already available via casehub-engine-scheduler-quartz)

The existing CDI exclusion list is unchanged.

### NoOpReactiveWorkerProvisioner

`@DefaultBean` — automatically displaced by `LifeReactiveWorkerProvisioner`. No exclusion needed.

## 7. Configuration Additions

### application.properties (production)

```properties
# Sentinel capabilities — capability → agent + heartbeat interval
casehub.life.sentinel.capabilities.contractor-sentinel.agent=HOME
casehub.life.sentinel.capabilities.contractor-sentinel.heartbeat-interval=PT4H
casehub.life.sentinel.capabilities.maintenance-sentinel.agent=HOME
casehub.life.sentinel.capabilities.maintenance-sentinel.heartbeat-interval=PT4H
casehub.life.sentinel.capabilities.follow-up-sentinel.agent=HEALTH
casehub.life.sentinel.capabilities.follow-up-sentinel.heartbeat-interval=PT12H
casehub.life.sentinel.capabilities.care-quality-sentinel.agent=HEALTH
casehub.life.sentinel.capabilities.care-quality-sentinel.heartbeat-interval=PT12H
casehub.life.sentinel.capabilities.patient-status-sentinel.agent=HEALTH
casehub.life.sentinel.capabilities.patient-status-sentinel.heartbeat-interval=PT24H
casehub.life.sentinel.capabilities.anomaly-sentinel.agent=FINANCE
casehub.life.sentinel.capabilities.anomaly-sentinel.heartbeat-interval=PT24H
casehub.life.sentinel.capabilities.booking-sentinel.agent=TRAVEL
casehub.life.sentinel.capabilities.booking-sentinel.heartbeat-interval=PT6H

# Quartz thread pool — 7 sentinel jobs + casehub-work SLA jobs
quarkus.quartz.thread-pool-size=15
```

### test application.properties

Same sentinel config with short intervals (e.g., `PT1S`) for test speed. `TestLifeReactiveWorkerProvisioner` (`@Alternative @Priority(10)`) replaces the production provisioner — tracks provisioning calls for assertion, skips heartbeat scheduling and LLM invocation.

## 8. Testing Strategy

### Unit tests

- **`LifeSentinelRegistryTest`** — plain unit test: register, isProvisioned (idempotency), findByCaseId, removeByCaseId, concurrent cases with same agent type
- **`LifeReactiveWorkerProvisionerTest`** — mock registry, scheduler. Verify: first provision() registers + schedules. Second provision() for same (caseId, capability) is no-op. terminateAllForCase() cancels heartbeats + deregisters.
- **`LifeProvisionerCleanupObserverTest`** — mock provisioner. Fire CaseLifecycleEvent with each terminal type (CaseCompleted, CaseFaulted, CaseCancelled). Verify terminateAllForCase() called. Non-terminal events ignored.
- **`LifeHeartbeatJobTest`** — mock openClawFactory, caseHubRuntime. Verify Agent.execute() called with correct system prompt and per-sentinel response schema. Verify signal() called with "sentinelReport" path and result output.

### Integration tests

- **`LifeProvisionerIntegrationTest`** (`@QuarkusTest`) — start a case with a sentinel binding (no inline worker). Verify provisioner invoked. Verify sentinel registered in LifeSentinelRegistry. Verify idempotency on re-provision. Simulate case completion → verify terminateAllForCase() called, sentinel deregistered.
- **Existing CaseHub integration tests** — must pass unchanged.

### Test infrastructure

`TestLifeReactiveWorkerProvisioner` (`@Alternative @Priority(10)`) — records provisioning calls in a list. Idempotent. No LLM calls, no Quartz scheduling.

## 9. Platform Coherence

- **Right repo:** Life owns sentinel domain logic. Foundation provides the provisioner SPI and CaseHubRuntime signal API.
- **Right abstraction:** `ReactiveWorkerProvisioner` is the engine SPI. Life implements it with heartbeat scheduling — an execution concern the SPI leaves to implementers.
- **No openclaw-casehub dependency for sentinels:** Sentinels reuse the DirectCallBridge path (already active for AgentExec) and CaseHubRuntime (already active for LifeCaseService). The openclaw provisioner infrastructure (registry, channel backend, status listener) is designed for a different model (1:1 agent↔case, autonomous agent with channel access). Sentinels are simpler — periodic LLM calls with direct case signaling.
- **LifeAgent reuse:** Sentinel config maps capabilities to `LifeAgent` enum constants. Same agents serve both AgentExec workers and sentinels. `LifeOpenClawChatModelFactory.forAgent(LifeAgent)` is shared.
- **Engine gap documented:** The engine does not call `terminate()`. Life handles termination via `CaseLifecycleEvent` observer. Idempotent — safe if the engine adds `terminate()` calls in the future.
- **agentId coupling:** `LifeAgent.agentId()` format is the single source of truth across all uses.

## 10. Follow-On Issues

| Issue | Title | Purpose |
|---|---|---|
| (to file on engine) | Engine: call `terminate()` on provisioner when case reaches terminal state | Engine should own provisioner lifecycle end-to-end |
| (to file on engine) | Engine: add timer sentry type for periodic binding evaluation | Declarative heartbeat bindings in YAML — cleaner than Quartz scheduling |
| (to file on engine) | Engine: extend QhorusMessageSignalBridge to route STATUS messages | Enable channel-based result delivery for provisioned workers |
| (to file on openclaw) | OpenClawAgentRegistry: support 1:N agentId→caseId mapping | Remove MVP 1:1 constraint for concurrent same-agent cases |
| life#47 | Structural CaseHub duplication | Extract augment pattern, cap() helper, double-checked lock |

## 11. Protocol Impact

- **PP-20260618-openclaw-agent** — add section on provisioner mode (coexists with AgentExec; sentinel capabilities use provisioner path with heartbeat scheduling and direct CaseHubRuntime signaling)
- **CLAUDE.md Layer 7** — update from "PENDING (WorkerProvisioner heartbeat)" to "COMPLETE"
- **New protocol** — `sentinel-heartbeat-pattern.md`: when to use sentinel capabilities vs AgentExec workers; heartbeat interval guidelines; idempotent provisioner pattern; cleanup observer pattern; sentinel capabilities are reserved (no inline workers); CaseHubRuntime.signal() for result delivery
