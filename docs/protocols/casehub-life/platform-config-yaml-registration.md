---
id: PP-20260607-4c59f4
title: "Register new YAML preference files in both main and test application.properties"
type: rule
scope: repo
applies_to: "Any new YAML file added to app/src/main/resources/casehub/life/"
severity: important
refs:
  - app/src/main/resources/application.properties
  - app/src/test/resources/application.properties
violation_hint: "New YAML config added but only registered in main application.properties — @QuarkusTest loads no thresholds from the new file, causing tests to use hardcoded defaults without surfacing an error."
created: 2026-06-07
---

Every YAML preference file placed under `app/src/main/resources/casehub/life/` must be added to the `casehub.platform.config.files` comma-separated list in **both** `app/src/main/resources/application.properties` and `app/src/test/resources/application.properties`. Registering in main only causes the file to be silently ignored in `@QuarkusTest` — the platform config provider loads files from the classpath list at startup; a missing entry produces no error, the preferences simply resolve to their coded defaults.
