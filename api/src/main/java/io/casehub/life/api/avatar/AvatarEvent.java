package io.casehub.life.api.avatar;

public sealed interface AvatarEvent permits
        AvatarEvent.TextDelta,
        AvatarEvent.ThinkingDelta,
        AvatarEvent.ToolCallStarted,
        AvatarEvent.ToolCallCompleted,
        AvatarEvent.ToolResult,
        AvatarEvent.ActionCard,
        AvatarEvent.ConfirmationRequired,
        AvatarEvent.SessionComplete {

    record TextDelta(String text) implements AvatarEvent {}

    record ThinkingDelta(String text) implements AvatarEvent {}

    record ToolCallStarted(String toolName) implements AvatarEvent {}

    record ToolCallCompleted(String toolName, String arguments) implements AvatarEvent {}

    record ToolResult(String toolName, String result, boolean isError) implements AvatarEvent {}

    record ActionCard(String type, String data) implements AvatarEvent {}

    record ConfirmationRequired(String actionId, String description, String consequences) implements AvatarEvent {}

    record SessionComplete(long inputTokens, long outputTokens) implements AvatarEvent {}
}
