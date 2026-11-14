# Changelog

All notable changes to PathHunter are documented in this file.

## [Unreleased]

### Planned

- Further path-traversal engine enhancements beyond absolute paths (see README roadmap)

## [0.2.0] – 2026-11-13

### Added

- **Absolute Path Payload Generation** for the Path Traversal Intruder generator.
- New **Absolute paths** configuration tab: enable/disable, Linux / Windows / Custom targets, editable path list, validation status.
- `AbsolutePathEngine` helper for OS-specific validation, encoding, and deduplication.
- Payload strategy tags: `STANDARD_TRAVERSAL`, `ABSOLUTE_LINUX`, `ABSOLUTE_WINDOWS`, `ABSOLUTE_CUSTOM`.
- `tools/smoke/AbsolutePathSmoke.java` (33 automated checks).
- Phase write-up: `docs/phases/phase-2.1-absolute-path-engine.md`.

### Changed

- Absolute path payloads are additive and **disabled by default** so existing traversal/LFI sets are unchanged until enabled.
- Absolute paths use the existing encoding tables once per enabled encoding (raw / URL / double-URL / UTF variants).

### Tests

| Check | Result |
| --- | --- |
| AbsolutePathSmoke | 33/33 |
| Smoke | 29/29 |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

PortSwigger Academy lab: documented manual procedure only; not executed in this environment.

## [0.1.1] – 2026-11-13

### Changed

- **Phase 1.2 branding presentation:** README focuses on PathHunter; replaced the prominent original-project attribution section with concise **License and Legal Information**.
- Softened GitHub repository description and `BappDescription.html` so marketing text does not advertise the prior project name.
- Legal provenance remains in `NOTICE`, `LICENSE`, and inherited source headers (unchanged obligations).

### Fixed / verified

- Confirmed GitHub repository is standalone (`fork: false`) after Leave fork network.

## [0.1.0] – 2026-11-13

PathHunter independent identity and modern Burp Suite loading compatibility. Derivative of psychoPATH (GPL-3.0) by Julian Horoszkiewicz.

### Fixed

- **Burp classloader compatibility:** Implementation classes no longer live in `package burp`. Modern Burp Suite parent-first loading of `burp.*` no longer treats the extension entry class as an unrecognized type.
- Entry point is now `uk.co.pentest.psychoPATH.BurpExtender`.

### Changed

- User-facing branding: extension name, suite tab, context menu, and scanner issue title use **PathHunter**.
- Distribution artifact renamed to `dist/PathHunter.jar` (historical `Java/psychoPATH.jar` left untouched).
- Professional README, NOTICE, and documentation under `docs/`.
- GitHub repository description and topics updated for PathHunter.
- Automated harness sources placed under `tools/smoke/`.

### Added

- `NOTICE` — derivative attribution (psychoPATH → PathHunter).
- `docs/identity-audit.md`, `docs/github-fork-detachment.md`, `docs/development-history.md`.
- `.gitignore` for build intermediates.
- LoadCheck / ProveBlock / ScanLikeBurp / Smoke harnesses for regression.

### GitHub / repository

- Description and topics configured on `Amirbarkhordari/PathHunter`.
- **Leave fork network:** not completed via API (`detachFork` mutation unavailable). Manual Settings → Danger Zone steps remain required. See `docs/github-fork-detachment.md`.

### Test results (2026-11-13)

| Check | Result |
| --- | --- |
| `javac --release 8` | Pass |
| JAR integrity / entry / no bundled Burp API / major 52 | Pass |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate (`uk.co.pentest.psychoPATH.BurpExtender`) |
| Smoke | 29/29 |
| Burp GUI load | Not run in automation |

### Attribution

Original project: [psychoPATH](https://github.com/ewilded/psychoPATH) by Julian Horoszkiewicz. License: GPL-3.0.
