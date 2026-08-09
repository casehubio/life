package io.casehub.life.api.response;

public record ChannelMessageResponse(
        String id,
        String channelId,
        String channelName,
        String sender,
        String messageType,
        String actorType,
        String content,
        String topic,
        String correlationId,
        String target,
        String commitmentId,
        String deadline,
        String acknowledgedAt,
        int replyCount,
        String createdAt
) {}
