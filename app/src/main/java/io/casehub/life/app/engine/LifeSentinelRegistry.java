/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.life.app.engine;

import jakarta.enterprise.context.ApplicationScoped;
import org.quartz.JobKey;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class LifeSentinelRegistry {

    public record SentinelEntry(LifeAgent agent, UUID caseId,
                                String capabilityName, JobKey heartbeatJobKey) {}

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SentinelEntry>> byCaseId =
            new ConcurrentHashMap<>();

    public boolean isProvisioned(UUID caseId, String capabilityName) {
        var entries = byCaseId.get(caseId);
        if (entries == null) return false;
        return entries.stream().anyMatch(e -> e.capabilityName().equals(capabilityName));
    }

    public void register(SentinelEntry entry) {
        byCaseId.computeIfAbsent(entry.caseId(), k -> new CopyOnWriteArrayList<>()).add(entry);
    }

    public List<SentinelEntry> findByCaseId(UUID caseId) {
        var entries = byCaseId.get(caseId);
        return entries != null ? List.copyOf(entries) : List.of();
    }

    public void removeByCaseId(UUID caseId) {
        byCaseId.remove(caseId);
    }
}
