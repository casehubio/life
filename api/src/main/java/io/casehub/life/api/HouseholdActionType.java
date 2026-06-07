package io.casehub.life.api;

import java.util.List;
import java.util.Optional;

/**
 * Typed taxonomy of consequential household actions declared by workers before execution.
 * Workers use actionType() when constructing PlannedAction; fromActionType() reverses the mapping.
 * Each constant encodes its inherent domain properties — gatePolicy, thresholdCategory,
 * reversible, candidateGroups — so all logic for a type lives here.
 */
public enum HouseholdActionType {

    SPEND_PURCHASE(
        GatePolicy.AMOUNT_THRESHOLD, ThresholdCategory.SPEND, true,
        List.of(HouseholdGroups.ADMIN)),

    SPEND_SUBSCRIPTION_CANCEL(
        GatePolicy.ALWAYS, null, true,
        List.of(HouseholdGroups.ADMIN)),

    SPEND_SUBSCRIPTION_MODIFY(
        GatePolicy.AMOUNT_THRESHOLD, ThresholdCategory.SPEND, true,
        List.of(HouseholdGroups.ADMIN)),

    BOOKING_NONREFUNDABLE(
        GatePolicy.ALWAYS, null, false,
        List.of(HouseholdGroups.ADMIN)),

    BOOKING_REFUNDABLE(
        GatePolicy.AMOUNT_THRESHOLD, ThresholdCategory.BOOKING, true,
        List.of(HouseholdGroups.ADMIN)),

    HEALTH_APPOINTMENT_SPECIALIST(
        GatePolicy.ALWAYS, null, true,
        List.of(HouseholdGroups.ADMIN)),

    /** Routine GP booking — no gate required. */
    HEALTH_APPOINTMENT_GP(
        GatePolicy.NEVER, null, true,
        List.of()),

    /** Medication interaction — irreversible safety concern; any adult can approve (speed matters). */
    HEALTH_MEDICATION_FLAG(
        GatePolicy.ALWAYS, null, false,
        List.of(HouseholdGroups.ADMIN, HouseholdGroups.MEMBER)),

    CONTRACTOR_ENGAGE(
        GatePolicy.AMOUNT_THRESHOLD, ThresholdCategory.CONTRACTOR, true,
        List.of(HouseholdGroups.ADMIN)),

    LEGAL_DOCUMENT_SUBMIT(
        GatePolicy.ALWAYS, null, false,
        List.of(HouseholdGroups.ADMIN)),

    /** Care decision for a dependent — any adult can approve (urgency matters). */
    ELDER_CARE_DECISION(
        GatePolicy.ALWAYS, null, true,
        List.of(HouseholdGroups.ADMIN, HouseholdGroups.MEMBER));

    public enum GatePolicy {
        ALWAYS,           // unconditional gate
        AMOUNT_THRESHOLD, // gate when context["amount"] >= configured threshold
        NEVER             // always autonomous
    }

    /**
     * Maps AMOUNT_THRESHOLD types to their threshold config category.
     * Null for ALWAYS and NEVER types — no threshold applies.
     */
    public enum ThresholdCategory { SPEND, BOOKING, CONTRACTOR }

    private final GatePolicy gatePolicy;
    private final ThresholdCategory thresholdCategory;
    private final boolean reversible;
    private final List<String> candidateGroups;

    HouseholdActionType(GatePolicy gatePolicy, ThresholdCategory thresholdCategory,
                        boolean reversible, List<String> candidateGroups) {
        this.gatePolicy = gatePolicy;
        this.thresholdCategory = thresholdCategory;
        this.reversible = reversible;
        this.candidateGroups = List.copyOf(candidateGroups);
    }

    public GatePolicy gatePolicy() { return gatePolicy; }

    /** Null for ALWAYS and NEVER types. */
    public ThresholdCategory thresholdCategory() { return thresholdCategory; }

    public boolean reversible() { return reversible; }

    public List<String> candidateGroups() { return candidateGroups; }

    /** The actionType string for PlannedAction.actionType(). e.g. SPEND_PURCHASE → "spend.purchase" */
    public String actionType() {
        return name().toLowerCase().replace('_', '.');
    }

    /** Parse a PlannedAction.actionType() string back to enum. Empty if unknown. */
    public static Optional<HouseholdActionType> fromActionType(String actionType) {
        if (actionType == null) return Optional.empty();
        try {
            return Optional.of(valueOf(actionType.toUpperCase().replace('.', '_')));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
