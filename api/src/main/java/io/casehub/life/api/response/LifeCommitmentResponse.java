package io.casehub.life.api.response;

import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.commitment.CommitmentMode;
import io.casehub.life.api.commitment.CommitmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LifeCommitmentResponse(
        UUID id,
        String correlationId,
        CommitmentMode mode,
        CommitmentStatus status,
        LifeDomain domain,
        String delegateTo,
        Instant deadline,
        Instant createdAt,
        BigDecimal amountThreshold,
        String purchaseCategory) {}
