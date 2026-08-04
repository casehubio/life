# io.casehub.life.api.FeatureStatistics

**Package:** `io.casehub.life.api`

**Kind:** `record`

## Fields

### `max` (`double`)

### `median` (`double`)

### `min` (`double`)

### `p75` (`double`)

### `sampleCount` (`int`)

## Record Components

### `max` (`double`)

### `median` (`double`)

### `min` (`double`)

### `p75` (`double`)

### `sampleCount` (`int`)

## Constructors

### `public FeatureStatistics(double min, double max, double median, double p75, int sampleCount)`

#### Parameters

- `min` (`double`)
- `max` (`double`)
- `median` (`double`)
- `p75` (`double`)
- `sampleCount` (`int`)

## Methods

### `public static io.casehub.life.api.FeatureStatistics compute(double[] values)`

#### Parameters

- `values` (`double[]`)

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public double max()`

### `public double median()`

### `public double min()`

### `private static double nearestRank(double[] sorted, double rank)`

#### Parameters

- `sorted` (`double[]`)
- `rank` (`double`)

### `public double p75()`

### `public int sampleCount()`

### `public final java.lang.String toString()`
