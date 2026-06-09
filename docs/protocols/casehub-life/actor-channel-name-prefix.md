---
id: PP-20260609-982617
title: "Prefix actor channel name segments — never use a raw UUID"
type: rule
scope: repo
applies_to: "Any code in app/ that constructs a qhorus channel name using an ExternalActor UUID or any other UUID"
severity: critical
refs:
  - app/src/main/java/io/casehub/life/app/infrastructure/LifeChannelInitializer.java
  - app/src/main/java/io/casehub/life/app/commitment/ContractorCommitmentStrategy.java
violation_hint: "Channel registration returns 500 with 'Invalid channel name segment — must match [a-z][a-z0-9]*(-[a-z0-9]+)*'; test failure is random (passes ~37% of runs when UUID starts with a-f)"
garden_ref: GE-20260607-a4d78a
created: 2026-06-09
---

Never pass a raw UUID as a qhorus channel name segment. `ChannelSlugValidator` requires each path segment to start with `[a-z]`; UUID hex strings start with a digit (0-9) in 62.5% of cases, causing a 500. Always prefix with a descriptive label that starts with a letter. For ExternalActor channels the convention is `ext-{uuid}`: `"life/actor/ext-" + externalActorId`.
