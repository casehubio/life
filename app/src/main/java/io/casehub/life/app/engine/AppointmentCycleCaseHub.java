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

import io.casehub.api.model.AgentWorkerFunction;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ai.WorkerAgentInvoker;
import io.casehub.life.api.LifeCaseType;
import io.casehub.life.app.engine.agent.BookingResult;
import io.casehub.life.app.engine.agent.ConfirmAppointmentResult;
import io.casehub.life.app.engine.agent.FindAlternativeResult;
import io.casehub.life.app.engine.agent.PreVisitPrepResult;
import io.casehub.life.app.engine.agent.RecordHealthDecisionResult;
import io.casehub.worker.api.Worker;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Appointment cycle case hub — loads the YAML definition and augments it with workers.
 *
 * <p>book-appointment-agent is the first real LLM-backed worker (casehubio/life#25):
 * it uses AgentWorkerFunction with the platform AgentProvider as the
 * LLM backend. Agent identity follows the {model-family}:{persona}@{major} convention
 * from docs/specs/life-actor-model.md. Refs engine#463 (settled function-as-worker design).
 *
 * <p>The humanTask binding (attend-and-record) is defined in YAML and handled by
 * {@link io.casehub.workadapter.HumanTaskScheduleHandler} — no Java worker needed.
 */
@ApplicationScoped
public class AppointmentCycleCaseHub extends LifeTypedCaseHub {

    public AppointmentCycleCaseHub() {
        super("life/appointment-cycle.yaml", LifeAgent.HEALTH);
    }

    @Override
    public LifeCaseType lifeCaseType() {
        return LifeCaseType.APPOINTMENT_CYCLE;
    }

    @Override
    protected void configureCase(CaseDefinition definition) {
        definition.getWorkers().add(bookAppointmentWorker());
        definition.getWorkers().add(agentWorker("find-alternative", """
                                                                    You are a healthcare appointment agent. Find an alternative provider
                                                                    after a booking was declined. Search available providers and propose
                                                                    an alternative appointment.
                                                                    Use search_contacts to find providers by name or specialty.
                                                                    Use list_contacts to browse available providers if search returns no results.
                                                                    Use calendar_list_events to check availability for alternative dates.
                                                                    Use calendar_create_event to book the alternative appointment.
                                                                    Include the contact ID and calendar event ID in your response.""", FindAlternativeResult.class));
        definition.getWorkers().add(agentWorker("confirm-appointment", """
                                                                       You are a healthcare appointment agent. Send appointment confirmation
                                                                       to the patient and schedule a reminder for 24 hours before.
                                                                       Use calendar_create_event to set the reminder event.
                                                                       Use send_chat to send the confirmation message to the patient.
                                                                       Include both the calendar event ID and notification message ID.""", ConfirmAppointmentResult.class));
        definition.getWorkers().add(agentWorker("pre-visit-prep", """
                                                                  You are a healthcare appointment agent. Send pre-visit preparation
                                                                  checklist and instructions to the patient.
                                                                  Use send_email to email the preparation documents and checklist.
                                                                  Use send_chat to send a quick reminder with the appointment details.
                                                                  Include the email message ID and notification message ID in your response.""", PreVisitPrepResult.class));
        definition.getWorkers().add(agentWorker("record-health-decision", """
                                                                          You are a healthcare records agent. Record health decision outcomes
                                                                          to the tamper-evident ledger.""", RecordHealthDecisionResult.class));}

    /**
     * Books an appointment via the platform agent provider.
     *
     * <p>First real LLM-backed worker in casehub-life. Uses AgentWorkerFunction
     * per the engine#463 settled abstraction. The agent returns structured JSON conforming
     * to BookingResult — confirmed=false indicates a PENDING booking; the confirm-appointment
     * binding fires when .booking != null and .booking.declined != true.
     *
     * <p>AgentDescriptor.agentId "life:health-agent@1" follows the
     * {model-family}:{persona}@{major} convention. This value MUST match the
     * platform agent config map key.
     */
    private Worker bookAppointmentWorker() {
        final WorkerAgentInvoker bookingAgent = agentProviderFactory.invokerFor(agent(),
                "You are a healthcare appointment booking agent for a UK household. " +
                "Book medical appointments with the requested provider. " +
                "Use search_contacts to look up the provider's contact details. " +
                "Use calendar_create_event to create the appointment in the calendar. " +
                "Include the contact ID and calendar event ID in your response. " +
                "If the provider is unavailable, set declined=true and provide a reason. " +
                "If cbrCalibration is provided, use historicalSuccessRate to inform " +
                "booking confidence and featureStats for appointment patterns. " +
                "Respond with valid JSON only — no prose, no explanation. " + CBR_SYSTEM_PROMPT_SUFFIX,
                "Book a {{appointmentType}} appointment with provider {{provider}}.", BookingResult.class,
                cbrInputTransformer);

        return Worker.builder()
                     .name("book-appointment-agent")
                     .capabilityName("book-appointment")
                     .function(new AgentWorkerFunction(bookingAgent))
                     .build();}
}
