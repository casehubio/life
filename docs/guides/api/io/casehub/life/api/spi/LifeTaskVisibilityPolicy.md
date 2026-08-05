# io.casehub.life.api.spi.LifeTaskVisibilityPolicy

**Package:** `io.casehub.life.api.spi`

**Kind:** `interface`

Determines whether a given principal may see a particular life task.

<p>Default implementation is permissive (always visible). The
`JuniorLifeTaskVisibilityPolicy` alternative restricts
`household-junior` principals to tasks they are assigned to
or whose candidate groups overlap their own groups.

## Methods

### `public abstract boolean isVisible(io.casehub.life.api.response.LifeTaskResponse task, java.lang.String actorId, java.util.Set<java.lang.String> groups)`

#### Parameters

- `task` (`io.casehub.life.api.response.LifeTaskResponse`)
- `actorId` (`java.lang.String`)
- `groups` (`java.util.Set<java.lang.String>`)
