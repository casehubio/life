package io.casehub.life.api;

/**
 * CDI group names used in household approval routing.
 * Matched against candidateGroups in RiskDecision.GateRequired.
 */
public final class HouseholdGroups {
    public static final String ADMIN  = "household-admin";
    public static final String MEMBER = "household-member";
    public static final String JUNIOR = "household-junior";

    private HouseholdGroups() {}
}
