package io.casehub.life.api;

public enum LifeCaseType {
    TRAVEL_PLAN("travel-plan"),
    HOME_MAINTENANCE("home-maintenance"),
    CARE_COORDINATION("care-coordination"),
    APPOINTMENT_CYCLE("appointment-cycle"),
    CONTRACTOR_COORDINATION("contractor-coordination"),
    FINANCIAL_REVIEW("financial-review");

    private final String caseName;

    LifeCaseType(String caseName) {
        this.caseName = caseName;
    }

    public String caseName() {
        return caseName;
    }
}
