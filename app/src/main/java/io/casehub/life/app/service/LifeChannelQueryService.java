package io.casehub.life.app.service;

import io.casehub.life.api.response.ChannelMessageResponse;
import io.casehub.life.app.entity.LifeCaseTracker;
import io.casehub.life.app.entity.LifeTaskContext;
import io.casehub.life.app.infrastructure.LifeChannelInitializer;
import io.casehub.qhorus.api.channel.Channel;
import io.casehub.qhorus.api.message.Message;
import io.casehub.qhorus.api.store.MessageStore;
import io.casehub.qhorus.api.store.query.MessageQuery;
import io.casehub.qhorus.runtime.channel.ChannelService;
import io.casehub.work.runtime.model.WorkItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class LifeChannelQueryService {

    private static final int MESSAGE_LIMIT = 50;

    @Inject ChannelService channelService;
    @Inject MessageStore messageStore;

    @Transactional
    public Optional<List<ChannelMessageResponse>> findChannelMessagesByCase(UUID caseTrackerId) {
        LifeCaseTracker tracker = LifeCaseTracker.findById(caseTrackerId);
        if (tracker == null) return Optional.empty();
        if (tracker.engineCaseId == null) return Optional.of(List.of());

        List<ChannelMessageResponse> all = new ArrayList<>();

        queryChannel(LifeChannelInitializer.DELEGATION_CHANNEL).ifPresent(all::addAll);
        queryChannel(LifeChannelInitializer.OVERSIGHT_CHANNEL).ifPresent(all::addAll);

        for (String actorChannel : resolveActorChannels(tracker.engineCaseId)) {
            queryChannel(actorChannel).ifPresent(all::addAll);
        }

        all.sort((a, b) -> {
            if (a.createdAt() == null) return 1;
            if (b.createdAt() == null) return -1;
            return a.createdAt().compareTo(b.createdAt());
        });

        return Optional.of(all);
    }

    private Optional<List<ChannelMessageResponse>> queryChannel(String channelName) {
        return channelService.findByName(channelName)
                .map(channel -> {
                    List<Message> messages = messageStore.scan(
                            MessageQuery.builder()
                                    .channelId(channel.id())
                                    .limit(MESSAGE_LIMIT)
                                    .descending(true)
                                    .build());
                    List<Message> chronological = new ArrayList<>(messages);
                    Collections.reverse(chronological);
                    return chronological.stream()
                            .map(m -> toResponse(m, channelName))
                            .toList();
                });
    }

    private ChannelMessageResponse toResponse(Message msg, String channelName) {
        return new ChannelMessageResponse(
                msg.id() != null ? msg.id().toString() : null,
                msg.channelId() != null ? msg.channelId().toString() : null,
                channelName,
                msg.sender(),
                msg.messageType() != null ? msg.messageType().name() : null,
                msg.actorType() != null ? msg.actorType().name() : null,
                msg.content(),
                msg.topic(),
                msg.correlationId(),
                msg.target(),
                msg.commitmentId() != null ? msg.commitmentId().toString() : null,
                msg.deadline() != null ? msg.deadline().toString() : null,
                msg.acknowledgedAt() != null ? msg.acknowledgedAt().toString() : null,
                msg.replyCount(),
                msg.createdAt() != null ? msg.createdAt().toString() : null
        );
    }

    private Set<String> resolveActorChannels(UUID engineCaseId) {
        String callerRefPrefix = "case:" + engineCaseId + "/";
        List<WorkItem> workItems = WorkItem.list("callerRef LIKE ?1", callerRefPrefix + "%");

        Set<String> channels = new LinkedHashSet<>();
        for (WorkItem wi : workItems) {
            LifeTaskContext.findByIdOptional(wi.id)
                    .map(obj -> (LifeTaskContext) obj)
                    .filter(ctx -> ctx.externalActorId != null)
                    .ifPresent(ctx -> channels.add("life/actor/ext-" + ctx.externalActorId));
        }
        return channels;
    }
}
