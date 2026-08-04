# io.casehub.life.api.response.LifeCaseResponse

**Package:** `io.casehub.life.api.response`

**Kind:** `record`

## Fields

### `caseId` (`java.util.UUID`)

### `caseType` (`io.casehub.life.api.LifeCaseType`)

### `completedAt` (`java.time.Instant`)

### `createdAt` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `status` (`io.casehub.life.api.LifeCaseStatus`)

## Record Components

### `caseId` (`java.util.UUID`)

### `caseType` (`io.casehub.life.api.LifeCaseType`)

### `completedAt` (`java.time.Instant`)

### `createdAt` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `status` (`io.casehub.life.api.LifeCaseStatus`)

## Constructors

### `public LifeCaseResponse(java.util.UUID caseId, io.casehub.life.api.LifeCaseType caseType, io.casehub.life.api.LifeDomain domain, io.casehub.life.api.LifeCaseStatus status, java.time.Instant createdAt, java.time.Instant completedAt)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `caseType` (`io.casehub.life.api.LifeCaseType`)
- `domain` (`io.casehub.life.api.LifeDomain`)
- `status` (`io.casehub.life.api.LifeCaseStatus`)
- `createdAt` (`java.time.Instant`)
- `completedAt` (`java.time.Instant`)

## Methods

### `public java.util.UUID caseId()`

### `public io.casehub.life.api.LifeCaseType caseType()`

### `public java.time.Instant completedAt()`

### `public java.time.Instant createdAt()`

### `public io.casehub.life.api.LifeDomain domain()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.life.api.LifeCaseStatus status()`

### `public final java.lang.String toString()`
