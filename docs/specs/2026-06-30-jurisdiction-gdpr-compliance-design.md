# Per-Action Jurisdiction, GDPR Compliance Report, CaseService Refactor

**Date:** 2026-06-30
**Issues:** life#48, life#50, life#51, life#49
**Branch:** issue-48-jurisdiction-gdpr-refine

---

## Problem Statement

Four related improvements to casehub-life's legal compliance and architectural quality:

1. **#48** — `LegalActionLedgerEntry.jurisdiction` is populated from tenant-wide config (`casehub.life.jurisdiction=GB`). Cross-jurisdiction actions (UK tax filing vs US immigration for a GB tenant) get the wrong jurisdiction recorded in the tamper-evident audit trail.

2. **#51** — `LifeCaseService.resolve()` uses a 6-arm switch on `LifeCaseType`, violating protocol PP-20260609-bd9d27 (no domain switches in service classes). Adding a new case type requires a switch arm in the service.

3. **#50** — The GDPR erasure endpoint (`DELETE /external-actors/{id}/personal-data`) returns 204 with no body. `ExternalActorErasureLedgerEntry` stores `memoryRecordsErased` but nothing surfaces it to the caller.

4. **#49** — `ExternalActorService.erase()` nullifies PII and erases memory but never calls `LedgerErasureService.erase()` to tokenise actor IDs in existing ledger entries. devtown's `GdprErasureService` integrates both; casehub-life does not.

## Implementation Order

`#48 → #50 → #51 → #49`. The GDPR issues (#50, #49) share data model changes; #51 is independent. #49 is last because it depends on #50's `ErasureResponse` design and introduces test config changes that affect other assertions.

---

## Design

### 1. Data Model Changes

**`CreateLifeTaskRequest`** (api/) — add optional `jurisdiction`:

```java
public record CreateLifeTaskRequest(
    @NotBlank String templateRef,
    @NotBlank String title,
    UUID externalActorId,
    Instant deadline,
    String jurisdiction  // optional — ISO 3166 code, e.g. "GB", "US-CA"
) {}
```

**`LifeTaskContext`** (app/) — add column:

```java
@Column(length = 10)
public String jurisdiction;
```

**`LifeTaskContextResponse`** (api/) — add `String jurisdiction` field.

**`ErasureResponse`** (api/) — new record in `io.casehub.life.api.response`:

```java
public record ErasureResponse(
    UUID erasedActorId,
    Instant erasedAt,
    int memoryRecordsErased,
    long ledgerEntriesAffected
) {}
```

**Migration V110:** `ALTER TABLE life_task_context ADD COLUMN jurisdiction VARCHAR(10)` — nullable, no default. The tenant-wide config (`casehub.life.jurisdiction`) remains the fallback in code.

### 2. LifeCaseService Switch Elimination (#51)

Replace 6 individual `@Inject` fields and the `resolve()` switch with CDI Instance lookup:

```java
@Inject @Any
Instance<LifeTypedCaseHub> caseHubs;

private CaseHub resolve(LifeCaseType type) {
    return caseHubs.stream()
            .filter(hub -> hub.lifeCaseType() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                    "No CaseHub registered for type: " + type));
}
```

CareEpisodeCaseHub and FamilyVoteCaseHub extend `YamlCaseHub` directly (not `LifeTypedCaseHub`), so they are correctly excluded — they are sub-case-only, never started via `LifeCaseService`.

Satisfies protocol PP-20260609-bd9d27: zero switch/if-else on `LifeCaseType` in service classes.

### 3. Jurisdiction Flow (#48)

**`LifeTaskService.create()`** — pass jurisdiction to supplement:

```java
ctx.jurisdiction = req.jurisdiction();
```

No validation — free-form string. Null means "use tenant-wide default."

**`LegalDomainLedgerHandler.writeEntry()`** — prefer task-level, fall back to config:

```java
entry.jurisdiction = ctx.jurisdiction != null ? ctx.jurisdiction : jurisdiction;
```

The `@ConfigProperty(name = "casehub.life.jurisdiction", defaultValue = "GB")` field stays as the default. Non-legal domain handlers are unaffected — jurisdiction is a legal concern only.

### 4. Compliance Report (#50)

**`ExternalActorResource.erasePersonalData()`** — change from 204 to 200 with `ErasureResponse`:

```java
ErasureResponse result = gdprErasureService.erase(id, currentPrincipal.actorId());
return Response.ok(result).build();
```

The resource injects `LifeGdprErasureService` directly for the erasure path. `ExternalActorService.erase()` is removed — it's not a CRUD operation.

### 5. LedgerErasureService Integration (#49)

**Extract `LifeGdprErasureService`** — new `@ApplicationScoped` service in `app/service/`.

Erasure pipeline (all within one `@Transactional` boundary):

1. Validate: actor exists, not already erased, no active tasks
2. PII nullification: `actor.name = "[ERASED]"`, `actor.contactValue = "[ERASED]"`, `actor.gdprErasedAt = Instant.now()`
3. Memory erasure: `memoryStore.eraseEntity(LifeActorIds.of(id), tenancyId)` — `MemoryCapabilityException` caught → 0
4. **Ledger tokenisation:** `ledgerErasureService.erase(LifeActorIds.of(id), ErasureReason.GDPR_ART_17_REQUEST)` — tokenises all `actorId` fields in existing ledger entries matching `life-actor:{uuid}`
5. Write `ExternalActorErasureLedgerEntry` via `LifeLedgerWriter`
6. Return `ErasureResponse(id, erasedAt, memoryRecordsErased, erasureResult.affectedEntryCount())`

**Transaction design:** `LifeGdprErasureService.erase()` is `@Transactional`. `LedgerErasureService.erase()` is also `@Transactional(REQUIRED)` — joins the outer transaction. All-or-nothing: if any step fails, the entire erasure rolls back.

**Test config changes:**

```properties
casehub.ledger.identity.tokenisation.enabled=true
casehub.ledger.erasure-receipt.enabled=true
```

**Impact (GE-20260531-46f8ab):** Enabling tokenisation globally means `actorId` values become opaque tokens at persist time. Tests asserting on raw `actorId` strings need updating: assert `isNotNull()` or use `ActorIdentityProvider.tokeniseForQuery()` to resolve expected tokens.

**Post-tokenisation constraint (GE-20260628-6599e6):** After erasure, the original `actorId` is severed from ledger entries. `ErasureResponse.ledgerEntriesAffected` is captured at erasure time from `ErasureResult.affectedEntryCount()` — cannot be reconstructed post-hoc. This is correct: the count is evidence recorded when it happened.

---

## Files Changed

### api/
- `CreateLifeTaskRequest.java` — add `jurisdiction` field
- `LifeTaskContextResponse.java` — add `jurisdiction` field
- `response/ErasureResponse.java` — new record

### app/
- `entity/LifeTaskContext.java` — add `jurisdiction` column
- `service/LifeTaskService.java` — pass `jurisdiction` to supplement
- `service/ledger/LegalDomainLedgerHandler.java` — prefer `ctx.jurisdiction` over config
- `engine/LifeCaseService.java` — replace 6 injects + switch with `Instance<LifeTypedCaseHub>`
- `service/LifeGdprErasureService.java` — new: orchestrates full GDPR erasure pipeline
- `service/ExternalActorService.java` — remove `erase()` method
- `resource/ExternalActorResource.java` — inject `LifeGdprErasureService`, return 200 + `ErasureResponse`
- `db/life/migration/V110__life_task_context_jurisdiction.sql` — add column

### test/
- `ExternalActorGdprResourceTest.java` — update for 200 response, add tokenisation assertions
- `LifeCaseServiceTest.java` or inline in existing test — verify Instance-based resolution
- `LegalDomainLedgerHandlerTest.java` — verify jurisdiction fallback logic
- `LifeGdprErasureServiceTest.java` — new: unit test for erasure pipeline
- `application.properties` (test) — add tokenisation and erasure-receipt config

---

## Protocols Verified

| Protocol | Status |
|----------|--------|
| PP-20260609-bd9d27 (no domain switches) | #51 eliminates last switch on LifeCaseType |
| PP-20260527-da1f66 (domain supplement) | Jurisdiction on LifeTaskContext, not WorkItem |
| PP-20260526-d0b921 (REST resources) | Resource stays @Blocking @ApplicationScoped |
| PP-20260526-75d9c9 (@Transactional on service) | LifeGdprErasureService owns the transaction |
| GE-20260531-46f8ab (tokenisation test flag) | Enabled in test config |
| GE-20260628-6599e6 (post-erasure query) | ErasureResponse captures count at erasure time |
