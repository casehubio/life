# io.casehub.life.api.response.PendingActionResponse

**Package:** `io.casehub.life.api.response`

**Kind:** `record`

## Fields

### `candidateGroups` (`java.lang.String`)

### `createdAt` (`java.time.Instant`)

### `daysOverdue` (`java.lang.Long`)

### `description` (`java.lang.String`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `expiresAt` (`java.time.Instant`)

### `status` (`java.lang.String`)

### `title` (`java.lang.String`)

### `urgency` (`io.casehub.life.api.Urgency`)

### `workItemId` (`java.util.UUID`)

## Record Components

### `candidateGroups` (`java.lang.String`)

### `createdAt` (`java.time.Instant`)

### `daysOverdue` (`java.lang.Long`)

### `description` (`java.lang.String`)

### `domain` (`io.casehub.life.api.LifeDomain`)

### `expiresAt` (`java.time.Instant`)

### `status` (`java.lang.String`)

### `title` (`java.lang.String`)

### `urgency` (`io.casehub.life.api.Urgency`)

### `workItemId` (`java.util.UUID`)

## Constructors

### `public PendingActionResponse(java.util.UUID workItemId, java.lang.String title, java.lang.String description, java.lang.String status, io.casehub.life.api.LifeDomain domain, java.lang.String candidateGroups, java.time.Instant createdAt, java.time.Instant expiresAt, io.casehub.life.api.Urgency urgency, java.lang.Long daysOverdue)`

#### Parameters

- `workItemId` (`java.util.UUID`)
- `title` (`java.lang.String`)
- `description` (`java.lang.String`)
- `status` (`java.lang.String`)
- `domain` (`io.casehub.life.api.LifeDomain`)
- `candidateGroups` (`java.lang.String`)
- `createdAt` (`java.time.Instant`)
- `expiresAt` (`java.time.Instant`)
- `urgency` (`io.casehub.life.api.Urgency`)
- `daysOverdue` (`java.lang.Long`)

## Methods

### `public java.lang.String candidateGroups()`

### `public java.time.Instant createdAt()`

### `public java.lang.Long daysOverdue()`

### `public java.lang.String description()`

### `public io.casehub.life.api.LifeDomain domain()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.time.Instant expiresAt()`

### `public final int hashCode()`

### `public java.lang.String status()`

### `public java.lang.String title()`

### `public final java.lang.String toString()`

### `public io.casehub.life.api.Urgency urgency()`

### `public java.util.UUID workItemId()`
