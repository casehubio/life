package io.casehub.life.api.response;

import io.casehub.life.api.LifeDomain;

public record BriefingItem(
        String text,
        LifeDomain domain,
        String type
) {}
