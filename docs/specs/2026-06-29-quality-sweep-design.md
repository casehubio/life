# Quality Sweep — Issues #30, #31, #41, #42, #43

**Date:** 2026-06-29
**Branch:** issue-30-quality-sweep
**Covers:** #30 (audit), #31 (ledger fields), #41 (visibility), #42 (MCP config), #43 (exclude-types)

---

## 1. #42 — MCP client config for Qhorus

### Finding

`quarkus-mcp-server-http` is transitively on life's classpath from `casehub-qhorus` (compile scope in qhorus runtime). The Quarkus MCP server extension auto-activates at build time, exposing two tool surfaces:

- **Default namespace:** OpenClaw accountability tools (`casehub_commit`, `casehub_done`, `casehub_reject`, `casehub_checkpoint`, `casehub_escalate`, etc.) — from `casehub-openclaw-casehub`
- **`qhorus` named server:** 39 channel management, messaging, and commitment tracking tools — from `casehub-qhorus` via `@McpServer("qhorus")`

This is by platform design: PLATFORM.md says "Library modules that expose MCP tools must use `@McpServer`" and qhorus follows this convention. Every application embedding qhorus gets the MCP tools automatically.

### Verification

**Question from issue #42:** "If Qhorus is only embedded for internal CDI services, no change needed."

**Answer:** Qhorus IS only embedded for internal CDI services. casehub-life is a personal automation harness — not a Claude integration surface like claudony. Life's MCP tools are consumed internally by CDI services via Java injection, not MCP transport. No external Claude agent, IDE, or tool connects to life's MCP endpoint. Claudony is the authenticated entry point for all Claude agent sessions (PLATFORM.md §Gateway topology).

The MCP surface auto-activating is benign: it exposes tools nobody calls externally.

### Changes

No changes needed. The auto-activated MCP surface has no external consumers.

---

## 2. #43 — Remove CurrentPrincipal exclude-types

### Rationale

Platform #111 shipped `OidcCurrentPrincipal @Alternative @Priority(100)`. Platform #112 shipped `FixedCurrentPrincipal @Alternative @Priority(200)`. CDI `@Alternative` resolution supersedes all non-alternative beans — exclude-types entries for CurrentPrincipal disambiguation are no longer needed.

Verified from bytecode:
- `FixedCurrentPrincipal`: `@Alternative @Priority(200)`, tenancyId `278776f9-e1b0-46fb-9032-8bddebdcf9ce` (canonical test ID)
- `TenantScopedPrincipal`: `@RequestScoped @Unremovable` — NOT `@Alternative`, superseded
- `QhorusInboundCurrentPrincipal`: `@ApplicationScoped` — NOT `@Alternative`, superseded

Production winner: `OidcCurrentPrincipal @Alternative @Priority(100)`.
Test winner: `FixedCurrentPrincipal @Alternative @Priority(200)`.

### Changes

**Production `application.properties`** — remove from `quarkus.arc.exclude-types`:
- `io.casehub.platform.mock.MockCurrentPrincipal`
- `io.casehub.qhorus.runtime.identity.QhorusInboundCurrentPrincipal`
- `io.casehub.persistence.memory.DefaultTestPrincipal`
- `io.casehub.work.runtime.service.TenantScopedPrincipal`

**Test `application.properties`** — remove from `quarkus.arc.exclude-types`:
- `io.casehub.qhorus.runtime.identity.QhorusInboundCurrentPrincipal`
- `io.casehub.work.runtime.service.TenantScopedPrincipal`

Keep all non-CurrentPrincipal exclusions unchanged.

**Protocol:** Retire `docs/protocols/casehub-life/current-principal-cdi-exclusion.md`. Replace content with a note: CDI `@Alternative @Priority` resolution handles CurrentPrincipal disambiguation since platform#112.

**CLAUDE.md:** Update the "CurrentPrincipal disambiguation" section to reflect `@Alternative`-based resolution.

**Prerequisite:** Pull latest platform and qhorus SNAPSHOTs. Build and run all tests.

---

## 3. #31 — Populate or remove unpopulated ledger entry fields

### appointmentDate — REMOVE

`HealthDecisionLedgerEntry.appointmentDate` has no data source. `BookingResult` (health agent response schema) has `appointmentId`, `provider`, `confirmed` — no date/time. `WorkItem`, `LifeTaskContext`, and `CreateLifeTaskRequest` have no appointment-specific date field. The field was premature — added to the schema before a population path existed.

Changes:
- Remove `appointmentDate` field and `@Column` from `HealthDecisionLedgerEntry`
- Remove from `domainContentBytes()` — hash structure changes (one fewer pipe segment)
- Add migration `V2104__drop_health_appointment_date.sql`: `ALTER TABLE health_decision_ledger_entry DROP COLUMN appointment_date`
- Update `LedgerEntryDomainContentBytesTest`

Merkle hash impact: no production data exists (development harness). Schema and hash changes are safe.

### jurisdiction — POPULATE

`LegalActionLedgerEntry.jurisdiction` has a data source: `@ConfigProperty(name = "casehub.life.jurisdiction", defaultValue = "GB")` exists in `LifeAgentDescriptorFactory`. This is the tenant's home jurisdiction — appropriate for a single-tenant personal life harness.

**Semantics:** The field captures the tenant's operative jurisdiction at the time of the legal action — not the per-action legal jurisdiction. A single household principal may have legal actions spanning multiple jurisdictions (UK tax filing, EU GDPR erasure, US immigration). The tenant-wide config produces a correct default for the common case (most actions fall under the home jurisdiction) but does not model cross-jurisdiction actions. This is accepted for the personal harness scope.

**Limitation:** If per-action jurisdiction is needed in the future, the population path would be a `jurisdiction` field on `CreateLifeTaskRequest` for legal-domain tasks, or derived from the legal obligation's properties. Filed as life#48 to track.

Changes:
- `LegalDomainLedgerHandler`: inject `casehub.life.jurisdiction` config, set `entry.jurisdiction` on every write
- `LegalDomainLedgerHandlerTest`: assert `jurisdiction` is populated with config value
- `LedgerEntryDomainContentBytesTest`: already tests non-null jurisdiction (no change needed)

No schema migration needed — column already exists and is nullable.

---

## 4. #30 — Second-pass scattered business logic audit

### Audit result: 3 violations in 2 files

All other `app/` classes are clean. Static maps eliminated by #27. Domain enum switches eliminated by descriptor pattern. Resource classes delegate cleanly. Entity classes have no policy logic.

### Violation 1+2: FINANCE hardcode in OversightGateStrategy and LifeWatchdogAlertObserver

**Root cause:** `LifeCommitmentRecord` has no `domain` field. Both classes hardcode `LifeDomain.FINANCE` when filtering `DomainLedgerHandler` instances, coupling oversight gates to the finance domain. Oversight gates should work for any domain (elder care, health, legal).

**Fix:**
- Add `LifeDomain domain` to `LifeCommitmentRecord` entity
- Add migration `V108__commitment_record_domain.sql`: `ALTER TABLE life_commitment_record ADD COLUMN domain VARCHAR(50)`
- Populate `record.domain` at creation time in all three commitment strategies:
  - `DelegationCommitmentStrategy` and `ContractorCommitmentStrategy`: from `taskContext.domain` (available via `DelegationContext.taskContext()` / `ContractorContext.taskContext()`)
  - `OversightGateStrategy`: from `OversightGateRequest.domain()` — a new required field on the request (see below)
- Replace `h -> h.domain() == LifeDomain.FINANCE` with `h -> h.domain() == record.domain` in both OversightGateStrategy and LifeWatchdogAlertObserver

**Domain path for OversightGateStrategy:** `OversightContext` contains only `OversightGateRequest`, which currently has no domain field. Unlike `DelegationContext`/`ContractorContext` (which carry `LifeTaskContext` with `domain`), oversight gates have no WorkItem yet — that's the whole point. Add `@NotNull LifeDomain domain` to `OversightGateRequest` in `casehub-life-api`. The REST caller specifies which domain the oversight gate covers. The strategy reads `oc.request().domain()` to populate `record.domain`.

### Violation 2b: CommitmentMode switch for escalation titles in LifeWatchdogAlertObserver

**Root cause:** Escalation title text is domain knowledge scattered in an observer.

**Fix:** Add `escalationTemplate()` to `CommitmentMode` enum in `casehub-life-api`. Each mode carries its own escalation text template — same principle as `reasonTemplate()` on `HouseholdActionType`. The enum is in `casehub-life-api` (life-specific, pure Java, no framework dependency).

```java
public enum CommitmentMode {
    DELEGATION("%s has not confirmed — action required"),
    CONTRACTOR("Contractor has not confirmed by deadline"),
    OVERSIGHT("Oversight gate expired — request not approved");

    private final String escalationTemplate;
    CommitmentMode(String escalationTemplate) { this.escalationTemplate = escalationTemplate; }
    public String escalationTemplate() { return escalationTemplate; }
}
```

The observer's `createEscalationTask()` collapses to:
```java
String title = record.mode == CommitmentMode.DELEGATION
    ? record.mode.escalationTemplate().formatted(
        record.delegateTo != null ? record.delegateTo : "Unknown")
    : record.mode.escalationTemplate();
```

This is consistent with putting `reasonTemplate()` on `HouseholdActionType`: text that is inherent to the type lives on the type. The observer owns interpolation, not template selection. The strategy is not involved — escalation is an observer concern, not a commitment execution concern.

### Violation 2c: delegateTo semantic overload

**Root cause:** `LifeCommitmentRecord.delegateTo` is repurposed as a dedup key for OVERSIGHT mode (format "title:templateRef"), while for DELEGATION it holds the principal ID. The observer's `createEscalationTask()` uses `delegate.contains(":")` to detect oversight-style keys in a field that should only contain principal IDs — yielding the wrong escalation message for a wrong data state.

ARC42STORIES.MD acknowledges: "delegateTo column repurposed as dedup key for OVERSIGHT mode. Acknowledged semantic overload — a dedicated oversight_key column would be cleaner."

**Fix:** Add a dedicated `oversightKey` column to resolve the overload:
- Add `String oversightKey` to `LifeCommitmentRecord` entity: `@Column(name = "oversight_key", length = 255)`
- Add migration `V109__commitment_record_oversight_key.sql`: `ALTER TABLE life_commitment_record ADD COLUMN oversight_key VARCHAR(255)`
- `OversightGateStrategy`: set `record.oversightKey = taskKey` instead of `record.delegateTo = taskKey`; set `record.delegateTo = null` (oversight gates have no delegate)
- Update dedup query to use `oversightKey` instead of `delegateTo`
- Observer DELEGATION case: remove the `contains(":")` guard — `delegateTo` now always means a principal ID for DELEGATION records
- Update ARC42STORIES.MD to remove the acknowledged debt

### Violation 3: HouseholdActionType switches in LifeActionRiskClassifier

**Root cause:** Threshold key resolution and reason text are in classifier switch statements instead of on the enum.

**Fix — two parts with different module placement:**

**Part A — `reasonTemplate()` on the enum (api/):** Add `reasonTemplate()` to `HouseholdActionType` returning a format string for the human-readable gate reason. This is pure Java — a `String` return type, no framework dependency. The classifier's `buildReason()` becomes a uniform single-line call for all gated types: `type.reasonTemplate().formatted(formatAmount(action.parameters()))`.

**Contract:** `reasonTemplate()` returns `null` for NEVER-gated types. Callers must verify `gatePolicy() != NEVER` before calling — this is already guaranteed by the classifier's control flow (`classifyKnownType()` returns `Autonomous` immediately for NEVER types, so `buildGate()`/`buildReason()` are unreachable). The `@Nullable` return is documented in Javadoc.

Each type's template (all `%s` slots filled by `formatAmount()` — no per-type conditional logic):
- `SPEND_PURCHASE`, `SPEND_SUBSCRIPTION_MODIFY`: `"Spend of %s requires household approval"`
- `SPEND_SUBSCRIPTION_CANCEL`: `"Subscription cancellation — confirm before proceeding"`
- `BOOKING_NONREFUNDABLE`: `"Non-refundable booking of %s — cannot be undone once confirmed"`
- `BOOKING_REFUNDABLE`: `"Refundable booking of %s requires household approval"`
- `HEALTH_APPOINTMENT_SPECIALIST`: `"Specialist appointment referral — confirm before booking"`
- `HEALTH_MEDICATION_FLAG`: `"Medication concern — family awareness required before any action"`
- `CONTRACTOR_ENGAGE`: `"Contractor instruction estimated at %s — approval required"`
- `LEGAL_DOCUMENT_SUBMIT`: `"Legal document submission — confirm before filing (irreversible)"`
- `ELDER_CARE_DECISION`: `"Care decision for dependent — family approval required"`
- `HEALTH_APPOINTMENT_GP`: `null` (NEVER gated — `buildReason()` is unreachable)

**Part B — threshold key resolution in app/ (not on enum):** `PreferenceKey` is from `casehub-platform-api`. Adding it to `HouseholdActionType` would require `casehub-platform-api` as a compile dependency of `casehub-life-api`, breaking the module-tier-structure constraint (api/ is pure Java, zero framework). The enum already documents this: "Threshold key resolution is handled in app/ routing via LifeRiskPolicyKeys, not in this enum."

Instead, create `HouseholdActionThresholdKeys` in `app/routing/` — a static map from `HouseholdActionType` to `ThresholdKeyPair(PreferenceKey<DoublePreference> member, PreferenceKey<DoublePreference> admin)`:

```java
public final class HouseholdActionThresholdKeys {
    private static final Map<HouseholdActionType, ThresholdKeyPair> KEYS = Map.of(
        SPEND_PURCHASE,            new ThresholdKeyPair(SPEND_THRESHOLD, ADMIN_SPEND_THRESHOLD),
        SPEND_SUBSCRIPTION_MODIFY, new ThresholdKeyPair(SPEND_THRESHOLD, ADMIN_SPEND_THRESHOLD),
        BOOKING_REFUNDABLE,        new ThresholdKeyPair(BOOKING_THRESHOLD, ADMIN_BOOKING_THRESHOLD),
        CONTRACTOR_ENGAGE,         new ThresholdKeyPair(CONTRACTOR_THRESHOLD, ADMIN_CONTRACTOR_THRESHOLD)
    );
    public static ThresholdKeyPair forType(HouseholdActionType type) {
        return Objects.requireNonNull(KEYS.get(type),
            "No threshold keys for non-AMOUNT_THRESHOLD type: " + type);
    }
}
```

The classifier's `resolveThreshold()` becomes: `HouseholdActionThresholdKeys.forType(type).resolve(prefs, admin)`. Adding a new AMOUNT_THRESHOLD type means one new map entry — zero changes to the classifier.

---

## 5. #41 — Data-level task visibility filter for household-junior

### Design

Implement as a visibility policy SPI per issue #41's implementation notes and the auth-retrofit-readiness protocol (PP-20260513). The protocol's §"When Auth Is Introduced" prescribes: "`WorkItemAccessPolicy` SPIs defined in `api/` with permissive no-op defaults. Query layer calls the SPI. Application tier provides domain-specific implementations."

### Ownership semantics

A task is visible to household-junior if:
- `WorkItem.assigneeId == principal.actorId()` (assigned/claimed), OR
- `household-junior` is in `WorkItem.candidateGroups` (eligible to claim)

Admins and members see everything — filter only restricts juniors.

### SPI structure

**`casehub-life-api/spi/LifeTaskVisibilityPolicy`** (pure Java interface):
```java
public interface LifeTaskVisibilityPolicy {
    boolean isVisible(LifeTaskResponse task, String actorId, Set<String> groups);
}
```

**`app/spi/DefaultLifeTaskVisibilityPolicy`** (`@DefaultBean` — permissive no-op):
```java
@DefaultBean @ApplicationScoped
public class DefaultLifeTaskVisibilityPolicy implements LifeTaskVisibilityPolicy {
    public boolean isVisible(LifeTaskResponse task, String actorId, Set<String> groups) {
        return true;
    }
}
```

**`app/spi/JuniorLifeTaskVisibilityPolicy`** (`@Alternative @Priority(1)` — restrictive):
```java
@Alternative @Priority(1) @ApplicationScoped
public class JuniorLifeTaskVisibilityPolicy implements LifeTaskVisibilityPolicy {
    public boolean isVisible(LifeTaskResponse task, String actorId, Set<String> groups) {
        if (!groups.contains(HouseholdGroups.JUNIOR)) return true;
        if (groups.contains(HouseholdGroups.ADMIN) || groups.contains(HouseholdGroups.MEMBER)) return true;
        return actorId.equals(task.assigneeId())
            || task.candidateGroups().stream().anyMatch(groups::contains);
    }
}
```

### Changes

**`LifeTaskResponse`** — add `assigneeId` and `candidateGroups` fields:
- `String assigneeId` (nullable — null if unassigned)
- `List<String> candidateGroups`

These fields are populated by `LifeTaskService.get()` from the already-loaded `WorkItem`. No additional database fetch required — the service already loads `WorkItem` to build the response.

**`LifeTaskResource.get()`** — inject `LifeTaskVisibilityPolicy` and `CurrentPrincipal`:
- After `lifeTaskService.get(id)` returns, call `policy.isVisible(response, principal.actorId(), principal.groups())`
- Return 404 (not 403) if not visible — avoids leaking task existence

The visibility check uses fields already on the response object. No redundant WorkItem fetch.

---

## Platform coherence review

| Check | Result |
|-------|--------|
| Descriptor+handler protocol (PP-20260609-bd9d27) | #30 fixes align — eliminates remaining switches; threshold keys in app/ descriptor |
| Module-tier-structure protocol | #30 respects api/ boundary — `reasonTemplate()` pure Java; threshold keys in app/ |
| Auth-retrofit-readiness protocol (PP-20260513) | #41 SPI approach — `LifeTaskVisibilityPolicy` with `@DefaultBean`/`@Alternative` |
| Named MCP server convention (PLATFORM.md) | #42 verified — no external MCP clients; no changes needed |
| Flyway version ranges | V108, V109 (default ds), V2104 (qhorus ds) — within allocated ranges |
| Peer repo boundary | No commits to peer repos. No deferred cross-repo issues identified |
| domainContentBytes() stability | Hash structure changes for appointmentDate removal — acceptable, no production data |
