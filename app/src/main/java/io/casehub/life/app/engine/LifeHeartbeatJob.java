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

import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.model.ai.Agent;
import io.casehub.life.app.engine.agent.AnomalySentinelReport;
import io.casehub.life.app.engine.agent.BookingSentinelReport;
import io.casehub.life.app.engine.agent.CareQualitySentinelReport;
import io.casehub.life.app.engine.agent.ContractorSentinelReport;
import io.casehub.life.app.engine.agent.FollowUpSentinelReport;
import io.casehub.life.app.engine.agent.LifeOpenClawChatModelFactory;
import io.casehub.life.app.engine.agent.MaintenanceSentinelReport;
import io.casehub.life.app.engine.agent.PatientStatusSentinelReport;
import io.casehub.worker.api.WorkerResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class LifeHeartbeatJob implements Job {

    private static final Logger LOG = Logger.getLogger(LifeHeartbeatJob.class);

    @Inject
    LifeOpenClawChatModelFactory openClawFactory;

    @Inject
    CaseHubRuntime caseHubRuntime;

    @Inject
    LifeChannelContextProvider channelContextProvider;

    @Override
    @SuppressWarnings("unchecked")
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        JobDataMap data = ctx.getMergedJobDataMap();
        LifeAgent agent = LifeAgent.valueOf(data.getString("agent"));
        UUID caseId = UUID.fromString(data.getString("caseId"));
        String capabilityName = data.getString("capabilityName");

        Map<String, Object> caseContext = (Map<String, Object>)
                caseHubRuntime.query(caseId, ".");

        Map<String, Object> enrichedContext = new HashMap<>(caseContext);
        try {
            enrichedContext.putAll(channelContextProvider.gatherContext(caseId));
        } catch (Exception e) {
            LOG.warnf(e, "Channel context gathering failed for case %s — proceeding with case context only", caseId);
        }

        Agent sentinelAgent = Agent.builder()
                .model(openClawFactory.forAgent(agent))
                .systemPrompt(sentinelSystemPrompt(capabilityName))
                .responseSchema(sentinelResponseSchema(capabilityName))
                .build();

        WorkerResult result = sentinelAgent.execute(enrichedContext);

        caseHubRuntime.signal(caseId, "sentinelReport", result.output());
    }

    static Class<?> sentinelResponseSchema(String capabilityName) {
        return switch (capabilityName) {
            case "contractor-sentinel" -> ContractorSentinelReport.class;
            case "maintenance-sentinel" -> MaintenanceSentinelReport.class;
            case "follow-up-sentinel" -> FollowUpSentinelReport.class;
            case "care-quality-sentinel" -> CareQualitySentinelReport.class;
            case "patient-status-sentinel" -> PatientStatusSentinelReport.class;
            case "anomaly-sentinel" -> AnomalySentinelReport.class;
            case "booking-sentinel" -> BookingSentinelReport.class;
            default -> throw new IllegalArgumentException("Unknown sentinel: " + capabilityName);
        };
    }

    static String sentinelSystemPrompt(String capabilityName) {
        return switch (capabilityName) {
            case "contractor-sentinel" -> """
                                          You are a contractor progress monitoring agent for a UK household.
                                          Check on the status of the active contractor job for this case.
                                          Use send_chat to contact the contractor for a status update if no recent
                                          communication exists. Report current progress, status (on-track/delayed/stalled),
                                          any concerns, and recommended actions. Include any notification message ID
                                          in your response.""";
            case "maintenance-sentinel" -> """
                                           You are a home maintenance progress monitoring agent for a UK household.
                                           Check on the status of the active maintenance job for this case.
                                           Use iot_get_state to read current property sensor data.
                                           Use send_chat to notify the household if sensors show anomalies.
                                           Report current progress, status (on-track/delayed/stalled),
                                           any concerns, and recommended actions. Include sensor readings and
                                           any notification message ID in your response.""";
            case "follow-up-sentinel" -> """
                                         You are a health appointment follow-up agent for a UK household.
                                         Check whether post-appointment actions have been completed:
                                         prescriptions collected, referrals booked, test results received.
                                         Use calendar_list_events to check for upcoming follow-up appointments.
                                         Use send_chat to send reminders for overdue actions.
                                         Report pending actions, days overdue, and whether escalation is needed.
                                         Include any calendar event ID and notification message ID.""";
            case "care-quality-sentinel" -> """
                                            You are a care quality monitoring agent for a UK household.
                                            Check whether scheduled care sessions have been delivered.
                                            Use calendar_list_events to verify scheduled vs completed sessions.
                                            Use send_chat to notify the family of any missed sessions.
                                            Report sessions scheduled vs completed, any missed sessions,
                                            concerns, and whether escalation is needed.
                                            Include any calendar event ID and notification message ID.""";
            case "patient-status-sentinel" -> """
                                              You are a patient status monitoring agent for a UK household.
                                              Assess the patient's current condition between care episodes.
                                              Use iot_get_state to read patient monitoring sensors (movement,
                                              temperature, medical devices).
                                              Use send_chat to alert carers if condition changes are detected.
                                              Report condition summary, trend (improving/stable/declining),
                                              any alerts, and whether escalation is needed.
                                              Include sensor readings and any notification message ID.""";
            case "anomaly-sentinel" -> """
                                       You are a financial anomaly detection agent for a UK household.
                                       Scan recent transactions for unusual patterns, budget overruns,
                                       or suspicious activity.
                                       Use bank_get_transactions to pull recent transaction data.
                                       Use send_chat to alert the household if anomalies are detected.
                                       Report anomalies found, severity, and whether escalation is needed.
                                       Include the transaction summary and any alert message ID.""";
            case "booking-sentinel" -> """
                                       You are a travel booking monitoring agent for a UK household.
                                       Check booking confirmations, price changes, and availability.
                                       Use calendar_list_events to verify travel dates are still in the calendar.
                                       Use send_chat to send reminders for upcoming travel.
                                       Report booking status, any price changes, alerts, and whether
                                       escalation is needed.
                                       Include any calendar event ID and reminder message ID.""";
            default -> throw new IllegalArgumentException("Unknown sentinel: " + capabilityName);
        };}
}
