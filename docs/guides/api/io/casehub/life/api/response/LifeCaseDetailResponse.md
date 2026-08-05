# io.casehub.life.api.response.LifeCaseDetailResponse

**Package:** `io.casehub.life.api.response`

**Kind:** `record`

## Fields

### `caseId` (`java.util.UUID`)

### `caseType` (`io.casehub.life.api.LifeCaseType`)

### `completedAt` (`java.time.Instant`)

### `createdAt` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `engineCaseId` (`java.util.UUID`)

### `status` (`io.casehub.life.api.LifeCaseStatus`)

## Record Components

### `caseId` (`java.util.UUID`)

### `caseType` (`io.casehub.life.api.LifeCaseType`)

### `completedAt` (`java.time.Instant`)

### `createdAt` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `engineCaseId` (`java.util.UUID`)

### `status` (`io.casehub.life.api.LifeCaseStatus`)

## Constructors

### `public LifeCaseDetailResponse(java.util.UUID caseId, io.casehub.life.api.LifeCaseType caseType, io.casehub.life.api.LifeDomain domain, io.casehub.life.api.LifeCaseStatus status, java.time.Instant createdAt, java.time.Instant completedAt, java.util.UUID engineCaseId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `caseType` (`io.casehub.life.api.LifeCaseType`)
- `domain` (`io.casehub.life.api.LifeDomain`)
- `status` (`io.casehub.life.api.LifeCaseStatus`)
- `createdAt` (`java.time.Instant`)
- `completedAt` (`java.time.Instant`)
- `engineCaseId` (`java.util.UUID`)

## Methods

### `public java.util.UUID caseId()`

### `public io.casehub.life.api.LifeCaseType caseType()`

### `public java.time.Instant completedAt()`

### `public java.time.Instant createdAt()`

### `public io.casehub.life.api.LifeDomain domain()`

### `public java.util.UUID engineCaseId()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.life.api.LifeCaseStatus status()`

### `public final java.lang.String toString()`
