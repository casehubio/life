package io.casehub.life.api.commitment;

public enum CommitmentMode {
    DELEGATION("%s has not confirmed — action required"),
    CONTRACTOR("Contractor has not confirmed by deadline"),
    OVERSIGHT("Oversight gate expired — request not approved");

    private final String escalationTemplate;

    CommitmentMode(final String escalationTemplate) {
        this.escalationTemplate = escalationTemplate;
    }

    public String escalationTemplate() {
        return escalationTemplate;
    }
}
