# Squash Plan — issue-40-wire-platform-oidc → main

**Range:** origin/main..HEAD  
**Before:** 9 commits  
**After:** 3 commits  
**Branch:** squash/wip-main-20260622  

---

## Already Clean — 0 commits

No commits that survive unchanged without changes.

---

## Group 1 — chore(#40): add casehub-platform-oidc dep + OIDC config
*1 commit → 1 (KEEP, no absorptions)*

✅ KEEP `42064e3` chore(#40): add casehub-platform-oidc dep + OIDC config for production, dev, and tests

> **Result:** 1 commit — unchanged.

---

## Group 2 — feat(#26): RBAC-differentiated thresholds in LifeActionRiskClassifier
*3 commits → 1*

**Final message:** `feat(#26): RBAC-differentiated thresholds in LifeActionRiskClassifier — admin elevated, junior always-gate, context-inactive member fallback (Closes #26)`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `c7dd3fc` feat(#26): add admin threshold PreferenceKeys and risk-policy.yaml entries | 🔀 MERGE ↑ | *(unified — prerequisite constants for the classifier; same scope/issue)* |
| `033b976` feat(#26): RBAC-differentiated thresholds in LifeActionRiskClassifier | ✅ KEEP | *(see Final message above)* |
| `d372fe8` test(#26): fix LifeActionRiskClassifierQuarkusTest — set member groups in @BeforeEach | 🔽 SQUASH ↑ | *(absorbed — test hardening for same feature)* |

> **Result:** 1 commit.

---

## Group 3 — feat(#40): wire @RolesAllowed on REST resources
*5 commits → 1*

**Final message:** `feat(#40): wire @RolesAllowed on all REST resources; add @TestSecurity; review fixes; doc sync (Closes #40)`

| Commit | Action | Curated result |
|--------|--------|----------------|
| `09999eb` feat(#40): add @RolesAllowed to all REST resources; add @TestSecurity | ✅ KEEP | *(see Final message above)* |
| `f60ad03` fix(#26): address code review — single groups() call, pre-computed admin flag | 🔽 SQUASH ↑ | *(absorbed — post-review fix touching @RolesAllowed resources and classifier)* |
| `c7e8e7d` docs(#40): sync CLAUDE.md and ARC42STORIES.MD | 🔽 SQUASH ↑ | *(absorbed — docs follow-on)* |
| `26fe3b9` docs(work-end): promote artifacts from workspace | 🔽 SQUASH ↑ | *(absorbed — housekeeping)* |
| `263ef1d` docs(#40): promote spec to project docs/specs/ | 🔽 SQUASH ↑ | *(absorbed — docs follow-on)* |

> **Result:** 1 commit.

---

## AFTER — what `git log --oneline` will show

  9  commits (original)
  -6  absorbed by squash
  ──────────────────────
  3  commits — no content lost

```
<sha>  feat(#40): wire @RolesAllowed on all REST resources; add @TestSecurity; review fixes; doc sync (Closes #40)
<sha>  feat(#26): RBAC-differentiated thresholds in LifeActionRiskClassifier — admin elevated, junior always-gate, context-inactive member fallback (Closes #26)
<sha>  chore(#40): add casehub-platform-oidc dep + OIDC config for production, dev, and tests
```
