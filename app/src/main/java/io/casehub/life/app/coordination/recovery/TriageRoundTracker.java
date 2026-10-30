package io.casehub.life.app.coordination.recovery;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class TriageRoundTracker {

    static final int MAX_TRIAGE_ROUNDS = 2;

    private final ConcurrentHashMap<String, AtomicInteger> rounds = new ConcurrentHashMap<>();

    public int roundCount(UUID caseId, String workerName) {
        AtomicInteger count = rounds.get(key(caseId, workerName));
        return count != null ? count.get() : 0;
    }

    public void increment(UUID caseId, String workerName) {
        rounds.computeIfAbsent(key(caseId, workerName), k -> new AtomicInteger()).incrementAndGet();
    }

    public boolean isExhausted(UUID caseId, String workerName) {
        return roundCount(caseId, workerName) >= MAX_TRIAGE_ROUNDS;
    }

    public void clearForCase(UUID caseId) {
        String prefix = caseId.toString() + ":";
        rounds.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private static String key(UUID caseId, String workerName) {
        return caseId.toString() + ":" + workerName;
    }
}
