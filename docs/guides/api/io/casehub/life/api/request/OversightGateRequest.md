# io.casehub.life.api.request.OversightGateRequest

**Package:** `io.casehub.life.api.request`

**Kind:** `record`

## Fields

### `amountThreshold` (`java.math.BigDecimal`)

### `deadline` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `pendingTask` (`io.casehub.life.api.request.CreateLifeTaskRequest`)

### `purchaseCategory` (`java.lang.String`)

## Record Components

### `amountThreshold` (`java.math.BigDecimal`)

### `deadline` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `pendingTask` (`io.casehub.life.api.request.CreateLifeTaskRequest`)

### `purchaseCategory` (`java.lang.String`)

## Constructors

### `public OversightGateRequest(io.casehub.life.api.LifeDomain domain, java.time.Instant deadline, io.casehub.life.api.request.CreateLifeTaskRequest pendingTask, java.math.BigDecimal amountThreshold, java.lang.String purchaseCategory)`

#### Parameters

- `domain` (`io.casehub.life.api.LifeDomain`)
- `deadline` (`java.time.Instant`)
- `pendingTask` (`io.casehub.life.api.request.CreateLifeTaskRequest`)
- `amountThreshold` (`java.math.BigDecimal`)
- `purchaseCategory` (`java.lang.String`)

## Methods

### `public java.math.BigDecimal amountThreshold()`

### `public java.time.Instant deadline()`

### `public io.casehub.life.api.LifeDomain domain()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.life.api.request.CreateLifeTaskRequest pendingTask()`

### `public java.lang.String purchaseCategory()`

### `public final java.lang.String toString()`
