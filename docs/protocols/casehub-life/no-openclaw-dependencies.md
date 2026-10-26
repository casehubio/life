---
id: PP-20261026-8a9dce
title: "No OpenClaw dependencies — use platform agent routing"
type: rule
scope: repo
applies_to: "casehub-life — all modules, dependency changes, worker wiring"
severity: critical
refs:
  - docs/specs/digital-life-assistant-roadmap/2026-10-26-digital-life-assistant-roadmap.md
violation_hint: "Any import from io.casehub.openclaw.*, casehub-openclaw-* in pom.xml, or OpenClaw config keys in application.properties"
created: 2026-10-26
supersedes: PP-20260618-openclaw-agent
---

Do not add casehub-openclaw-core or casehub-openclaw-casehub as dependencies.
Platform RoutingChatModel + manifests replace the LLM bridge, connector platform
SPIs replace the skill ecosystem, and eidos AgentDescriptor replaces agent identity.
Workers use LifeAgentProviderFactory (backed by platform AgentProvider). OpenClaw
was removed in life#138 (cb492b6, 2026-10-26). The old openclaw-agent-worker-pattern
protocol is superseded.
