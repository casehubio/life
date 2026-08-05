# io.casehub.life.api.response.ExternalActorResponse

**Package:** `io.casehub.life.api.response`

**Kind:** `record`

## Fields

### `actorType` (`io.casehub.life.api.LifeActorType`)

### `contactMethod` (`java.lang.String`)

### `contactValue` (`java.lang.String`)

### `createdAt` (`java.time.Instant`)

### `gdprErasedAt` (`java.time.Instant`)

### `id` (`java.util.UUID`)

### `name` (`java.lang.String`)

### `trustProfile` (`io.casehub.life.api.response.ExternalActorResponse.TrustProfile`)

## Record Components

### `actorType` (`io.casehub.life.api.LifeActorType`)

### `contactMethod` (`java.lang.String`)

### `contactValue` (`java.lang.String`)

### `createdAt` (`java.time.Instant`)

### `gdprErasedAt` (`java.time.Instant`)

### `id` (`java.util.UUID`)

### `name` (`java.lang.String`)

### `trustProfile` (`io.casehub.life.api.response.ExternalActorResponse.TrustProfile`)

## Constructors

### `public ExternalActorResponse(java.util.UUID id, java.lang.String name, io.casehub.life.api.LifeActorType actorType, java.lang.String contactMethod, java.lang.String contactValue, java.time.Instant createdAt, java.time.Instant gdprErasedAt, io.casehub.life.api.response.ExternalActorResponse.TrustProfile trustProfile)`

#### Parameters

- `id` (`java.util.UUID`)
- `name` (`java.lang.String`)
- `actorType` (`io.casehub.life.api.LifeActorType`)
- `contactMethod` (`java.lang.String`)
- `contactValue` (`java.lang.String`)
- `createdAt` (`java.time.Instant`)
- `gdprErasedAt` (`java.time.Instant`)
- `trustProfile` (`io.casehub.life.api.response.ExternalActorResponse.TrustProfile`)

## Methods

### `public io.casehub.life.api.LifeActorType actorType()`

### `public java.lang.String contactMethod()`

### `public java.lang.String contactValue()`

### `public java.time.Instant createdAt()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.time.Instant gdprErasedAt()`

### `public final int hashCode()`

### `public java.util.UUID id()`

### `public java.lang.String name()`

### `public final java.lang.String toString()`

### `public io.casehub.life.api.response.ExternalActorResponse.TrustProfile trustProfile()`
