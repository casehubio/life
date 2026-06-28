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

public enum LifeAgent {
    HEALTH("health-agent", "OpenClaw Health Agent",
            "casehubio/life/health", "Health domain coordination agent"),
    HOME("home-agent", "OpenClaw Home Agent",
            "casehubio/life/household", "Household maintenance agent"),
    FINANCE("finance-agent", "OpenClaw Finance Agent",
            "casehubio/life/finance", "Financial review and governance agent"),
    TRAVEL("travel-agent", "OpenClaw Travel Agent",
            "casehubio/life/travel", "Travel planning and booking agent");

    public static final String MODEL_FAMILY = "openclaw";
    public static final int MAJOR_VERSION = 1;

    private final String persona;
    private final String displayName;
    private final String slot;
    private final String briefing;

    LifeAgent(String persona, String displayName, String slot, String briefing) {
        this.persona = persona;
        this.displayName = displayName;
        this.slot = slot;
        this.briefing = briefing;
    }

    public String agentId() {
        return MODEL_FAMILY + ":" + persona + "@" + MAJOR_VERSION;
    }

    public String persona() {
        return persona;
    }

    public String displayName() {
        return displayName;
    }

    public String slot() {
        return slot;
    }

    public String briefing() {
        return briefing;
    }
}
