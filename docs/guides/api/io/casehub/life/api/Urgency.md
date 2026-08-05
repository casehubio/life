# io.casehub.life.api.Urgency

**Package:** `io.casehub.life.api`

**Kind:** `enum`

## Enum Constants

### `DUE_SOON` (`io.casehub.life.api.Urgency`)

### `NORMAL` (`io.casehub.life.api.Urgency`)

### `NO_DEADLINE` (`io.casehub.life.api.Urgency`)

### `OVERDUE` (`io.casehub.life.api.Urgency`)

## Constructors

### `private Urgency()`

## Methods

### `public static io.casehub.life.api.Urgency classify(java.time.Instant expiresAt, java.time.Instant now, int dueSoonHours)`

#### Parameters

- `expiresAt` (`java.time.Instant`)
- `now` (`java.time.Instant`)
- `dueSoonHours` (`int`)

### `public static java.lang.Long daysOverdue(java.time.Instant expiresAt, java.time.Instant now)`

#### Parameters

- `expiresAt` (`java.time.Instant`)
- `now` (`java.time.Instant`)

### `public static io.casehub.life.api.Urgency valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.life.api.Urgency[] values()`
