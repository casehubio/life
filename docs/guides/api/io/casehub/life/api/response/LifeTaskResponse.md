# io.casehub.life.api.response.LifeTaskResponse

**Package:** `io.casehub.life.api.response`

**Kind:** `record`

## Fields

### `assigneeId` (`java.lang.String`)

### `candidateGroups` (`java.util.List<java.lang.String>`)

### `commitmentMode` (`io.casehub.life.api.commitment.CommitmentMode`)

### `commitmentStatus` (`io.casehub.life.api.commitment.CommitmentStatus`)

### `createdAt` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `externalActorId` (`java.util.UUID`)

### `status` (`java.lang.String`)

### `templateRef` (`java.lang.String`)

### `workItemId` (`java.util.UUID`)

## Record Components

### `assigneeId` (`java.lang.String`)

### `candidateGroups` (`java.util.List<java.lang.String>`)

### `commitmentMode` (`io.casehub.life.api.commitment.CommitmentMode`)

### `commitmentStatus` (`io.casehub.life.api.commitment.CommitmentStatus`)

### `createdAt` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `externalActorId` (`java.util.UUID`)

### `status` (`java.lang.String`)

### `templateRef` (`java.lang.String`)

### `workItemId` (`java.util.UUID`)

## Constructors

### `public LifeTaskResponse(java.util.UUID workItemId, java.lang.String templateRef, io.casehub.life.api.LifeDomain domain, java.lang.String status, java.util.UUID externalActorId, java.time.Instant createdAt, io.casehub.life.api.commitment.CommitmentMode commitmentMode, io.casehub.life.api.commitment.CommitmentStatus commitmentStatus, java.lang.String assigneeId, java.util.List<java.lang.String> candidateGroups)`

#### Parameters

- `workItemId` (`java.util.UUID`)
- `templateRef` (`java.lang.String`)
- `domain` (`io.casehub.life.api.LifeDomain`)
- `status` (`java.lang.String`)
- `externalActorId` (`java.util.UUID`)
- `createdAt` (`java.time.Instant`)
- `commitmentMode` (`io.casehub.life.api.commitment.CommitmentMode`)
- `commitmentStatus` (`io.casehub.life.api.commitment.CommitmentStatus`)
- `assigneeId` (`java.lang.String`)
- `candidateGroups` (`java.util.List<java.lang.String>`)

## Methods

### `public java.lang.String assigneeId()`

### `public java.util.List<java.lang.String> candidateGroups()`

### `public io.casehub.life.api.commitment.CommitmentMode commitmentMode()`

### `public io.casehub.life.api.commitment.CommitmentStatus commitmentStatus()`

### `public java.time.Instant createdAt()`

### `public io.casehub.life.api.LifeDomain domain()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.UUID externalActorId()`

### `public final int hashCode()`

### `public java.lang.String status()`

### `public java.lang.String templateRef()`

### `public final java.lang.String toString()`

### `public java.util.UUID workItemId()`
