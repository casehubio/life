# casehub-life Agentic Harness — Layer Log

Structured record of what was built at each integration layer, optimised for LLM consumption.
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
tradition — integration layers by foundation module adoption sequence. The open question (spec §5.8)
of consumer product vs developer showcase is noted and does not foreclose either direction.

---

## Layer 1 — Domain baseline (no CaseHub foundation)

**Status:** Pending
**Issue:** casehubio/life#2
**Navigation:** `git log --grep="#2" --oneline` (fill in at layer close)

### What it shows

Household domain model with no CaseHub foundation modules. Core entities in `app/`
(Quarkus Panache) and domain constants in `api/` (pure Java). A REST API that persists
household tasks, goals, events, and external actors directly — no accountability, SLA, or
obligation tracking.

This is the starting point every subsequent layer improves. The gaps are structural: REST calls
go directly to the database. No record of who committed to what. No SLA governs how long a task
sits. No formal obligation exists when a contractor says they will come on Thursday.

### Accountability gaps this layer leaves open

These gaps are what the subsequent layers close, one foundation module at a time:

| Gap | What breaks | Closed by |
|-----|-------------|-----------|
| No SLA enforcement | A contractor task created here can sit indefinitely | Layer 2 (casehub-work) |
| No commitment tracking | "Plumber committed to come Thursday" is a mental note, not a machine-tracked obligation | Layer 3 (casehub-qhorus) |
| No tamper-evident audit | Health and financial decisions have no independently verifiable record | Layer 4 (casehub-ledger) |
| No formal obligation | "Pick up kids at 3:30" has no required RESPONSE, no Watchdog | Layer 3 (casehub-qhorus) |
| No escalation path | Missed task sits silently — no automatic notification or escalation | Layer 2 (casehub-work) |

### Key files (planned)

- `api/src/main/java/io/casehub/life/api/LifeDomain.java` — enum: HOUSEHOLD, HEALTH, FINANCE, FAMILY_SCHEDULING, TRAVEL, LEGAL, CONTRACTOR_COORDINATION, ELDER_CARE
- `api/src/main/java/io/casehub/life/api/LifeCapabilities.java` — capability tag constants
- `api/src/main/java/io/casehub/life/api/LifeTrustDimensions.java` — trust dimension constants
- `api/src/main/java/io/casehub/life/api/ActorType.java` — AI_AGENT, HOUSEHOLD_PRINCIPAL, EXTERNAL_HUMAN
- `api/src/main/java/io/casehub/life/api/model/` — enums: HouseholdTaskStatus, LifeGoalStatus, ExternalActorType
- `app/src/main/java/io/casehub/life/entity/HouseholdTask.java` — task: domain, title, description, deadline, slaHours, status, assignedTo
- `app/src/main/java/io/casehub/life/entity/LifeGoal.java` — goal: domain, title, targetDate, status
- `app/src/main/java/io/casehub/life/entity/LifeEvent.java` — event: domain, title, occurredAt, description
- `app/src/main/java/io/casehub/life/entity/ExternalActor.java` — contractor/doctor: name, contactMethod, contactValue, type
- `app/src/main/java/io/casehub/life/service/HouseholdTaskService.java` — createTask, listTasks, completeTask
- `app/src/main/java/io/casehub/life/service/ExternalActorService.java` — register, list
- `app/src/main/java/io/casehub/life/resource/HouseholdTaskResource.java` — POST/GET /tasks
- `app/src/main/java/io/casehub/life/resource/LifeGoalResource.java` — POST/GET /goals
- `app/src/test/java/io/casehub/life/ShowcaseScenarioTest.java` — household week @QuarkusTest narrative

### Architectural decisions

- **Direct Panache, no service SPI** — casehub-life is an application, not a library. Services grow across layers (Layer 2 adds a WorkItem call alongside the existing persist) rather than being replaced by alternative implementations. No `@DefaultBean` substitution pattern needed.
- **`slaHours` in Layer 1** — declared on `HouseholdTask` even though nothing enforces it until Layer 2. Correct domain modelling: a household task has an expected SLA whether or not the platform enforces it.
- **`ExternalActor` is life-specific** — not in casehub-qhorus-api. The actor model spec left this open; Layer 1 resolves it as a life-domain entity. Layers 2-3 add Qhorus commitment tracking against it.
- **`api/` is domain vocabulary only** — enums, constants, value records. No service interfaces. The api/app split follows the hexagonal pattern from AML and clinical.

### Pattern to replicate

1. Create `api/` — pure Java, zero framework, zero JPA. Domain enums and constants only.
2. Define `LifeDomain` enum — each domain scopes a permission boundary and routing context.
3. Create `app/` — Quarkus Panache Active Record entities for each core entity type.
4. Flyway migrations V100–V199 (casehub-work owns V1–V21+; ledger owns V1000–V1007).
5. Write a `@QuarkusTest ShowcaseScenarioTest` that narrates a full household week: task created, contractor engaged, appointment booked — showing the accountability gaps in sequence.
6. Unit-test stateless domain logic (status transitions, validation) in pure Java without Quarkus.

---

## Layer 2 — + casehub-work (SLA enforcement)

**Status:** Pending
**Issue:** casehubio/life#3
**Navigation:** `git log --grep="#3" --oneline` (fill in at layer close)

### What it shows

Integrates `casehub-work` to create formal WorkItems with deadlines for household tasks that
matter. A grocery order with a Wednesday deadline. A boiler service booking with a 14-day SLA.
A contractor task with a follow-up deadline. The platform escalates if the deadline passes —
the household app does not need to know how.

### Accountability gaps closed

- No SLA on household tasks → `WorkItem.claimDeadline` enforces the deadline
- No escalation path → casehub-work fires escalation automatically on breach
- No formal human task inbox → WorkItem provides a structured inbox per principal

### Key wiring (planned)

- `HouseholdTaskService.createTask()` → `WorkItemCreateRequest` with `claimDeadline` alongside `task.persist()`
- `casehub-work-api` in `api/pom.xml` (safe — pure Java, no JPA)
- `casehub-work` in `app/pom.xml`
- Flyway: domain migrations remain at V100+; casehub-work occupies V1–V21+

---

## Layer 3 — + casehub-qhorus (commitment lifecycle)

**Status:** Pending
**Issue:** casehubio/life#4
**Navigation:** `git log --grep="#4" --oneline` (fill in at layer close)

### What it shows

Integrates `casehub-qhorus` for formal COMMAND/RESPONSE commitment tracking. Family task
delegation ("pick up kids at 3:30") becomes a COMMAND with a RESPONSE requirement and a
Watchdog. External actor commitment tracking ("plumber committed to come Thursday") becomes a
tracked obligation with automated follow-up via OpenClaw's messaging skill when the Watchdog fires.

Oversight channel gates: any household decision above a configurable threshold (spend > £X,
medical decision, legal action) routes to the oversight channel. Human RESPONSE required before
any action proceeds.

---

## Layer 4 — + casehub-ledger (tamper-evident audit)

**Status:** Pending
**Issue:** casehubio/life#5
**Navigation:** `git log --grep="#5" --oneline` (fill in at layer close)

### What it shows

Integrates `casehub-ledger` for tamper-evident Merkle audit of health decisions, financial
decisions, and legal actions. GDPR Art.17 erasure for personal data stored in the ledger. Every
major decision has a cryptographically verifiable record — not just a database entry.

---

## Layer 5 — + casehub-engine (multi-step workflows)

**Status:** Pending
**Issue:** casehubio/life#6
**Navigation:** `git log --grep="#6" --oneline` (fill in at layer close)

### What it shows

Integrates `casehub-engine` for complex multi-step CasePlanModel workflows. Travel planning
(destination research → budget gate → flight search → human approval → booking → reminders).
Care coordination (assessment → care plan → site assignments → SLA monitoring).
The CasePlanModel replaces linear REST calls with adaptive workflow orchestration.

---

## Layer 6 — Trust routing

**Status:** Pending
**Issue:** casehubio/life#7
**Navigation:** `git log --grep="#7" --oneline` (fill in at layer close)

### What it shows

Trust-weighted agent routing. Which health-agent has the highest deadline-reliability score?
Which finance-agent has the best cost-accuracy record? Over time, the platform learns which
agents handle which household domains reliably and routes accordingly.

---

## Layer 7 — + casehub-openclaw (OpenClaw integration)

**Status:** Pending
**Issue:** casehubio/life#8
**Navigation:** `git log --grep="#8" --oneline` (fill in at layer close)

### What it shows

Integrates casehub-openclaw as the WorkerProvisioner. OpenClaw instances execute household
skills: banking API aggregation (Open Banking skills), Google Calendar integration (calendar
skill), Home Assistant smart home control (IoT skill), WhatsApp/SMS follow-up (messaging skills).

The ChannelContextWindow ensures each OpenClaw agent wakes with fresh context from Qhorus
channels — grocery-agent sees finance-agent's budget warning before placing an order;
health-agent sees smart home movement data before sending a medication reminder.

This layer demonstrates the bidirectional integration: CaseHub orchestrates via direct call
to /hooks/agent; OpenClaw heartbeat monitors ambient conditions and creates CaseHub WorkItems
when conditions warrant. Neither replaces the other.
