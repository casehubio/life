# ActionRiskClassifier Design — casehub-life Layer 7

**Issue:** life#20
**Date:** 2026-06-07
**Status:** approved

## Context

`casehub-engine-api` now ships `ActionRiskClassifier` (engine#402): a SPI interface that classifies a `PlannedAction` as either `Autonomous` (proceed) or `GateRequired(reason, reversible, candidateGroups, expiresIn, scope)` (pause for human approval). The engine's `ChainedReactiveActionRiskClassifier` collects all `@RiskClassifier`-annotated implementations via CDI `Instance<ActionRiskClassifier>`, reduces via `mostRestrictive()`, and routes to the oversight channel on `GateRequired`.

This spec covers the life-specific implementation: action type taxonomy, risk policy configuration, and the classifier itself.

---

## What Changes

| Type | Location | Purpose |
|------|----------|---------|
| `HouseholdActionType` | `api/` | Domain vocabulary — typed action type enum |
| `HouseholdGroups` | `api/` | Group name constants for approval routing |
| `LifeRiskPolicyKeys` | `app/routing/` | `PreferenceKey` constants for YAML thresholds |
| `LifeActionRiskClassifier` | `app/routing/` | `@RiskClassifier @ApplicationScoped` implementation |
| `risk-policy.yaml` | `app/resources/casehub/life/` | Operational thresholds and approval expiry |

No new Flyway migrations. `casehub-platform-config` already wired from Layer 6.

---

## Action Type Taxonomy

`HouseholdActionType` enum in `api/`. Each constant carries three inherent domain properties:

| Constant | Gate Policy | Reversible | Candidate Groups |
|----------|-------------|------------|-----------------|
| `SPEND_PURCHASE` | AMOUNT_THRESHOLD | true | `[household-admin]` |
| `SPEND_SUBSCRIPTION_CANCEL` | ALWAYS | true | `[household-admin]` |
| `SPEND_SUBSCRIPTION_MODIFY` | AMOUNT_THRESHOLD | true | `[household-admin]` |
| `BOOKING_NONREFUNDABLE` | ALWAYS | **false** | `[household-admin]` |
| `BOOKING_REFUNDABLE` | AMOUNT_THRESHOLD | true | `[household-admin]` |
| `HEALTH_APPOINTMENT_SPECIALIST` | ALWAYS | true | `[household-admin]` |
| `HEALTH_APPOINTMENT_GP` | NEVER | true | `[]` |
| `HEALTH_MEDICATION_FLAG` | ALWAYS | **false** | `[household-admin, household-member]` |
| `CONTRACTOR_ENGAGE` | AMOUNT_THRESHOLD | true | `[household-admin]` |
| `LEGAL_DOCUMENT_SUBMIT` | ALWAYS | **false** | `[household-admin]` |
| `ELDER_CARE_DECISION` | ALWAYS | true | `[household-admin, household-member]` |

**Gate policy semantics:**
- `ALWAYS` — unconditional gate regardless of amount
- `AMOUNT_THRESHOLD` — gate when `context["amount"] >= configured threshold`
- `NEVER` — always autonomous (routine operations)

**Candidate groups rationale:**
- Default gated actions: `[household-admin]` only (1 group = narrower per engine `mostRestrictive()` semantics)
- `HEALTH_MEDICATION_FLAG` and `ELDER_CARE_DECISION`: `[household-admin, household-member]` — urgency warrants faster response from any adult

**Action type strings:** derived from enum name via `name().toLowerCase().replace('_', '.')`.  
`SPEND_PURCHASE` → `"spend.purchase"`, `BOOKING_NONREFUNDABLE` → `"booking.nonrefundable"`, etc.  
Workers construct `PlannedAction` using `HouseholdActionType.SPEND_PURCHASE.actionType()`.

---

## Risk Policy Configuration

### YAML (`app/resources/casehub/life/risk-policy.yaml`)

Single scope for all household-global thresholds:

```yaml
entries:
  - scope: casehubio/life/risk-policy
    casehubio.life.risk-policy.spend.threshold: "100.0"
    casehubio.life.risk-policy.contractor.threshold: "200.0"
    casehubio.life.risk-policy.booking.threshold: "150.0"
    casehubio.life.risk-policy.approval.expires-hours: "24.0"
```

### PreferenceKeys (`LifeRiskPolicyKeys` in `app/routing/`)

Namespace: `casehubio.life.risk-policy`

| Key | Default | Used by |
|-----|---------|---------|
| `spend.threshold` | 100.0 | SPEND_PURCHASE, SPEND_SUBSCRIPTION_MODIFY |
| `contractor.threshold` | 200.0 | CONTRACTOR_ENGAGE |
| `booking.threshold` | 150.0 | BOOKING_REFUNDABLE |
| `approval.expires-hours` | 24.0 | all GateRequired decisions |

`SPEND_SUBSCRIPTION_MODIFY` shares `SPEND_THRESHOLD` — the concern is the same financial one as a purchase.

---

## Classifier

### Activation

`LifeActionRiskClassifier` is annotated `@ApplicationScoped @RiskClassifier`. The engine's `ChainedReactiveActionRiskClassifier` discovers it via `@Inject @RiskClassifier Instance<ActionRiskClassifier>`. No `@Alternative` needed — the engine collects all `@RiskClassifier` beans and takes the most restrictive result.

### Decision logic

```
classify(PlannedAction action):
  type = HouseholdActionType.fromActionType(action.actionType())
  if absent: return Autonomous          // unknown type — don't gate

  switch type.gatePolicy():
    ALWAYS:           return buildGate(type, action)
    NEVER:            return Autonomous
    AMOUNT_THRESHOLD:
      amount = context["amount"] parsed as double
      if missing or unparseable: return Autonomous
      threshold = resolveThreshold(type)
      return amount >= threshold ? buildGate(...) : Autonomous
```

### GateRequired fields

| Field | Value |
|-------|-------|
| `reason` | Human-readable per action type (e.g. "Non-refundable booking of GBP 350 — cannot be undone") |
| `reversible` | From `HouseholdActionType.reversible()` |
| `candidateGroups` | From `HouseholdActionType.candidateGroups()` |
| `expiresIn` | `Duration.ofHours((long) prefs.get(APPROVAL_EXPIRES_HOURS).value())` |
| `scope` | `"casehubio/life/oversight"` (verify mapping against engine#437) |

### Fail-safe

If `LifeActionRiskClassifier` throws, the engine applies its own fail-safe: `GateRequired("Classifier error — manual review required before proceeding", reversible=true, null groups, null expiry, null scope)`. No additional error handling needed in the classifier.

---

## Tests

### `LifeActionRiskClassifierTest` (unit, no Quarkus)

Mocks `PreferenceProvider` returning YAML defaults. Covers:
- Each `ALWAYS` type → `GateRequired`; correct `reversible`, `candidateGroups`, non-null `expiresIn`, scope = `"casehubio/life/oversight"`
- `HEALTH_APPOINTMENT_GP` (NEVER) → `Autonomous`
- `SPEND_PURCHASE` at 99.99 → `Autonomous`; at 100.0 → `GateRequired`
- `CONTRACTOR_ENGAGE` at 199.99 → `Autonomous`; at 200.0 → `GateRequired`
- Missing `amount` key → `Autonomous`
- Unparseable amount (`"not-a-number"`) → `Autonomous`
- Unknown `actionType` (`"foo.bar"`) → `Autonomous`
- `HEALTH_MEDICATION_FLAG` → `candidateGroups` = `["household-admin", "household-member"]`
- `BOOKING_NONREFUNDABLE` → `reversible = false`
- `LEGAL_DOCUMENT_SUBMIT` → `reversible = false`
- `expiresIn` = `Duration.ofHours(24)`

### `LifeActionRiskClassifierQuarkusTest` (@QuarkusTest)

- Inject `ChainedReactiveActionRiskClassifier`; ALWAYS type → `GateRequired`
- `SPEND_PURCHASE` with amount 99.0 → `Autonomous` (confirms YAML loaded)
- `SPEND_PURCHASE` with amount 100.0 → `GateRequired` (confirms threshold)
- `@RiskClassifier Instance<ActionRiskClassifier>` is satisfied (not empty)

---

## Deferred

| Issue | Description |
|-------|-------------|
| engine#437 | Clarify `GateRequired.scope` semantics — verify `"casehubio/life/oversight"` is correct |
| engine#438 | Extract `OversightGateService` from casehub-openclaw to casehub-engine-api |
| life#26 | RBAC-differentiated thresholds — per-principal spend gates (blocked on auth retrofit) |

---

## Platform Coherence Notes

- `ActionRiskClassifier` SPI in `casehub-engine-api/api/spi/` per consumer-spi-placement protocol ✅
- No auth types in classifier — auth-retrofit-readiness ✅
- Trust routing (agent selection) and risk classification (action gating) are orthogonal concerns ✅
- YAML + PreferenceKey pattern matches Layer 6 trust routing exactly ✅
- CLAUDE.md Layer 7 additions section to be updated post-commit ✅
