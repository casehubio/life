# casehub-life: Actor Model Spec

**Status:** Design reference  
**Scope:** casehub-life actor taxonomy — AI agents, household principals, external human actors  
**Audience:** Contributors implementing the life module and integrating with casehub-qhorus, casehub-engine, and Claudony

---

## 1. Three Actor Types

### Type 1 — AI Agents

AI agents are named personas operating as CaseHub workers. Each agent is an OpenClaw instance configured with a specific household role.

**Named personas:**
- `home-agent` — household task coordination, contractor management, energy monitoring
- `health-agent` — medication schedules, appointment follow-up, referral chains
- `finance-agent` — financial aggregation, spend oversight, budget gate management
- `travel-agent` — trip planning, booking coordination, itinerary consolidation

Additional personas may be defined per household configuration (e.g., `care-agent` for elder care coordination).

**Identity format:** `{model-family}:{persona}@{major}`

Examples:
- `claude:home-agent@3`
- `gpt:finance-agent@4`
- `claude:health-agent@3`

The `@{major}` component refers to the major model version, not a semantic version of the persona. This allows trust scoring to track model capability changes over time.

**Trust scoring:** AI agents carry trust scores on their CaseHub identity. A degraded trust score constrains what the agent can authorise without human principal confirmation. Specifically:
- An agent with a trust score below threshold cannot issue a COMMAND that creates a financial Commitment above a configured spend limit
- An agent with a degraded score cannot approve a medication change WorkItem
- Trust score degradation triggers automatic escalation to oversight channel

**Agent lifecycle:** agents are registered as workers in casehub-engine. Their WorkItem assignability is scoped by their trust score and the RBAC roles of the channel they write to.

---

### Type 2 — Household Principals

Household principals are authenticated human members of the household. Authentication is via Claudony (WebAuthn, passkey, or X-Api-Key bridge from OpenClaw).

**Role hierarchy:**

| Role | Description | Capabilities |
|------|-------------|--------------|
| `household-admin` | Adult with full household governance authority | COMMAND, RESPONSE, QUERY, oversight gate approval, quorum participant |
| `household-member` | Adult household member | COMMAND (within allowed decision categories), RESPONSE, QUERY |
| `household-junior` | Child or restricted member | RESPONSE (to assigned tasks only), QUERY (own tasks only) |

Role hierarchy is strictly enforced: `household-admin` > `household-member` > `household-junior`. Roles do not inherit upward — a `household-junior` cannot acquire `household-member` capabilities through any delegation mechanism.

**Authentication:** each principal authenticates via Claudony or the OpenClaw bridge. The authenticated identity maps to a CaseHub principal, which carries the role assignment.

**RBAC mapping:** roles map directly to:
- Qhorus `allowed_writers` on channels (which principals can post to which channels)
- WorkItem assignees (which principals can be assigned tasks)
- Oversight gate responders (which principals can approve gated decisions)

**Quorum:** multi-party decisions use the existing `MultiInstanceCoordinator` with M-of-N configuration. A dual-party decision (e.g., major purchase) requires COMMAND from two `household-admin` or `household-member` principals before a Commitment is created.

---

### Type 3 — External Human Actors

External human actors are people who hold obligations in the household's life but have no CaseHub account and no ability to interact with CaseHub directly. This is a structural gap in the current CaseHub model — no existing entity type covers this actor class.

**Examples:** plumber, electrician, GP, specialist, solicitor, school, utility provider, landlord.

**Characteristics:**
- External obligors: they commit to actions (arriving at a time, filing a document, sending a referral)
- No CaseHub account: they cannot directly update WorkItems or confirm completion
- Multi-channel reachable: they can be contacted via WhatsApp, SMS, email, phone — all via OpenClaw skills
- Proxy-tracked: their compliance is observed through household principal confirmation or agent-mediated contact attempts

**Design options (not yet resolved):**
- Option A: `ExternalActor` entity in casehub-qhorus-api — a lightweight record capturing name, contact details, and role; referenceable as an obligor on a Commitment without authentication
- Option B: life-specific config entity in casehub-life — a `ServiceProvider` or `Contractor` record that maps to a Commitment obligor field

The external actor gap is known and not yet resolved at the platform level. The concrete interaction pattern is fully designed (see §2); the data model is pending.

---

## 2. External Human Actor Commitment Pattern

This is a concrete workflow illustrating how a contractor commitment is tracked end-to-end without the contractor having a CaseHub account.

### Scenario: Plumber commits to Thursday 10am–12pm

**Step 1 — Commitment capture**

User tells `home-agent`: "The plumber committed to Thursday 10am to 12pm."

`home-agent` creates:
- A Commitment with the plumber as the named obligor (external, no account)
- Obligor fields: name, contact method (WhatsApp number or phone), committed window (Thursday 10am–12pm)
- Commitment status: PENDING

**Step 2 — Watchdog set**

Watchdog is configured for Thursday 12pm (the end of the committed window).

**Step 3 — Watchdog fires**

Thursday 12pm arrives. No DONE confirmation has been received (no household principal has marked the commitment fulfilled, and no agent has confirmed completion).

Watchdog fires → WorkItem created: "Plumber commitment window elapsed — no confirmation received."

**Step 4 — Agent-mediated contact**

`home-agent` uses OpenClaw messaging skill to send WhatsApp/SMS to the plumber's contact number:

> "Hi, we had you booked for 10am–12pm today. Can you confirm your ETA or let us know if you need to reschedule?"

This is an OpenClaw execution action, not a CaseHub action. The message is logged as a WorkItem comment.

**Step 5 — Response window**

A configurable N-hour response window opens (default: 2 hours).

**Step 6a — Response received**

Plumber replies (via WhatsApp, relayed by OpenClaw messaging skill). `home-agent` receives the response, parses the ETA or rescheduled time, updates the WorkItem with the new commitment window, and resets the Watchdog.

**Step 6b — No response**

N hours pass with no response. WorkItem escalates to `household-admin` oversight channel:

> "Plumber no-show: contact attempt sent at [time], no response after 2 hours. Action required."

`household-admin` receives the WorkItem and takes manual action (rebook, find alternative, or mark dispute).

**Step 7 — Resolution**

Whichever path resolves the situation, the household principal closes the WorkItem with outcome: FULFILLED, RESCHEDULED, or FAILED.

**What CaseHub contributes:**
- Commitment with named obligor and committed window
- Watchdog with automatic escalation trigger
- Full chain tracked as a case — contact attempts, response (or non-response), escalation, resolution
- At Level 4: tamper-evident record of the entire commitment lifecycle

---

## 3. Household Permission Topology

### Decision Categories

Not all household decisions are equivalent. The permission model recognises four categories:

| Category | Examples | Required Principals |
|----------|----------|---------------------|
| Single-party | Grocery order, school pickup, routine errand | Any `household-member` or above |
| Dual-party | Holiday booking, major purchase, contractor engagement above threshold | M-of-N from `household-admin` pool |
| Individual | Personal health decisions, work schedule, individual financial matters | The individual principal only |
| Junior-accessible | Responding to assigned tasks, querying own task list | `household-junior` (RESPONSE and QUERY only) |

Junior-accessible tasks are explicitly limited to RESPONSE and QUERY. A `household-junior` cannot issue COMMAND in any category.

### Existing Machinery

The casehub-life permission topology uses existing platform capabilities without new primitives:

- **MultiInstanceCoordinator (M-of-N):** dual-party decisions use the existing multi-instance mechanism configured with N=2 (or configurable threshold). No new quorum mechanism needed.
- **RBAC roles:** `household-admin`, `household-member`, `household-junior` map to existing `@RolesAllowed` and `CurrentPrincipal.roles()` patterns
- **Qhorus `allowed_writers`:** channels are configured with allowed writer sets that enforce which principals can post. The oversight channel allows only `household-admin` writers.
- **Engine conditional branching:** `household-junior` tasks branch to a restricted workflow path at case creation

### Open Question — Quorum Configuration

How is the quorum configuration for a dual-party decision expressed? Three options under consideration:

1. **CasePlanModel property:** the plan definition declares `quorum: 2` for specific tasks — quorum is a property of the workflow template
2. **Channel property:** the oversight channel carries a `required_writers: 2` property — any WorkItem routed to that channel requires dual approval
3. **Life-specific config entity:** casehub-life defines a `HouseholdDecisionPolicy` entity that maps decision categories to quorum requirements — more flexible but adds a new entity type

Not yet resolved. The existing MultiInstanceCoordinator supports all three approaches; the question is where the configuration lives.

---

## 4. Privacy Partitioning Across Life Domains

### Domain Isolation Requirements

The casehub-life privacy model requires stronger isolation than ACL-based access control. The requirement is domain-level isolation — entire categories of data must not be accessible across domain boundaries, regardless of principal permissions.

**The four non-negotiable isolation boundaries:**

| Source Domain | Must Not Bleed To |
|---------------|-------------------|
| Health data | Finance agents, household agents, work agents |
| Financial data | Household agents accessible to children (`household-junior` reachable agents) |
| Work/professional data | Household agents |
| Elder care data | Finance agents, household agents outside care coordination scope |

### Why ACL Is Insufficient

An ACL says "this principal can read this data." Domain isolation says "this type of data cannot be queried by this class of agent, regardless of principal." A finance-agent operating on behalf of a `household-admin` principal should not be able to retrieve health facts, even if the `household-admin` principal has read access to health data. The agent's domain constrains what it can access, not just the principal's role.

This distinction matters because:
- Agents are long-lived workers with persistent query access to CaseMemoryStore
- A compromised or misbehaving agent could exfiltrate data outside its legitimate scope
- Children using junior-accessible household agents should not be able to trigger queries that surface financial data even indirectly

### CaseMemoryStore Domain Boundaries

CaseMemoryStore (see platform spec) must respect domain boundaries. The SPI layer enforces this — domain isolation is not delegated to the memory backend. Concretely:

- A query from `finance-agent` must not return facts tagged with `domain: health`
- A query from any agent operating in a `household-junior`-accessible context must not return facts tagged with `domain: financial`
- Elder care facts are queryable only by agents operating in the `care` domain

Domain tags are set at fact emission time (when a fact is stored in CaseMemoryStore). The domain tag is immutable after storage.

### Open Question — Isolation Implementation

Is domain isolation a configuration of the permission model or a structural property of the casehub-life domain model?

**Option A — Permission model configuration:** domain isolation is expressed as permission rules in Claudony/Qhorus — "agents with persona X cannot query facts with domain tag Y." Flexible, but the permission model must be configured correctly per deployment.

**Option B — Structural property:** casehub-life defines domain types as first-class entities, and CaseMemoryStore SPI accepts a `Domain` parameter on every query. Agents are registered with a domain affinity; cross-domain queries are structurally prevented, not just policy-prevented.

Not yet resolved. Option B is structurally safer; Option A is more consistent with the existing permission model.

---

## 5. RBAC / ACL Three-Layer Enforcement

The casehub-life permission model operates at three distinct layers. All three must pass for an action to proceed. Passing one layer does not grant access to subsequent layers.

### Layer 1 — Authentication (Claudony)

**Question answered:** who is this principal?

**Mechanisms:**
- WebAuthn / passkey for household principals via Claudony
- X-Api-Key bridge for OpenClaw agents calling CaseHub APIs
- `household-junior` principals authenticate with the same mechanism but receive a role-constrained session token

**Output:** authenticated principal identity with roles attached.

### Layer 2 — RBAC (casehub-engine / casehub-work)

**Question answered:** what is this principal allowed to do?

**Mechanisms:**
- `@RolesAllowed` on WorkItem operations (create, assign, close, escalate)
- `CurrentPrincipal.roles()` checked at operation time
- Role hierarchy enforced: `household-junior` role does not include `household-member` permissions

**Output:** permitted operation set for this principal.

### Layer 3 — Channel ACL (Qhorus allowed_writers)

**Question answered:** can this principal write to this specific channel?

**Mechanisms:**
- `allowed_writers` list on each Qhorus channel
- Oversight channel: `allowed_writers = [household-admin]` — COMMAND from a `household-member` is silently dropped
- Health channel: `allowed_writers = [household-admin, health-agent]` — `household-member` cannot post without explicit channel inclusion

**Output:** write permission on this specific channel granted or denied.

### Key Deontic Consequence

A COMMAND that does not pass all three layers never creates a Commitment.

This is the deontic foundation of the system: obligation is only created when the principal has the authority to create it. A COMMAND posted to a channel the principal cannot write to is not an obligation — it is noise. No Watchdog is set. No obligor is named. No case is created.

### HANDOFF and Permission Laundering

A HANDOFF message (handing a case from one agent to another) cannot launder permission escalation. If agent A hands off a case to agent B, agent B must independently satisfy all three layers at every subsequent operation. A HANDOFF does not carry the originating principal's permissions forward.

This prevents the following attack: a low-trust agent issues a COMMAND it is not authorised to issue by handing off to a high-trust agent and relying on the higher trust to execute it. The receiving agent's own trust score and the principal's own permissions must be satisfied at the point of action.

---

## Open Questions

1. **External actor entity:** `ExternalActor` in casehub-qhorus-api or life-specific config entity? Not yet resolved.

2. **Quorum configuration:** CasePlanModel property, channel property, or `HouseholdDecisionPolicy` entity? Not yet resolved.

3. **Domain isolation implementation:** permission model configuration (Option A) or structural property of casehub-life domain model (Option B)? Not yet resolved. Option B is structurally safer.

4. **CaseMemoryStore domain query parameter:** if domain isolation is structural (Option B), the SPI `query()` method must accept a `Domain` parameter. This has implications for the CaseMemoryStore SPI design — see platform spec.
