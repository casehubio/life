# LifeAgent Identity Consolidation

**Issue:** life#46 (expanded scope)
**Date:** 2026-06-27

## Problem

Agent identity — persona, agentId, display name, slot, briefing — is scattered across string literals in 7 CaseHub classes. Three forms of the same identity appear independently:

1. **Full agentId** (`"openclaw:health-agent@1"`) — in AgentDescriptor constructors and `setAgentDescriptors()` Map keys (19 occurrences)
2. **Persona name** (`"health-agent"`) — in `forAgent()` calls (32 occurrences)
3. **AgentDescriptor boilerplate** — 18-arg constructor with 14 constant fields, repeated in 7 private methods

Additionally, `jurisdiction` is hardcoded to `"GB"` in all 7 descriptors (deployment-specific data in code), and `tenancyId` is injected via `@ConfigProperty` into all 7 CaseHubs solely for descriptor construction.

**Briefing drift:** CaseHubs sharing the same agent have divergent briefings. AppointmentCycleCaseHub uses "Health domain booking and follow-up agent" while CareEpisodeCaseHub and CareCoordinationCaseHub use "Health domain agent". Nothing prevents further divergence — each CaseHub independently constructs its own descriptor.

## Design

### Design benefit: descriptor consistency guarantee

Today, three CaseHubs independently construct HEALTH descriptors with nothing preventing divergence (briefings already differ). After this change, all three use `descriptorFactory.descriptorFor(LifeAgent.HEALTH)` — one source of truth, guaranteed consistent. The same guarantee applies to HOME (shared by HomeMaintenanceCaseHub and ContractorCoordinationCaseHub).

### 1. `LifeAgent` enum — `io.casehub.life.app.engine`

Pure identity data. No framework dependencies.

```java
public enum LifeAgent {
    HEALTH("health-agent", "OpenClaw Health Agent",
           "casehubio/life/health", "Health domain coordination agent"),
    HOME("home-agent", "OpenClaw Home Agent",
         "casehubio/life/household", "Household maintenance agent"),
    FINANCE("finance-agent", "OpenClaw Finance Agent",
            "casehubio/life/finance", "Financial review and governance agent"),
    TRAVEL("travel-agent", "OpenClaw Travel Agent",
           "casehubio/life/travel", "Travel planning and booking agent");

    public static final String MODEL_FAMILY = "openclaw";
    public static final int MAJOR_VERSION = 1;

    private final String persona;
    private final String displayName;
    private final String slot;
    private final String briefing;
}
```

**Briefing choices:**
- HEALTH: "Health domain coordination agent" — replaces divergent values ("booking and follow-up agent" / "Health domain agent"). Covers appointment booking, care coordination, and care episodes accurately.
- HOME: "Household maintenance agent" — already consistent across both CaseHubs.
- FINANCE, TRAVEL: single CaseHub each, no divergence.

**Derived accessors:**

- `agentId()` — returns `"openclaw:health-agent@1"` (format: `{MODEL_FAMILY}:{persona}@{MAJOR_VERSION}`)
- `persona()` — returns `"health-agent"` (the persona fragment, used by `LifeOpenClawChatModelFactory` and future WorkerProvisioner config)

**Constants:**

- `MODEL_FAMILY` — `public static final String`, value `"openclaw"`
- `MAJOR_VERSION` — `public static final int`, value `1`

**Why separate from `LifeDomain`:** The mapping is not 1:1. CONTRACTOR_COORDINATION and HOUSEHOLD share `home-agent`. FAMILY_SCHEDULING, LEGAL, and ELDER_CARE have no agent. Agent identity is an OpenClaw integration concern, not a domain concept. `LifeDomain` (in `api/`) models domain semantics; `LifeAgent` (in `app/`) models agent integration.

### 2. `LifeAgentDescriptorFactory` — `io.casehub.life.app.engine.agent`

CDI bean that owns config-to-descriptor construction. Uses constructor injection for unit testability.

```java
@ApplicationScoped
public class LifeAgentDescriptorFactory {

    private final String tenancyId;
    private final String jurisdiction;

    @Inject
    public LifeAgentDescriptorFactory(
            @ConfigProperty(name = "casehub.life.tenancy-id") String tenancyId,
            @ConfigProperty(name = "casehub.life.jurisdiction", defaultValue = "GB") String jurisdiction) {
        this.tenancyId = tenancyId;
        this.jurisdiction = jurisdiction;
    }

    public AgentDescriptor descriptorFor(LifeAgent agent) {
        return AgentDescriptor.builder()
                .agentId(agent.agentId())
                .name(agent.displayName())
                .version(String.valueOf(LifeAgent.MAJOR_VERSION))
                .provider(LifeAgent.MODEL_FAMILY)
                .modelFamily(LifeAgent.MODEL_FAMILY)
                .slot(agent.slot())
                .jurisdiction(jurisdiction)
                .tenancyId(tenancyId)
                .briefing(agent.briefing())
                .build();
    }
}
```

**Why a factory, not `descriptor()` on the enum:**

- Eliminates 7 `@ConfigProperty tenancyId` injections from CaseHubs
- Makes `jurisdiction` configurable (fixes hardcoded "GB")
- Natural extension point when capabilities, disposition, vocabulary URIs are populated
- Life#37 (WorkerProvisioner) can inject the same factory

**Package placement:** `app.engine.agent` — alongside `LifeOpenClawChatModelFactory` and response schema records. The factory is agent infrastructure (creates platform identity objects), not CaseHub orchestration.

**Config additions to `application.properties`:**

- `casehub.life.jurisdiction=GB` — new property (default "GB", overridable per deployment)
- `casehub.life.tenancy-id` — already exists, now consumed by factory instead of 7 CaseHubs

### 3. `LifeOpenClawChatModelFactory` — signature change

Change `forAgent(String openClawAgentId)` to `forAgent(LifeAgent agent)`. No string overload retained. Extracts `agent.persona()` internally.

```java
public ChatModelProvider forAgent(LifeAgent agent) {
    var provider = new OpenClawAgentProvider(
            bridge, hookClient, agent.persona(), deliveryBaseUrl);
    var chatModel = new OpenClawChatModel(
            provider, Duration.ofSeconds(timeoutSeconds));
    return new ChatModelProvider() {
        @Override public ModelType type() { return ModelType.OPENAI; }
        @Override public dev.langchain4j.model.chat.ChatModel get() { return chatModel; }
    };
}
```

`TestLifeOpenClawChatModelFactory` updates accordingly — the agent parameter is unused (canned responses match on system prompt content).

### 4. CaseHub cleanup

Each of the 7 CaseHubs changes to:

```java
private static final LifeAgent AGENT = LifeAgent.HEALTH;  // or HOME, FINANCE, TRAVEL

@Inject LifeOpenClawChatModelFactory openClawFactory;
@Inject LifeAgentDescriptorFactory descriptorFactory;
// @ConfigProperty tenancyId — REMOVED

private CaseDefinition augment(final CaseDefinition yaml) {
    yaml.getWorkers().addAll(List.of(
            bookAppointmentWorker(), ...
    ));
    yaml.setAgentDescriptors(Map.of(
            AGENT.agentId(), descriptorFactory.descriptorFor(AGENT)));
    return yaml;
}

private Worker bookAppointmentWorker() {
    final Agent agent = Agent.builder()
            .model(openClawFactory.forAgent(AGENT))
            .systemPrompt(...)
            .responseSchema(BookingResult.class)
            .build();
    return Worker.builder()
            .name("book-appointment-agent")
            .capabilities(List.of(cap("book-appointment")))
            .function(new AgentWorkerFunction(agent))
            .build();
}
```

**Per CaseHub, removed:**
- Private `*Descriptor()` method (18-arg constructor)
- `@ConfigProperty tenancyId` field
- All agentId string literals
- All persona string literals

**CaseHub → LifeAgent mapping:**

| CaseHub | LifeAgent |
|---------|-----------|
| AppointmentCycleCaseHub | HEALTH |
| CareCoordinationCaseHub | HEALTH |
| CareEpisodeCaseHub | HEALTH |
| HomeMaintenanceCaseHub | HOME |
| ContractorCoordinationCaseHub | HOME |
| FinancialReviewCaseHub | FINANCE |
| TravelPlanCaseHub | TRAVEL |
| FamilyVoteCaseHub | (none — pure humanTask, no change) |

## Testing

- **`LifeAgentTest`** — plain unit test: `agentId()` format derivation, `persona()` extraction, all 4 constants have correct identity data, `MODEL_FAMILY` and `MAJOR_VERSION` constants
- **`LifeAgentDescriptorFactoryTest`** — plain unit test: construct factory directly via constructor (`new LifeAgentDescriptorFactory("test-tenant", "GB")`), verify descriptor fields match agent identity + injected config values for each `LifeAgent` constant
- **`AppointmentCycleCaseHubTest`** — update existing @QuarkusTest: replace `"openclaw:health-agent@1"` string literals with `LifeAgent.HEALTH.agentId()` (4 occurrences)
- **All 7 CaseHub @QuarkusTest classes** — verify unchanged behaviour after refactoring (no new tests needed, existing tests cover the integration)

## Consolidation summary

| Before | After |
|--------|-------|
| 7 private descriptor methods (18-arg constructors) | 1 factory method using builder |
| 19 agentId string literals | 4 enum constants |
| 32 persona string literals in `forAgent()` | `AGENT` constant per CaseHub |
| 7 `@ConfigProperty tenancyId` in CaseHubs | 1 in factory |
| 7 hardcoded `"GB"` | 1 `@ConfigProperty` with default |
| Divergent briefings across shared agents | 1 canonical briefing per agent |

## Protocol impact

- **PP-20260618-openclaw-agent** — update to reference `LifeAgent` enum and `LifeAgentDescriptorFactory` instead of describing per-CaseHub descriptor methods
- **CLAUDE.md Layer 7 additions** — update to document `LifeAgent`, `LifeAgentDescriptorFactory`

## Out of scope

- Structural CaseHub duplication (augment pattern, cap() helper, double-checked lock) — tracked as life#47
- `LifeOpenClawChatModelFactory` internal caching — marginal gain, separate concern
- LifeAgent ↔ LifeDomain typed mapping — not needed until a consumer requires it
