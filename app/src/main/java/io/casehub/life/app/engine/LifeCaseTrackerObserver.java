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

import io.casehub.engine.common.spi.event.CaseLifecycleEvent;
import io.casehub.life.api.LifeCaseStatus;
import io.casehub.life.app.entity.LifeCaseTracker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;

/**
 * CDI observer that updates LifeCaseTracker status when engine cases complete.
 *
 * <p>Observes {@link CaseLifecycleEvent} fired asynchronously by casehub-engine and updates
 * the corresponding {@link LifeCaseTracker} record when a case reaches COMPLETED status.
 *
 * <p>Uses {@link Transactional.TxType#REQUIRES_NEW} to ensure the status update is persisted
 * independently of the engine's transaction.
 */
@ApplicationScoped
public class LifeCaseTrackerObserver {

    private static final Logger LOG = Logger.getLogger(LifeCaseTrackerObserver.class);

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onCaseCompleted(@ObservesAsync CaseLifecycleEvent event) {
        if (!"CaseCompleted".equals(event.eventType())) return;

        LifeCaseTracker.findByEngineCaseId(event.caseId()).ifPresentOrElse(
                tracker -> {
                    tracker.status = LifeCaseStatus.COMPLETED;
                    tracker.completedAt = Instant.now();
                },
                () -> LOG.debugf("No LifeCaseTracker for caseId=%s — not a life case", event.caseId())
        );
    }
}
