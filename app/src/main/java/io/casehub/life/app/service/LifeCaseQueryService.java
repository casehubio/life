package io.casehub.life.app.service;

import io.casehub.life.api.ActionType;
import io.casehub.life.api.HouseholdGroups;
import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.api.LifeCaseType;
import io.casehub.life.api.LifeDomain;
import io.casehub.life.api.Urgency;
import io.casehub.life.api.response.LifeCaseDetailResponse;
import io.casehub.life.api.response.LifeCaseResponse;
import io.casehub.life.api.response.LifeCommitmentResponse;
import io.casehub.life.api.response.PagedResponse;
import io.casehub.life.api.response.PendingActionResponse;
import io.casehub.life.api.spi.LifeCaseVisibilityPolicy;
import io.casehub.life.app.entity.LifeCaseTracker;
import io.casehub.life.app.entity.LifeCommitmentRecord;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.casehub.work.runtime.model.WorkItem;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class LifeCaseQueryService {
    private static final String LIFE_SCOPE_PREFIX = "casehubio/life/";


    @Inject CurrentPrincipal currentPrincipal;
    @Inject LifeCaseVisibilityPolicy visibilityPolicy;

    @Transactional
    public PagedResponse<LifeCaseResponse> listCases(LifeDomain domain,
                                                     LifeCaseStatus status,
                                                     LifeCaseType caseType,
                                                     int page, int size) {
        QueryParts query = buildListQuery(domain, status, caseType);
        List<LifeCaseTracker> allTrackers = LifeCaseTracker.find(query.hql(),
                                                                 Sort.by("createdAt", Sort.Direction.Descending), query.params())
                                                           .list();

        String      actorId = currentPrincipal.actorId();
        Set<String> groups  = currentPrincipal.groups();

        List<LifeCaseResponse> visible = allTrackers.stream()
                                                    .map(this::toResponse)
                                                    .filter(r -> visibilityPolicy.isVisible(r, actorId, groups))
                                                    .toList();

        long                   total     = visible.size();
        int                    fromIndex = Math.min(page * size, visible.size());
        int                    toIndex   = Math.min(fromIndex + size, visible.size());
        List<LifeCaseResponse> items     = visible.subList(fromIndex, toIndex);

        return new PagedResponse<>(items, page, size, total);
    }

    @Transactional
    public Optional<LifeCaseDetailResponse> findById(UUID id) {
        LifeCaseTracker tracker = LifeCaseTracker.findById(id);
        if (tracker == null) return Optional.empty();

        LifeCaseResponse response = toResponse(tracker);
        String actorId = currentPrincipal.actorId();
        Set<String> groups = currentPrincipal.groups();

        if (!visibilityPolicy.isVisible(response, actorId, groups)) {
            return Optional.empty();
        }

        return Optional.of(toDetailResponse(tracker));
    }

    @Transactional
    public Optional<List<PendingActionResponse>> findTasksByCase(UUID caseTrackerId) {
        LifeCaseTracker tracker = LifeCaseTracker.findById(caseTrackerId);
        if (tracker == null) {return Optional.empty();}
        if (tracker.engineCaseId == null) {return Optional.of(List.of());}

        String callerRefPrefix = "case:" + tracker.engineCaseId + "/";
        List<WorkItem> workItems = WorkItem.<WorkItem>list("callerRef LIKE ?1 ORDER BY createdAt ASC",
                                                           callerRefPrefix + "%");

        Set<String> groups  = currentPrincipal.groups();

        Instant now = Instant.now();
        List<PendingActionResponse> responses = workItems.stream()
                                                         .map(wi -> toPendingAction(wi, now))
                                                         .filter(r -> isTaskVisible(r, groups))
                                                         .toList();
        return Optional.of(responses);
    }

    private boolean isTaskVisible(PendingActionResponse task, Set<String> groups) {
        if (groups.contains(HouseholdGroups.ADMIN) || groups.contains(HouseholdGroups.MEMBER)) {
            return true;
        }
        if (task.candidateGroups() == null || task.candidateGroups().isEmpty()) {return false;}
        Set<String> taskGroups = Arrays.stream(task.candidateGroups().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        for (String g : groups) {
            if (taskGroups.contains(g)) {return true;}
        }
        return false;
    }


    @Transactional
    public Optional<List<LifeCommitmentResponse>> findCommitmentsByCase(UUID caseTrackerId) {
        LifeCaseTracker tracker = LifeCaseTracker.findById(caseTrackerId);
        if (tracker == null) {return Optional.empty();}
        if (tracker.engineCaseId == null) {return Optional.of(List.of());}

        String callerRefPrefix = "case:" + tracker.engineCaseId + "/";
        List<WorkItem> workItems = WorkItem.<WorkItem>list("callerRef LIKE ?1",
                                                           callerRefPrefix + "%");
        List<UUID> workItemIds = workItems.stream().map(wi -> wi.id).toList();
        if (workItemIds.isEmpty()) {return Optional.of(List.of());}

        List<LifeCommitmentRecord> records = LifeCommitmentRecord
                                                     .<LifeCommitmentRecord>list("workItemId IN ?1", workItemIds);
        return Optional.of(records.stream().map(this::toCommitmentResponse).toList());
    }

    private LifeCommitmentResponse toCommitmentResponse(LifeCommitmentRecord rec) {
        return new LifeCommitmentResponse(
                rec.id, rec.correlationId, rec.mode, rec.status,
                rec.domain, rec.delegateTo, rec.deadline,
                rec.createdAt, rec.amountThreshold, rec.purchaseCategory);
    }


    private LifeCaseResponse toResponse(LifeCaseTracker tracker) {
        return new LifeCaseResponse(
                tracker.id,
                LifeCaseType.valueOf(caseNameToEnumName(tracker.caseType)),
                tracker.domain,
                tracker.status,
                tracker.createdAt,
                tracker.completedAt
        );
    }

    private LifeCaseDetailResponse toDetailResponse(LifeCaseTracker tracker) {
        return new LifeCaseDetailResponse(
                tracker.id,
                LifeCaseType.valueOf(caseNameToEnumName(tracker.caseType)),
                tracker.domain,
                tracker.status,
                tracker.createdAt,
                tracker.completedAt,
                tracker.engineCaseId
        );
    }

    private String caseNameToEnumName(String caseName) {
        return caseName.toUpperCase().replace('-', '_');
    }

    private record QueryParts(String hql, Map<String, Object> params) {}

    private QueryParts buildListQuery(LifeDomain domain, LifeCaseStatus status,
                                       LifeCaseType caseType) {
        var conditions = new ArrayList<String>();
        var params = new HashMap<String, Object>();

        if (domain != null) {
            conditions.add("domain = :domain");
            params.put("domain", domain);
        }
        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status);
        }
        if (caseType != null) {
            conditions.add("caseType = :caseType");
            params.put("caseType", caseType.caseName());
        }

        String hql = conditions.isEmpty() ? "" : String.join(" and ", conditions);
        return new QueryParts(hql, params);
    }

    private PendingActionResponse toPendingAction(WorkItem wi, Instant now) {
        LifeDomain domain      = domainFromScope(wi.scope);
        Urgency    urgency     = Urgency.classify(wi.expiresAt, now, 24);
        Long       daysOverdue = Urgency.daysOverdue(wi.expiresAt, now);
        return new PendingActionResponse(
                wi.id, wi.title, wi.description,
                wi.status != null ? wi.status.name() : null,
                domain, wi.candidateGroups,
                wi.createdAt, wi.expiresAt, urgency, daysOverdue,
                PendingActionsService.ESCALATION_CALLER_REF.equals(wi.callerRef)
                        ? ActionType.WATCHDOG_ALERT : ActionType.WORK_ITEM);
    }

    private LifeDomain domainFromScope(String scope) {
        if (scope == null || !scope.startsWith(LIFE_SCOPE_PREFIX)) {return null;}
        String segment = scope.substring(LIFE_SCOPE_PREFIX.length());
        int    slash   = segment.indexOf('/');
        if (slash > 0) {segment = segment.substring(0, slash);}
        return LifeDomain.fromCategory(segment).orElse(null);
    }
}
