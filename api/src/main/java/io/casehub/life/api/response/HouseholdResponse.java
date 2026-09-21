package io.casehub.life.api.response;

import java.time.Instant;
import java.util.UUID;

public record HouseholdResponse(UUID id, String name, String timezone, String jurisdiction, Instant createdAt) {}
