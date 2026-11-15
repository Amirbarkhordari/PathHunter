# Changelog

All notable changes to PathHunter are documented in this file.

## [Unreleased]

### Planned

- Further path-traversal engine enhancements (see master documentation roadmap)
- Manual Burp / PortSwigger Academy validation of File Upload execution workflow

## [0.9.2] – 2026-11-15

### Fixed

- Managed Testing now auto-loads Sel-checked Filename/MIME/Content cases when the tab is opened (no silent empty queue).
- Replaced compressing `GridLayout` action strip with FlowLayout toolbars so Managed controls stay readable.
- Redesigned Managed Testing layout: readable authorization panel, horizontal split for case table vs detail tabs, per-engine queue summary.
- Selection sync uses stable test case IDs; collectSelectedCases deduplicates by ID across engines.

### Added

- Max body bytes field on Managed authorization panel.
- FileUploadWorkflowSmoke coverage for mixed-engine transfer, tab auto-load, layout preferred sizes, and load/prepare no-HTTP (60 pass / 2 skipped).

### Changed

- Managed action labels shortened (**Refresh Cases**, **Validate**, **Prepare**, **Execute**, …) with full tooltips.

## [0.9.1] – 2026-11-15

### Fixed

- File Upload Filename/MIME/Content plan tables no longer remain empty after analysis: auto-select first **file** part and auto-generate plans.
- Moved **Generate Test Plan** / MIME / Content Generate controls to a prominent toolbar above each plan table (were easy to miss under empty tables).
- Stopped silently remapping a selected non-file part to the primary file part during generation.
- Clearing stale plans when the selected file part changes; Managed load explains why 0 cases were loaded.
- Empty-plan and oversize/unsupported content reasons surfaced in status text.
- Results export handles missing analysis when evidence exists; Results filter no longer offers misleading `NOT_EXECUTED` evidence rows.

### Added

- `tools/smoke/FileUploadWorkflowSmoke.java` (31 pass / 2 skipped) covering Analyzer→plans→Managed→evidence workflows.

### Changed

- Updated File Upload notices to point at Managed Testing instead of “future phase” wording.

## [0.9.0] – 2026-11-14

### Added

- **Phase 4.5.2** Controlled HTTP Execution (`ControlledHttpExecutor`, `BurpHttpTransport`, `HttpServiceTarget`).
- Explicit authorization policy gates (disabled by default; host/port match; batch limits; inter-request delay; cancel; duplicate-prep block).
- **Phase 4.5.3** Response analysis (`ResponseAnalysisEngine`, baseline comparison, behavior indicators, confidence labels).
- **Phase 4.5.4** Evidence store, Results dashboard, JSON/HTML report export.
- Context-menu handoff retains `IHttpService` and baseline response bytes.
- `tools/smoke/ControlledExecutionSmoke.java` (51 pass / 2 skipped; mocked transport only).

### Changed

- File Upload Managed Testing GUI: Authorize / Execute / Results workflow.
- Master documentation updated for completed Phase 4.5 (no per-phase docs created).

### Security

- Preparation still never transmits HTTP.
- Execution requires Enable + Authorize Target + confirmation dialog.
- Sensitive headers redacted in previews/reports.
- Behavioral observations are not automatic vulnerability confirmations.

### Tests

| Check | Result |
| --- | --- |
| ControlledExecutionSmoke | 51/51 (+2 SKIP) |
| ManagedRequestBuilderSmoke | 73/73 (+2 SKIP) |
| FileContentValidationSmoke | 91/91 (+2 SKIP) |
| MimeValidationSmoke | 78/78 (+2 SKIP) |
| FilenameTestingSmoke | 80/80 (+2 SKIP) |
| FileUploadAnalyzerSmoke | 80/80 (+2 SKIP) |
| UrlEncodingSmoke | 31/31 |
| AbsolutePathSmoke | 33/33 |
| Smoke | 29/29 |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |
| JAR major version | 52 |
| Bundled `burp/` API classes | 0 |

## [0.8.1] – 2026-11-14

### Changed

- Consolidated project documentation into canonical [docs/PATHHUNTER_MASTER_DOCUMENTATION.md](docs/PATHHUNTER_MASTER_DOCUMENTATION.md).
- Slimmed README.md to user-facing summary with link to the master document.
- Removed obsolete per-phase Markdown files under `docs/phases/` after migrating content into the master document.
- Future phases update the master document and `CHANGELOG.md` only (no new phase-specific docs).

### Added

- `tools/docs/validate-master-docs.ps1` lightweight documentation validation script.

## [0.8.0] – 2026-11-14

### Added

- **Managed execution architecture** (`fileupload.execution`): lifecycle, validation, batch preparation, authorization policy (disabled by default).
- **MultipartRequestBuilder** with isolated filename / MIME / content mutations; original request preservation; SHA-256 integrity; request diff.
- Sensitive header redaction for previews (Cookie / Authorization / CSRF).
- File Upload → **Managed Testing** GUI (prepare/validate/preview only; no Execute / no HTTP).
- `tools/smoke/ManagedRequestBuilderSmoke.java` (73 pass / 2 skipped).
- See `docs/PATHHUNTER_MASTER_DOCUMENTATION.md` (phase details consolidated).

### Not in this release

- HTTP request transmission, uploads, response analysis, evidence reporting.

### Tests

| Check | Result |
| --- | --- |
| ManagedRequestBuilderSmoke | 73/73 (+2 SKIP) |
| FileContentValidationSmoke | 91/91 (+2 SKIP) |
| MimeValidationSmoke | 78/78 (+2 SKIP) |
| FilenameTestingSmoke | 80/80 (+2 SKIP) |
| FileUploadAnalyzerSmoke | 80/80 (+2 SKIP) |
| UrlEncodingSmoke | 31/31 |
| AbsolutePathSmoke | 33/33 |
| Smoke | 29/29 |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

## [0.7.0] – 2026-11-14

### Added

- **File Content Validation Engine** (`FileContentValidationEngine`, `FileContentAnalyzer`, `FileStructureValidator`, `FileContentHasher`, `ContentVariantGenerator`).
- Bounded PNG / JPEG / GIF / PDF structural checks; SHA-256 integrity; metadata consistency.
- Controlled in-memory content candidates (truncate / marker / signature / length / malformed metadata) with dedupe and resource limits.
- Extended `UploadTestCase` / `UploadTestPlan` with `CATEGORY_CONTENT`, `TARGET_PART_CONTENT_BYTES`, content hashes/lengths, `FileContentAnalysisResult`.
- File Upload → **Content Testing** GUI sub-tab (no Execute).
- `tools/smoke/FileContentValidationSmoke.java` (91 pass / 2 skipped).
- See `docs/PATHHUNTER_MASTER_DOCUMENTATION.md` (phase details consolidated).

### Not in this release

- Managed HTTP execution, automatic uploads, webshell/polyglot generation, full format decode, response-based verification.

### Tests

| Check | Result |
| --- | --- |
| FileContentValidationSmoke | 91/91 (+2 SKIP) |
| MimeValidationSmoke | 78/78 (+2 SKIP) |
| FilenameTestingSmoke | 80/80 (+2 SKIP) |
| FileUploadAnalyzerSmoke | 80/80 (+2 SKIP) |
| UrlEncodingSmoke | 31/31 |
| AbsolutePathSmoke | 33/33 |
| Smoke | 29/29 |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

PortSwigger file-upload labs: documented manual procedure only; not executed here.

## [0.6.0] – 2026-11-14

### Added

- **MIME Validation Testing Engine** (`MimeValidationEngine`, `MimeAnalyzer`, `MimeExtensionMapper`, `MimeSignatureComparator`, `MimeVariantGenerator`).
- MIME parse/normalize with raw preservation; extension↔MIME mapping; signature comparison statuses.
- Strategies: declared/case/parameter/missing-empty/mismatch/custom MIME candidates.
- Extended `UploadTestCase` with `CATEGORY_MIME`, `TARGET_PART_CONTENT_TYPE`, MIME fields.
- File Upload → **MIME Testing** GUI sub-tab (no Execute).
- `tools/smoke/MimeValidationSmoke.java` (78 pass / 2 skipped).
- See `docs/PATHHUNTER_MASTER_DOCUMENTATION.md` (phase details consolidated).

### Not in this release

- Managed HTTP execution, file content validation, polyglots, response-based verification.

### Tests

| Check | Result |
| --- | --- |
| MimeValidationSmoke | 78/78 (+2 SKIP) |
| FilenameTestingSmoke | 80/80 (+2 SKIP) |
| FileUploadAnalyzerSmoke | 80/80 (+2 SKIP) |
| UrlEncodingSmoke | 31/31 |
| AbsolutePathSmoke | 33/33 |
| Smoke | 29/29 |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

PortSwigger Content-Type lab: documented manual procedure only; not executed here.

## [0.5.0] – 2026-11-14

### Added

- **Filename & Extension Testing Engine** (`FilenameTestingEngine`, `FilenameAnalyzer`, `FilenameVariantGenerator`).
- Strategies: multiple extensions, case variants, trailing characters, encoding, whitespace/special, filename parameters, path-separator indicators.
- Configurable allowed/denied/custom extension lists with candidate limits and deterministic deduplication.
- Extended `UploadTestCase` / `UploadTestPlan` for structured planning (`NOT_EXECUTED`).
- File Upload → **Filename Testing** GUI sub-tab (generate/select/deselect; no Execute).
- `tools/smoke/FilenameTestingSmoke.java` (80 pass / 2 skipped).
- See `docs/PATHHUNTER_MASTER_DOCUMENTATION.md` (phase details consolidated).

### Not in this release

- Managed HTTP execution, MIME/content testing, polyglots, response-based verification.

### Tests

| Check | Result |
| --- | --- |
| FilenameTestingSmoke | 80/80 (+2 SKIP) |
| FileUploadAnalyzerSmoke | 80/80 (+2 SKIP) |
| UrlEncodingSmoke | 31/31 |
| AbsolutePathSmoke | 33/33 |
| Smoke | 29/29 |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

PortSwigger file-upload labs: documented manual procedure only; not executed here.

## [0.4.0] – 2026-11-14

### Added

- **File Upload Request Analyzer** (`uk.co.pentest.psychoPATH.fileupload`).
- Byte-oriented `MultipartParser` with binary-safe part extraction and resource limits.
- `FileSignatureInspector` for common magic-byte signatures (JPEG/PNG/GIF/PDF/ZIP/GZIP/BMP/WEBP/ELF/PE).
- Structured models: `UploadRequestResult`, `UploadPart`, `UploadFileMetadata`, `UploadAnalysisWarning`, plus empty `UploadTestPlan` / `UploadTestCase` for future managed testing.
- **File Upload** suite tab (`FileUploadPanel`) for passive analysis display.
- Context menu: **Send to PathHunter File Upload Analyzer**.
- `tools/smoke/FileUploadAnalyzerSmoke.java` (80 pass / 2 skipped Burp-runtime checks).
- See `docs/PATHHUNTER_MASTER_DOCUMENTATION.md` (phase details consolidated).

### Changed

- Compile set now includes `Java/src/uk/co/pentest/fileupload/*.java`.

### Not in this release

- Active/managed upload testing, filename/extension/MIME bypass engines, polyglot generation, response-based verification.

### Tests

| Check | Result |
| --- | --- |
| FileUploadAnalyzerSmoke | 80/80 (+2 SKIP Burp GUI) |
| UrlEncodingSmoke | 31/31 |
| AbsolutePathSmoke | 33/33 |
| Smoke | 29/29 |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

PortSwigger file-upload labs: documented manual procedure only; not executed here.

## [0.3.0] – 2026-11-13

### Added

- **URL Encoding & Decode-Order Bypass Engine** (`UrlEncodingEngine`).
- Strategies: single / double / triple URL encoding, mixed encoding, mixed separators, case variants, combined depths.
- Bounded encoding depth (default 2, max 5) and payload cap (400).
- Local decode-stage simulator for preview/analysis (no network).
- **URL Encoding** configuration tab with preview.
- `tools/smoke/UrlEncodingSmoke.java` (31 checks).
- See `docs/PATHHUNTER_MASTER_DOCUMENTATION.md` (phase details consolidated).

### Changed

- Extended `psychoPayload` metadata for encoding strategy fields (compatible defaults).
- URL encoding payloads are additive and **disabled by default**.

### Tests

| Check | Result |
| --- | --- |
| UrlEncodingSmoke | 31/31 |
| AbsolutePathSmoke | 33/33 |
| Smoke | 29/29 |
| LoadCheck | 11/11 |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

PortSwigger Academy superfluous URL-decode lab: documented manual procedure only; not executed here.

## [0.2.0] – 2026-11-13

### Added

- **Absolute Path Payload Generation** for the Path Traversal Intruder generator.
- New **Absolute paths** configuration tab: enable/disable, Linux / Windows / Custom targets, editable path list, validation status.
- `AbsolutePathEngine` helper for OS-specific validation, encoding, and deduplication.
- Payload strategy tags: `STANDARD_TRAVERSAL`, `ABSOLUTE_LINUX`, `ABSOLUTE_WINDOWS`, `ABSOLUTE_CUSTOM`.
- `tools/smoke/AbsolutePathSmoke.java` (33 automated checks).
- See `docs/PATHHUNTER_MASTER_DOCUMENTATION.md` (phase details consolidated).

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
