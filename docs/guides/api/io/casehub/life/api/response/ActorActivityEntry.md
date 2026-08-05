# io.casehub.life.api.response.ActorActivityEntry

**Package:** `io.casehub.life.api.response`

**Kind:** `record`

## Fields

### `completedAt` (`java.time.Instant`)

### `createdAt` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `outcome` (`java.lang.String`)

### `scope` (`java.lang.String`)

### `status` (`java.lang.String`)

### `title` (`java.lang.String`)

### `workItemId` (`java.util.UUID`)

## Record Components

### `completedAt` (`java.time.Instant`)

### `createdAt` (`java.time.Instant`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `outcome` (`java.lang.String`)

### `scope` (`java.lang.String`)

### `status` (`java.lang.String`)

### `title` (`java.lang.String`)

### `workItemId` (`java.util.UUID`)

## Constructors

### `public ActorActivityEntry(java.util.UUID workItemId, java.lang.String title, io.casehub.life.api.LifeDomain domain, java.lang.String status, java.lang.String scope, java.time.Instant createdAt, java.time.Instant completedAt, java.lang.String outcome)`

#### Parameters

- `workItemId` (`java.util.UUID`)
- `title` (`java.lang.String`)
- `domain` (`io.casehub.life.api.LifeDomain`)
- `status` (`java.lang.String`)
- `scope` (`java.lang.String`)
- `createdAt` (`java.time.Instant`)
- `completedAt` (`java.time.Instant`)
- `outcome` (`java.lang.String`)

## Methods

### `public java.time.Instant completedAt()`

### `public java.time.Instant createdAt()`

### `public io.casehub.life.api.LifeDomain domain()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String outcome()`

### `public java.lang.String scope()`

### `public java.lang.String status()`

### `public java.lang.String title()`

### `public final java.lang.String toString()`

### `public java.util.UUID workItemId()`
