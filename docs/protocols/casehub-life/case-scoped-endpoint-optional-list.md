---
id: PP-20260810-f66e2a
title: "Case-scoped endpoints return Optional<List<T>> — 404 for unknown case, 200 empty for no data"
type: rule
scope: application
applies_to: "all GET /life-cases/{id}/{resource} endpoints"
severity: important
refs:
  - app/src/main/java/io/casehub/life/app/service/LifeCaseQueryService.java
  - app/src/main/java/io/casehub/life/app/resource/LifeCaseResource.java
violation_hint: "Endpoint returns 200 with null body for unknown case, or 404 for case with no data"
created: 2026-08-10
---

Case-scoped query services return `Optional<List<ResponseType>>`. The Optional
encodes case existence (empty → 404); the List encodes data presence (empty list → 200).
Service method pattern: look up LifeCaseTracker by ID (null → `Optional.empty()`),
check engineCaseId (null → `Optional.of(List.of())`), query scoped data,
return `Optional.of(results)`. The resource delegates to
`.map(r -> Response.ok(r).build()).orElse(Response.status(NOT_FOUND).build())`.
