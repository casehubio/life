# io.casehub.life.api.LifeRoutingPolicy

**Package:** `io.casehub.life.api`

**Kind:** `record`

Domain-specific routing policy configuration record.
Holds threshold, observation requirements, margin, fallback strategy, and rationale.
Mapped from static configuration to engine's TrustRoutingPolicy via preference overlay.

## Fields

### `borderlineMargin` (`java.util.OptionalDouble`)

### `fallbackType` (`java.util.Optional<java.lang.String>`)

### `minimumObservations` (`java.util.OptionalInt`)

### `rationale` (`java.lang.String`)

### `threshold` (`java.util.OptionalDouble`)

## Record Components

### `borderlineMargin` (`java.util.OptionalDouble`)

### `fallbackType` (`java.util.Optional<java.lang.String>`)

### `minimumObservations` (`java.util.OptionalInt`)

### `rationale` (`java.lang.String`)

### `threshold` (`java.util.OptionalDouble`)

## Constructors

### `public LifeRoutingPolicy(java.util.OptionalDouble threshold, java.util.OptionalInt minimumObservations, java.util.OptionalDouble borderlineMargin, java.util.Optional<java.lang.String> fallbackType, java.lang.String rationale)`

#### Parameters

- `threshold` (`java.util.OptionalDouble`)
- `minimumObservations` (`java.util.OptionalInt`)
- `borderlineMargin` (`java.util.OptionalDouble`)
- `fallbackType` (`java.util.Optional<java.lang.String>`)
- `rationale` (`java.lang.String`)

## Methods

### `public java.util.OptionalDouble borderlineMargin()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.Optional<java.lang.String> fallbackType()`

### `public final int hashCode()`

### `public java.util.OptionalInt minimumObservations()`

### `public java.lang.String rationale()`

### `public java.util.OptionalDouble threshold()`

### `public final java.lang.String toString()`
