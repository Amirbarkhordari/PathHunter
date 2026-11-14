# PathHunter development history

Chronological record of PathHunter’s establishment as an independently identified project. Dates use the local Phase work calendar.

## Phase 0 — Initial audit (2026-11-13)

- Cloned `Amirbarkhordari/PathHunter` (then a GitHub fork of `ewilded/psychoPATH`).
- Confirmed HEAD `e29eb6b` matched upstream history at clone time.
- Documented architecture, features, limitations, environment, and build approach.
- Rebuilt sources with `javac --release 8` against Burp Suite Professional 2026.4.3.
- Created in-process Smoke harness (no external HTTP targets).
- **No commit/push** in Phase 0.

## Phase 0.1 — Burp loading compatibility (2026-11-13)

- Diagnosed Extender load failure: modern Burp parent-first `burp.*` loading / allowlist behaviour rejected project classes in `package burp`.
- Relocated implementation classes to `package uk.co.pentest.psychoPATH` under `Java/src/uk/co/pentest/`.
- Entry class: `uk.co.pentest.psychoPATH.BurpExtender`.
- User-facing names set to PathHunter; artifact `dist/PathHunter.jar`.
- Verified with LoadCheck, ProveBlock (`burp.Zv_d`), ScanLikeBurp, Smoke.
- Documented in `docs/burp-loading-fix.md`.
- **No commit/push** in Phase 0.1.

## Phase 1 — Independent project identity (2026-11-13)

- Identity audit classifying branding vs attribution vs technical identifiers.
- Professional README, NOTICE, docs updates.
- Branding strings for menu and scanner issue titles.
- Fork-detachment guidance only; Leave fork network **not** executed.
- **No commit/push** in Phase 1.

## Phase 1.1 — GitHub repository finalization (2026-11-13)

- Updated GitHub repository **description** and **topics** via authenticated REST API.
- Attempted GraphQL `detachFork`: **field does not exist** on `Mutation` for this API. Leave fork network was completed later by the owner via the Settings UI.
- Clean rebuild; LoadCheck 11/11, ProveBlock Pass, ScanLikeBurp 1 candidate, Smoke 29/29.
- Added CHANGELOG, development history, `.gitignore`, `tools/smoke/` harness sources.
- Committed Phase 0–1.1 work and pushed to `origin` (`https://github.com/Amirbarkhordari/PathHunter.git`).
- Upstream remote retained locally for historical fetch.

## Phase 1.2 — Independent branding presentation (2026-11-13)

- README presents PathHunter as the primary public identity; replaced prominent original-project attribution block with concise **License and Legal Information**.
- Softened GitHub description and `BappDescription.html` marketing text; legal provenance remains in `NOTICE`.
- Verified GitHub `fork: false` (standalone repository).
- Updated identity audit, project overview, changelog, and development history.
- No Java package/class renames; no license change.

## Phase 2.1 — Absolute Path Payload Engine (2026-11-13)

- Added optional absolute filesystem path generation (`/etc/passwd`, Windows absolute paths, custom list).
- New Absolute paths UI tab; additive Intruder Path traversal payloads; encoding via existing tables.
- AbsolutePathSmoke 33/33; Smoke 29/29; LoadCheck / ProveBlock / ScanLikeBurp regression passed.
- Documented PortSwigger Academy manual validation; lab not executed here.
- Details: `docs/phases/phase-2.1-absolute-path-engine.md`.

## License continuity

Throughout all phases: GPL-3.0 (`LICENSE`), original authorship attribution preserved in `NOTICE` and inherited source headers. Public marketing text does not replace those legal obligations.
