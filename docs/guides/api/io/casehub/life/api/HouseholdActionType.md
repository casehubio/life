# io.casehub.life.api.HouseholdActionType

**Package:** `io.casehub.life.api`

**Kind:** `enum`

Typed taxonomy of consequential household actions declared by workers before execution.
Workers use actionType() when constructing PlannedAction; fromActionType() reverses the mapping.
Each constant encodes its inherent domain properties — gatePolicy, reversible, candidateGroups,
reasonTemplate — so all logic for a type lives here. Threshold key resolution is handled in app/
routing via LifeRiskPolicyKeys, not in this enum.

## Fields

### `candidateGroups` (`CandidateSetStrategy`)

### `gatePolicy` (`io.casehub.life.api.HouseholdActionType.GatePolicy`)

### `reasonTemplate` (`java.lang.String`)

### `reversible` (`boolean`)

## Enum Constants

### `BOOKING_NONREFUNDABLE` (`io.casehub.life.api.HouseholdActionType`)

### `BOOKING_REFUNDABLE` (`io.casehub.life.api.HouseholdActionType`)

### `CONTRACTOR_ENGAGE` (`io.casehub.life.api.HouseholdActionType`)

### `ELDER_CARE_DECISION` (`io.casehub.life.api.HouseholdActionType`)

Care decision for a dependent — any adult can approve (urgency matters).

### `HEALTH_APPOINTMENT_GP` (`io.casehub.life.api.HouseholdActionType`)

Routine GP booking — no gate required.

### `HEALTH_APPOINTMENT_SPECIALIST` (`io.casehub.life.api.HouseholdActionType`)

### `HEALTH_MEDICATION_FLAG` (`io.casehub.life.api.HouseholdActionType`)

Medication interaction — irreversible safety concern; any adult can approve (speed matters).

### `LEGAL_DOCUMENT_SUBMIT` (`io.casehub.life.api.HouseholdActionType`)

### `SPEND_PURCHASE` (`io.casehub.life.api.HouseholdActionType`)

### `SPEND_SUBSCRIPTION_CANCEL` (`io.casehub.life.api.HouseholdActionType`)

### `SPEND_SUBSCRIPTION_MODIFY` (`io.casehub.life.api.HouseholdActionType`)

## Constructors

### `private HouseholdActionType(io.casehub.life.api.HouseholdActionType.GatePolicy gatePolicy, boolean reversible, CandidateSetStrategy candidateGroups, java.lang.String reasonTemplate)`

#### Parameters

- `gatePolicy` (`io.casehub.life.api.HouseholdActionType.GatePolicy`)
- `reversible` (`boolean`)
- `candidateGroups` (`CandidateSetStrategy`)
- `reasonTemplate` (`java.lang.String`)

## Methods

### `public java.lang.String actionType()`

The actionType string for PlannedAction.actionType(). e.g. SPEND_PURCHASE → "spend.purchase"

### `public CandidateSetStrategy candidateGroups()`

### `public static java.util.Optional<io.casehub.life.api.HouseholdActionType> fromActionType(java.lang.String actionType)`

Parse a PlannedAction.actionType() string back to enum. Empty if unknown.

#### Parameters

- `actionType` (`java.lang.String`)

### `public io.casehub.life.api.HouseholdActionType.GatePolicy gatePolicy()`

### `public java.lang.String reasonTemplate()`

Reason template for GateRequired.reason(). Nullable for NEVER-gated types (HEALTH_APPOINTMENT_GP).
May contain %s format specifiers for amount substitution. String.formatted() silently ignores
extra args, so templates without %s work fine when formatted with amount.

### `public boolean reversible()`

### `public static io.casehub.life.api.HouseholdActionType valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.life.api.HouseholdActionType[] values()`
