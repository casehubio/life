# io.casehub.life.api.request.CommitmentRequest

**Package:** `io.casehub.life.api.request`

**Kind:** `record`

Request for POST /life-tasks/{id}/commit.
Exactly one of delegateTo or externalActorId must be non-null, and deadline must be non-null.
Validated in LifeCommitmentService before strategy dispatch.

## Fields

### `deadline` (`java.time.Instant`)

### `delegateTo` (`java.lang.String`)

### `externalActorId` (`java.util.UUID`)

## Record Components

### `deadline` (`java.time.Instant`)

### `delegateTo` (`java.lang.String`)

### `externalActorId` (`java.util.UUID`)

## Constructors

### `public CommitmentRequest(java.lang.String delegateTo, java.util.UUID externalActorId, java.time.Instant deadline)`

#### Parameters

- `delegateTo` (`java.lang.String`)
- `externalActorId` (`java.util.UUID`)
- `deadline` (`java.time.Instant`)

## Methods

### `public java.time.Instant deadline()`

### `public java.lang.String delegateTo()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.UUID externalActorId()`

### `public final int hashCode()`

### `public final java.lang.String toString()`
