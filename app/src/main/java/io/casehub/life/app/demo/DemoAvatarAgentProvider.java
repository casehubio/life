package io.casehub.life.app.demo;

import io.casehub.platform.agent.AgentEvent;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSession;
import io.casehub.platform.agent.AgentSessionConfig;
import io.casehub.platform.agent.AgentSessionInit;
import io.quarkus.arc.profile.IfBuildProfile;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.time.Duration;

@ApplicationScoped
@Alternative
@Priority(300)
@IfBuildProfile("demo")
public class DemoAvatarAgentProvider implements AgentProvider {

    @Override
    public Multi<AgentEvent> invoke(AgentSessionConfig config) {
        return Multi.createFrom().items(
                new AgentEvent.TextDelta(generateResponse(config.userPrompt())));
    }

    @Override
    public AgentSession openSession(AgentSessionInit init) {
        return new DemoAvatarSession();
    }

    private static String generateResponse(String userMessage) {
        if (userMessage == null) return "Hello! How can I help you today?";
        String lower = userMessage.toLowerCase();

        if (lower.contains("task") || lower.contains("todo"))
            return "I can see your pending tasks. You have a few items that need attention — would you like me to create a new task or check what's overdue?";
        if (lower.contains("appointment") || lower.contains("doctor") || lower.contains("dentist"))
            return "I can help with appointment management. Would you like me to check the calendar for upcoming health appointments?";
        if (lower.contains("contractor") || lower.contains("bob") || lower.contains("quote"))
            return "I can look into contractor follow-ups. Shall I check the status of any outstanding quotes or commitments?";
        if (lower.contains("calendar") || lower.contains("schedule"))
            return "Let me check the calendar. I can see your upcoming events and any conflicts that need resolving.";
        if (lower.contains("help") || lower.contains("what can you do"))
            return "I can help you manage your household: create tasks, track appointments, follow up with contractors, manage finances, and coordinate family schedules. Just tell me what you need!";

        return "I understand you're asking about \"" + truncate(userMessage, 50) + "\". I can help with tasks, appointments, contractors, calendar, and household management. What would you like to do?";
    }

    private static String truncate(String text, int maxLen) {
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private static class DemoAvatarSession implements AgentSession {
        @Override
        public Multi<AgentEvent> query(String prompt) {
            String response = generateResponse(prompt);
            return Multi.createFrom().items(new AgentEvent.TextDelta(response));
        }

        @Override
        public void close(Duration drain) {}

        @Override
        public Uni<Void> interrupt() {
            return Uni.createFrom().voidItem();
        }
    }
}
