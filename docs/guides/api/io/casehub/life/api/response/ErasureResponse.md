# io.casehub.life.api.response.ErasureResponse

**Package:** `io.casehub.life.api.response`

**Kind:** `record`

## Fields

### `erasedActorId` (`java.util.UUID`)

### `erasedAt` (`java.time.Instant`)

### `ledgerEntriesAffected` (`long`)

### `memoryRecordsErased` (`int`)

### `tokenisationEnabled` (`boolean`)

## Record Components

### `erasedActorId` (`java.util.UUID`)

### `erasedAt` (`java.time.Instant`)

### `ledgerEntriesAffected` (`long`)

### `memoryRecordsErased` (`int`)

### `tokenisationEnabled` (`boolean`)

## Constructors

### `public ErasureResponse(java.util.UUID erasedActorId, java.time.Instant erasedAt, int memoryRecordsErased, long ledgerEntriesAffected, boolean tokenisationEnabled)`

#### Parameters

- `erasedActorId` (`java.util.UUID`)
- `erasedAt` (`java.time.Instant`)
- `memoryRecordsErased` (`int`)
- `ledgerEntriesAffected` (`long`)
- `tokenisationEnabled` (`boolean`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.UUID erasedActorId()`

### `public java.time.Instant erasedAt()`

### `public final int hashCode()`

### `public long ledgerEntriesAffected()`

### `public int memoryRecordsErased()`

### `public final java.lang.String toString()`

### `public boolean tokenisationEnabled()`
