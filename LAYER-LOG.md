# casehub-life Agentic Harness — Layer Log

Structured record of what was built at each tutorial layer, optimised for LLM consumption.
Each entry is the raw material needed to reproduce the layer in a different domain harness.
Entries are ordered for learning, not chronology. Each entry is complete when the layer closes.

Cross-references:
- Platform compliance gap analysis: `docs/specs/life-automation.md`
- Actor model: `docs/specs/life-actor-model.md`
- Tutorial teaching objectives: `../parent/docs/tutorial-strategy.md`
- AML reference implementation: `../aml/LAYER-LOG.md`
- Clinical reference implementation: `../clinical/LAYER-LOG.md`
- Research spec: `../parent/docs/specs/2026-05-25-openclaw-casehub-integration.md`

**Architectural note — hexagonal pattern:** casehub-life uses the AML api/app split:
- `api/` — pure Java, zero framework imports, zero JPA. Domain records and constants.
- `app/` — Quarkus application: Panache entities, REST resources, Flyway, foundation wiring.

**Strategic positioning note:** casehub-life is a developer showcase in the devtown/clinical
tradition — tutorial layers by foundation module adoption sequence. The open question (spec §5.8)
of consumer product vs developer showcase is noted and does not foreclose either direction.

---

## Layer 1 — Naive Java (no CaseHub)

**Status:** Pending
**Planned issues:** casehubio/life#2 (Epic 2: scaffold + domain model)

### What it will show

Household domain model with no CaseHub foundation modules. Core entities in app/ (Quarkus Panache)
and domain constants in api/ (pure Java). A REST API that persists entities directly with no
accountability, SLA, or obligation tracking. A showcase scenario test covering the full domain
hierarchy end-to-end.

This is the baseline every subsequent layer improves. The gaps are structural — REST calls go
directly to the database. No record of who committed to what. No SLA governs how long a task
sits. No formal obligation exists when a contractor says they'll come Thursday.

### Key files (planned)

- `api/src/main/java/io/casehub/life/api/LifeDomain.java` — enum: HEALTH, FINANCE, HOUSEHOLD, LEGAL, CARE, TRAVEL
- `api/src/main/java/io/casehub/life/api/LifeCapabilities.java` — capability tag constants
- `api/src/main/java/io/casehub/life/api/LifeTrustDimensions.java` — trust dimension constants
- `api/src/main/java/io/casehub/life/api/ActorType.java` — AI_AGENT, HOUSEHOLD_PRINCIPAL, EXTERNAL_HUMAN
- `api/src/main/java/io/casehub/life/api/model/` — enums: HouseholdTaskStatus, LifeGoalStatus, ExternalActorType
- `app/src/main/java/io/casehub/life/entity/HouseholdTask.java` — task: domain, title, description, deadline, status, assignedTo
- `app/src/main/java/io/casehub/life/entity/LifeGoal.java` — goal: domain, title, targetDate, status
- `app/src/main/java/io/casehub/life/entity/LifeEvent.java` — event: domain, title, occurredAt, description
- `app/src/main/java/io/casehub/life/entity/ExternalActor.java` — contractor/doctor: name, contactMethod, contactValue, type
- `app/src/main/java/io/casehub/life/resource/HouseholdTaskResource.java` — POST/GET /tasks
- `app/src/main/java/io/casehub/life/resource/LifeGoalResource.java` — POST/GET /goals
- `app/src/test/java/io/casehub/life/ShowcaseScenarioTest.java` — household week scenario

### The gap comments (planned)

```java
// LAYER 1 GAP: no SLA — a contractor task created here can sit indefinitely.
// Layer 2 adds a WorkItem claimDeadline when the task is created.

// LAYER 1 GAP: no commitment tracking — "plumber committed to come Thursday"
// is a mental note, not a machine-tracked obligation.
// Layer 3 adds Qhorus COMMAND + Commitment lifecycle for external actor commitments.

// LAYER 1 GAP: no tamper-evident audit — health and financial decisions have
// no independently verifiable record.
// Layer 4 adds the Merkle ledger.

// LAYER 1 GAP: no formal obligation — "pick up kids at 3:30" is a chat message.
// No RESPONSE required, no Watchdog fires if the pickup doesn't happen.
```

### Pattern to replicate

1. Create `api/` — pure Java, zero framework, zero JPA. Domain enums and constants only.
2. Define `LifeDomain` enum — each domain scopes a permission boundary and routing context.
3. Create `app/` — Quarkus Panache Active Record entities for each core entity type.
4. Flyway migrations V100–V199 (casehub-work owns V1–V21+; ledger owns V1000–V1007).
5. Write a showcase test that exercises the full entity hierarchy.
6. Add gap comments to every service call that bypasses accountability.

---

## Layer 2 — + casehub-work (SLA enforcement)

**Status:** Pending
**Planned issues:** casehubio/life#3 (Epic 3: casehub-work)

### What it will show

Adds `casehub-work` to create formal WorkItems with deadlines for household tasks that matter.
A grocery order with a Wednesday deadline. A boiler service booking with a 14-day SLA.
A contractor task with a follow-up deadline. The platform escalates if the deadline passes —
the household app does not need to know how.

### Key wiring (planned)

- `HouseholdTaskService.createTask()` → `WorkItemCreateRequest` with `claimDeadline`
- `casehub-work-api` in api/pom.xml (safe — pure Java); `casehub-work` in app/pom.xml
- Flyway: domain migrations must be V100+; casehub-work occupies V1–V21+

---

## Layer 3 — + casehub-qhorus (commitment lifecycle)

**Status:** Pending
**Planned issues:** casehubio/life#4 (Epic 4: casehub-qhorus)

### What it will show

Adds `casehub-qhorus` for formal COMMAND/RESPONSE commitment tracking. Family task delegation
("pick up kids at 3:30") becomes a COMMAND with a RESPONSE requirement and a Watchdog. External
actor commitment tracking ("plumber committed to come Thursday") becomes a tracked obligation
with automated follow-up via OpenClaw's messaging skill when the Watchdog fires.

Oversight channel gates: any household decision above a configurable threshold (spend > £X,
medical decision, legal action) routes to the oversight channel. Human RESPONSE required before
any action proceeds. OpenClaw delivers the oversight message via WhatsApp/Telegram.

---

## Layer 4 — + casehub-ledger (tamper-evident audit)

**Status:** Pending
**Planned issues:** casehubio/life#5 (Epic 5: casehub-ledger)

### What it will show

Adds `casehub-ledger` for tamper-evident Merkle audit of health decisions, financial decisions,
and legal actions. GDPR Art.17 erasure for personal data stored in the ledger. Every major
decision has a cryptographically verifiable record — not just a database entry.

---

## Layer 5 — + casehub-engine (multi-step workflows)

**Status:** Pending
**Planned issues:** casehubio/life#6 (Epic 6: casehub-engine)

### What it will show

Adds `casehub-engine` for complex multi-step CasePlanModel workflows. Travel planning
(destination research → budget gate → flight search → human approval → booking → reminders).
Care coordination (assessment → care plan → site assignments → SLA monitoring).
The CasePlanModel replaces linear REST calls with adaptive workflow orchestration.

---

## Layer 6 — Trust routing

**Status:** Pending
**Planned issues:** casehubio/life#7 (Epic 7: trust routing)

### What it will show

Trust-weighted agent routing. Which health-agent has the highest deadline-reliability score?
Which finance-agent has the best cost-accuracy record? Over time, the platform learns which
agents handle which household domains reliably and routes accordingly.

---

## Layer 7 — + casehub-openclaw (OpenClaw integration)

**Status:** Pending
**Planned issues:** casehubio/life#8 (Epic 8: casehub-openclaw)

### What it will show

Adds casehub-openclaw as the WorkerProvisioner. OpenClaw instances execute household skills:
banking API aggregation (Open Banking skills), Google Calendar integration (calendar skill),
Home Assistant smart home control (IoT skill), WhatsApp/SMS follow-up (messaging skills).

The ChannelContextWindow ensures each OpenClaw agent wakes with fresh context from Qhorus
channels — grocery-agent sees finance-agent's budget warning before placing an order;
health-agent sees smart home movement data before sending a medication reminder.

This layer demonstrates the bidirectional integration: CaseHub orchestrates via direct call
to /hooks/agent; OpenClaw heartbeat monitors ambient conditions and creates CaseHub WorkItems
when conditions warrant. Neither replaces the other.
