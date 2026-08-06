package io.casehub.life.api.response;

import java.util.List;

public record BriefingResponse(
        String greeting,
        int actionCount,
        List<BriefingItem> items
) {}
