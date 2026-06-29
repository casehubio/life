---
id: PP-20260615-8ed738
title: "RETIRED — CurrentPrincipal CDI disambiguation"
type: rule
scope: repo
status: retired
retired_date: 2026-06-29
retired_reason: "platform#112 shipped @Alternative @Priority resolution"
---

**Retired.** Since platform#112, `OidcCurrentPrincipal @Alternative @Priority(100)` wins in production and `FixedCurrentPrincipal @Alternative @Priority(200)` wins in tests. No exclude-types entries needed for CurrentPrincipal disambiguation.
