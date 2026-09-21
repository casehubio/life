package io.casehub.life.app.service;

import io.casehub.life.api.response.HouseholdCapabilitiesResponse;
import io.casehub.life.api.response.HouseholdMemberResponse;
import io.casehub.life.api.response.HouseholdResponse;
import io.casehub.life.api.response.OnboardingStatusResponse;
import io.casehub.life.api.request.CreateHouseholdRequest;
import io.casehub.life.api.request.CreateMemberRequest;
import io.casehub.life.api.spi.VoiceEnrollmentService;
import io.casehub.life.app.entity.Household;
import io.casehub.life.app.entity.HouseholdMember;
import io.casehub.platform.api.identity.CurrentPrincipal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class HouseholdService {

    @Inject CurrentPrincipal currentPrincipal;
    @Inject VoiceEnrollmentService voiceEnrollmentService;

    public OnboardingStatusResponse getOnboardingStatus() {
        UUID tenancyId = UUID.fromString(currentPrincipal.tenancyId());
        boolean exists = Household.findByTenancyId(tenancyId).isPresent();
        return new OnboardingStatusResponse(!exists);
    }

    public HouseholdResponse getHousehold() {
        UUID tenancyId = UUID.fromString(currentPrincipal.tenancyId());
        return Household.findByTenancyId(tenancyId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Household not found"));
    }

    @Transactional
    public HouseholdResponse createHousehold(CreateHouseholdRequest req) {
        Household h = new Household();
        h.id = UUID.fromString(currentPrincipal.tenancyId());
        h.name = req.name();
        h.timezone = req.timezone();
        h.jurisdiction = req.jurisdiction();
        h.persist();
        return toResponse(h);
    }

    @Transactional
    public HouseholdMemberResponse addMember(CreateMemberRequest req) {
        UUID householdId = UUID.fromString(currentPrincipal.tenancyId());
        HouseholdMember m = new HouseholdMember();
        m.householdId = householdId;
        m.keycloakUserId = "pending-" + UUID.randomUUID();
        m.name = req.name();
        m.email = req.email();
        m.role = req.role();
        m.relationship = req.relationship();
        m.relatedTo = req.relatedTo();
        m.notificationChannel = req.notificationChannel();
        m.notificationValue = req.notificationValue();
        m.persist();
        return toMemberResponse(m);
    }

    public List<HouseholdMemberResponse> listMembers() {
        UUID householdId = UUID.fromString(currentPrincipal.tenancyId());
        return HouseholdMember.findByHouseholdId(householdId).stream()
                .map(this::toMemberResponse).toList();
    }

    public HouseholdCapabilitiesResponse getCapabilities() {
        return new HouseholdCapabilitiesResponse(
                !(voiceEnrollmentService instanceof io.casehub.life.app.spi.NoOpVoiceEnrollmentService));
    }

    private HouseholdResponse toResponse(Household h) {
        return new HouseholdResponse(h.id, h.name, h.timezone, h.jurisdiction, h.createdAt);
    }

    private HouseholdMemberResponse toMemberResponse(HouseholdMember m) {
        return new HouseholdMemberResponse(
                m.id, m.name, m.email, m.role,
                m.relationship, m.relatedTo,
                m.notificationChannel, m.notificationValue,
                m.joinedAt, m.isActive());
    }
}
