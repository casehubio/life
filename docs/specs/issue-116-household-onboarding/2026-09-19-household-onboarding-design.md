# Household Onboarding — Design Spec

**Date:** 2026-09-19
**Issue:** #116
**Status:** Draft
**Covers:** #116

## 1. Goal

A new user can set up a household in under 5 minutes: name the household,
add family members with roles, add external actors, and land on a working
dashboard. No SQL seeds, no REST API calls, no manual Keycloak config.

The same code path runs in dev mode (Keycloak Dev Services) and production
(external Keycloak deployment). No demo shims for auth — real OIDC tokens
throughout.

## 2. Architecture

### 2.1 OIDC — Keycloak Dev Services

Life ships a `realm-config.json` defining:
- Client: `casehub-life` (confidential, authorization code flow)
- Roles: `household-admin`, `household-member`, `household-junior`
- Custom claim mapping: `tenancyId` → JWT custom claim
- Bootstrap admin: a single `setup-admin` user (temporary password, forced
  change on first login) with `household-admin` role and no tenancyId — this
  solves the chicken-and-egg problem (no users = no authentication = can't
  reach onboarding). The onboarding flow creates the real admin, assigns
  tenancyId, and the bootstrap account can be deactivated afterward.

Imported via `quarkus.keycloak.devservices.realm-path=realm-config.json`.

**Auth model transition (from life#40):** The existing dev profile disables
OIDC and uses `DemoCurrentPrincipal` + `DemoIdentityProvider`. This was
appropriate when life had no user management — all requests shared one
identity. Onboarding requires real multi-user auth: different family members
with different roles making different requests. The transition:
- **dev profile**: enables Keycloak Dev Services + real OIDC. Existing
  `@TestSecurity` + `FixedCurrentPrincipal` patterns for `@QuarkusTest`
  are unaffected — they bypass OIDC regardless of dev profile config.
- **demo profile**: retains `DemoCurrentPrincipal` + `DemoIdentityProvider`
  for headless/CI environments without Docker.
- **test profile**: unchanged — `@TestSecurity` sets roles, `FixedCurrentPrincipal`
  sets groups/tenancyId. No Keycloak container in tests.

Config changes in `application.properties`:
```properties
# Remove these two lines from %dev profile:
# %dev.quarkus.oidc.enabled=false
# %dev.quarkus.keycloak.devservices.enabled=false

# Add:
%dev.quarkus.keycloak.devservices.realm-path=realm-config.json
```

The `demo` profile retains `DemoCurrentPrincipal` and `DemoIdentityProvider`
for headless/CI scenarios where Docker isn't available.

### 2.2 Entities

**Household** — one per tenancy, created during onboarding.

```java
@Entity
@Table(name = "household")
public class Household extends PanacheEntityBase {
    @Id
    public UUID id;                    // = tenancyId
    public String name;                // "The Proctors"
    public String timezone;            // "Europe/London"
    public String jurisdiction;        // "GB"
    public Instant createdAt;
}
```

**HouseholdMember** — one per family member, linked to Keycloak user.

```java
@Entity
@Table(name = "household_member")
public class HouseholdMember extends PanacheEntityBase {
    @Id
    public UUID id;
    public UUID householdId;
    public String keycloakUserId;      // Keycloak user UUID
    public String name;
    public String email;
    @Column(nullable = false, length = 32)
    public String role;                // validated against HouseholdGroups constants

    @Enumerated(EnumType.STRING)
    public MemberRelationship relationship;  // PARENT, CHILD, SPOUSE, GUARDIAN, OTHER
    public UUID relatedTo;             // another HouseholdMember.id (nullable)

    public String notificationChannel; // "sms" | "email" | "push" (nullable — default email)
    public String notificationValue;   // phone number or email for delivery

    public Instant joinedAt;
    public Instant deactivatedAt;      // soft delete — preserves audit trail

    @PrePersist
    void validateRole() {
        if (!Set.of(HouseholdGroups.ADMIN, HouseholdGroups.MEMBER, HouseholdGroups.JUNIOR).contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }
}
```

`MemberRelationship` enum: `PARENT`, `CHILD`, `SPOUSE`, `GUARDIAN`, `OTHER`.

These are permission-relevant relationships — `PARENT`→`CHILD` maps to
admin/member→junior authority chains. Everything else (who picks up Ella,
who is Jean's primary contact) belongs in the neocortex mind map (#121).

### 2.3 Services

**KeycloakAdminService** — wraps Keycloak Admin REST API.

Uses `quarkus-keycloak-admin-client` (Quarkus extension auto-configured to
the same Keycloak instance as OIDC Dev Services). Operations:
- `createUser(name, email, password, role)` → creates Keycloak user, assigns
  realm role, sets `tenancyId` user attribute
- `updateUserRole(keycloakUserId, newRole)` → changes realm role assignment
- `deactivateUser(keycloakUserId)` → disables Keycloak user (preserves for audit)
- `listUsers(tenancyId)` → users filtered by tenancy attribute

**HouseholdService** — orchestrates onboarding and household management.

- `createHousehold(name, timezone, jurisdiction)` → creates `Household` entity,
  calls `KeycloakAdminService` to configure the creating user's tenancy claim
- `addMember(householdId, name, email, role, relationship, relatedTo)` → creates
  Keycloak user + `HouseholdMember` record
- `removeMember(memberId)` → soft-deactivates (sets `deactivatedAt`, disables
  Keycloak user)
- `updateMemberRole(memberId, newRole)` → updates both entity and Keycloak
- `getOnboardingStatus()` → checks if current user's tenancy has a `Household`

**Tenancy scoping:** All `Household` and `HouseholdMember` queries are scoped
by `CurrentPrincipal.tenancyId()`. The `Household.id` IS the tenancy ID, so
`GET /household` resolves to `Household.findById(currentPrincipal.tenancyId())`.
Member queries filter by `householdId = currentPrincipal.tenancyId()`. No
cross-tenancy access is possible.

**Atomicity:** Keycloak Admin API calls are HTTP — they can't participate in
JPA transactions. Operation ordering: Keycloak first, JPA second. If Keycloak
succeeds but JPA fails, the compensating action is to disable the Keycloak
user. If Keycloak fails, the JPA operation is never attempted. `addMember()`
and `removeMember()` follow this pattern.

**VoiceEnrollmentService** — SPI stub for #120.

```java
public interface VoiceEnrollmentService {
    void enrollVoice(UUID memberId, byte[] voiceSample);
    boolean isEnrolled(UUID memberId);
    Optional<UUID> identifyByVoice(byte[] voiceSample);
}

@DefaultBean
@ApplicationScoped
public class NoOpVoiceEnrollmentService implements VoiceEnrollmentService {
    public void enrollVoice(UUID memberId, byte[] voiceSample) { }
    public boolean isEnrolled(UUID memberId) { return false; }
    public Optional<UUID> identifyByVoice(byte[] voiceSample) { return Optional.empty(); }
}
```

\#120 provides the real implementation backed by pages avatar voice detection.
GDPR note: voice samples are PII. When #120 lands, `LifeGdprErasureService`
must be extended to erase voice profiles via `VoiceEnrollmentService.eraseVoice(memberId)`.
This is tracked in #120's acceptance criteria.

### 2.4 REST Resources

**OnboardingResource** — `/onboarding`

| Method | Path | Role | What |
|--------|------|------|------|
| GET | `/onboarding/status` | any authenticated | `{needsOnboarding: bool}` — true if no Household for user's tenancy |
| POST | `/onboarding/household` | any authenticated | Create household + assign creator as admin |
| POST | `/onboarding/members` | household-admin | Bulk add members (Keycloak users + entities) |
| POST | `/onboarding/templates` | household-admin | Confirm or customise template selection |

**HouseholdResource** — `/household`

| Method | Path | Role | What |
|--------|------|------|------|
| GET | `/household` | admin, member, junior | Household details |
| PUT | `/household` | admin | Update name, timezone, jurisdiction |
| GET | `/household/members` | admin, member | List members with roles and relationships |
| POST | `/household/members` | admin | Add member |
| PUT | `/household/members/{id}` | admin | Update role, relationship |
| DELETE | `/household/members/{id}` | admin | Soft-deactivate member |
| GET | `/household/templates` | admin, member | List enabled templates |
| PUT | `/household/templates` | admin | Enable/disable templates |

### 2.5 Flyway Migration

`V113__household_and_members.sql`:

```sql
CREATE TABLE household (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    timezone      VARCHAR(64) NOT NULL DEFAULT 'UTC',
    jurisdiction  VARCHAR(10),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE household_member (
    id                UUID PRIMARY KEY,
    household_id      UUID NOT NULL REFERENCES household(id),
    keycloak_user_id  VARCHAR(255) NOT NULL,
    name              VARCHAR(255) NOT NULL,
    email             VARCHAR(255),
    role              VARCHAR(32) NOT NULL,
    relationship      VARCHAR(32),
    related_to           UUID REFERENCES household_member(id),
    notification_channel VARCHAR(32),
    notification_value   VARCHAR(255),
    joined_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at       TIMESTAMP
);

CREATE INDEX idx_household_member_household ON household_member(household_id);
```

## 3. Frontend

### 3.1 Onboarding wizard — `onboarding-view.ts`

Multi-step wizard, shown when `GET /onboarding/status` returns
`needsOnboarding: true`. The app shell checks on load and redirects.

| Step | UI | Skip? | Backend |
|------|-----|-------|---------|
| 1. Create household | Name, timezone (dropdown), jurisdiction (dropdown) | No | `POST /onboarding/household` |
| 2. Add members | Repeatable form: name, email, role (dropdown), relationship, notification pref (sms/email/push) | Yes ("add later") | `POST /onboarding/members` |
| 3. Add actors | Repeatable form: name, type, contact method, contact value | Yes | `POST /external-actors` (existing API, bulk mode) |
| 4. Templates + seed | Checklist of system templates with toggles. Checked templates seed initial tasks for the household. | Yes (keeps defaults, seeds from all) | `POST /onboarding/templates` → enables templates + calls `POST /life-tasks` per checked template to create starter tasks |
| 5. Done | Summary card (members added, actors added, tasks seeded) → "Go to dashboard" button | No | navigates to `#home` |

Progress indicator at top. Back/Next navigation. Each step commits to
the backend before advancing (no client-side-only state that can be lost).

### 3.2 Settings view — `settings-view.ts`

Accessible from gear icon in app shell toolbar. Tabs:

| Tab | Content |
|-----|---------|
| Members | List with name, role, relationship, notification pref, status. Add/edit/deactivate actions. |
| Household | Edit name, timezone, jurisdiction. |
| Templates | Toggle list matching onboarding step 4. Seed tasks from newly enabled templates. |
| Voice | Checks `GET /household/capabilities` — if `voiceEnrollment: true`, shows enrollment UI; otherwise shows "Coming soon" placeholder. |

### 3.3 App shell changes

- `GET /onboarding/status` on mount → redirect to `#onboarding` if needed
- Gear icon in toolbar → navigates to `#settings`
- User identity display shows current member name from `HouseholdMember`

## 4. Demo Profile

The `demo` profile retains its existing shims (`DemoCurrentPrincipal`,
`DemoIdentityProvider`) for environments without Docker. Demo mode skips
onboarding entirely — `import-demo.sql` seeds a `Household` and
`HouseholdMember` records alongside the existing demo data.

Add to `import-demo.sql`:
```sql
INSERT INTO household (id, name, timezone, jurisdiction, created_at)
VALUES ('278776f9-e1b0-46fb-9032-8bddebdcf9ce', 'Demo Household', 'Europe/London', 'GB', CURRENT_TIMESTAMP);

INSERT INTO household_member (id, household_id, keycloak_user_id, name, email, role, relationship, related_to, notification_channel, notification_value, joined_at)
VALUES
  ('a0000001-0000-0000-0000-000000000001', '278776f9-e1b0-46fb-9032-8bddebdcf9ce', 'demo-admin', 'Mark', 'mark@example.uk', 'household-admin', 'PARENT', NULL, 'sms', '+447700000001', CURRENT_TIMESTAMP),
  ('a0000001-0000-0000-0000-000000000002', '278776f9-e1b0-46fb-9032-8bddebdcf9ce', 'demo-member', 'Sarah', 'sarah@example.uk', 'household-member', 'PARENT', NULL, 'email', 'sarah@example.uk', CURRENT_TIMESTAMP),
  ('a0000001-0000-0000-0000-000000000003', '278776f9-e1b0-46fb-9032-8bddebdcf9ce', 'demo-junior-1', 'Ella', 'ella@example.uk', 'household-junior', 'CHILD', 'a0000001-0000-0000-0000-000000000001', NULL, NULL, CURRENT_TIMESTAMP),
  ('a0000001-0000-0000-0000-000000000004', '278776f9-e1b0-46fb-9032-8bddebdcf9ce', 'demo-junior-2', 'Tom', 'tom@example.uk', 'household-junior', 'CHILD', 'a0000001-0000-0000-0000-000000000001', NULL, NULL, CURRENT_TIMESTAMP);
```

## 5. Testing

| Test | Type | What |
|------|------|------|
| `HouseholdServiceTest` | `@QuarkusTest` | Create household, add members, update roles, soft-delete |
| `OnboardingResourceTest` | `@QuarkusTest` | Onboarding flow end-to-end: status → create → members → templates |
| `HouseholdResourceTest` | `@QuarkusTest` | CRUD operations, RBAC enforcement (junior can't add members) |
| `KeycloakAdminServiceTest` | `@QuarkusTest` | User creation, role assignment (requires dev services Keycloak) |
| `VoiceEnrollmentServiceTest` | Unit | NoOp impl returns expected defaults |

`FixedCurrentPrincipal` sets `tenancyId` to the household ID in tests.
`@TestSecurity` sets roles for RBAC tests.

## 6. Scope Boundaries

**In scope:**
- Household and member entities + migration
- Keycloak Dev Services realm config
- KeycloakAdminService for user CRUD
- Onboarding wizard (5-step)
- Settings view (4 tabs)
- VoiceEnrollmentService SPI stub
- Demo profile seed data
- App shell onboarding redirect + settings icon

**Out of scope (separate issues):**
- Voice enrollment implementation (#120)
- Mind map seeding from onboarding (#121 owns the seeding code; #116 provides
  the data via `HouseholdMember` and `ExternalActor` entities that #121 reads)
- Email invitations for new members (future)
- Google Contacts import (future)
- Production Keycloak deployment guide (ops concern)

## References

- `app/src/main/java/io/casehub/life/api/HouseholdGroups.java` — existing role constants
- `app/src/main/java/io/casehub/life/app/demo/DemoCurrentPrincipal.java` — demo auth shim (retained for demo profile)
- `app/src/main/java/io/casehub/life/app/demo/DemoIdentityProvider.java` — demo HTTP auth (retained)
- `app/src/main/resources/application.properties` — OIDC + dev services config
- `app/src/main/resources/import-demo.sql` — demo seed data
- Quarkus Keycloak Dev Services: https://quarkus.io/guides/security-openid-connect-dev-services
- Quarkus Keycloak Admin Client: https://quarkus.io/guides/security-keycloak-admin-client
- casehubio/life#120 — voice intake (blocked by this)
- casehubio/life#121 — family mind map (blocked by this)
- `specs/issue-116-household-onboarding/decisions.md` — D1–D5 design decisions
