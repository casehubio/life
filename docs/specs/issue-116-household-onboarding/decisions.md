# Decisions — Household Onboarding (#116)

## D1: OIDC approach

**Choice:** Keycloak Dev Services + Admin API (real OIDC in dev mode)
**Alternatives:**
- Self-contained demo onboarding (store members in life DB, bypass OIDC) — simpler but diverges dev from prod; every test uses fake auth shims
**Rationale:** Real OIDC tokens mean OidcCurrentPrincipal works natively. @RolesAllowed, visibility policies, tenant isolation — same code path in dev and production. Keycloak Dev Services auto-starts a container, zero external infrastructure.
**Trade-offs:** Requires Docker for dev mode (Keycloak container). Slightly slower startup (~5s).
**Exploration:** deep-analysis
**Status:** captured

## D2: Realm config approach

**Choice:** Declarative realm-config.json imported by Keycloak Dev Services
**Alternatives:**
- Dynamic realm creation via Admin API — more code, realm config lives in Java instead of a declarative file
**Rationale:** Roles and client config are static (don't change per household). Only users are dynamic. quarkus.keycloak.devservices.realm-path supports importing a realm JSON file. Less code, less to go wrong.
**Trade-offs:** Changes to roles require updating the JSON and restarting dev services.
**Depends on:** D1 (Keycloak Dev Services)
**Exploration:** quick
**Status:** captured

## D3: Household-tenancy relationship

**Choice:** Household IS the tenancy — Household.id = tenancyId, one-to-one
**Alternatives:**
- Separate household from tenancy (household belongs to tenancy, tenancy could contain multiple households) — more flexible but adds indirection nobody needs
**Rationale:** YAGNI. Current usage is one household per tenancy. If multi-household ever matters, it's a migration not a rewrite. Priority is ease of setup.
**Trade-offs:** Cannot support multiple households per tenancy without migration.
**Exploration:** quick
**Status:** captured

## D4: Voice enrollment scope

**Choice:** Stub SPI with NoOp @DefaultBean — placeholder for #120
**Alternatives:**
- Full voice enrollment in this branch — blocks onboarding on pages avatar integration
- Enrollment UI only (record + store, no identity resolution) — partial value, still needs #120
**Rationale:** Voice enrollment depends on pages avatar component. Shipping onboarding shouldn't be blocked by it. SPI stub means #120 just provides the implementation — no onboarding changes needed.
**Trade-offs:** Voice enrollment not available at launch. Users must re-run enrollment step when #120 lands.
**Depends on:** D1 (onboarding flow exists)
**Exploration:** quick
**Status:** captured

## D5: Member relationships

**Choice:** Explicit fields for permission-relevant relationships (PARENT/CHILD/SPOUSE/GUARDIAN), mind map for everything else
**Alternatives:**
- All relationships in mind map — clean but permission logic can't query a graph store for authorization decisions
- All relationships as entity fields — bloated entity, domain knowledge that belongs in the knowledge graph
**Rationale:** Permission-relevant relationships are structural (parent→child = admin→junior authority). Everything else (who picks up Ella, who is Jean's primary contact) is knowledge that belongs in the neocortex mind map (#121).
**Trade-offs:** Two places to look for relationship data. Mitigation: the mind map is enrichment, not authoritative for permissions.
**Depends on:** D3 (household entity exists)
**Exploration:** quick
**Status:** captured
