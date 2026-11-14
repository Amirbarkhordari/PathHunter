# Changelog

All notable changes to PathHunter are documented in this file.

## [Unreleased]

### Planned

- Phase 4.5 – Managed File Upload Test Executor (not started)
- Further path-traversal engine enhancements (see README roadmap)

## [0.7.0] – 2026-11-14

### Added

- **File Content Validation Engine** (`FileContentValidationEngine`, `FileContentAnalyzer`, `FileStructureValidator`, `FileContentHasher`, `ContentVariantGenerator`).
- Bounded PNG / JPEG / GIF / PDF structural checks; SHA-256 integrity; metadata consistency.
- Controlled in-memory content candidates (truncate / marker / signature / length / malformed metadata) with dedupe and resource limits.
- Extended `UploadTestCase` / `UploadTestPlan` with `CATEGORY_CONTENT`, `TARGET_PART_CONTENT_BYTES`, content hashes/lengths, `FileContentAnalysisResult`.
- File Upload → **Content Testing** GUI sub-tab (no Execute).
- `tools/smoke/FileContentValidationSmoke.java` (91 pass / 2 skipped).
- Phase write-up: `docs/phases/phase-4.4-file-content-validation-engine.md`.

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
- Phase write-up: `docs/phases/phase-4.3-mime-validation-testing-engine.md`.

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
- Phase write-up: `docs/phases/phase-4.2-filename-extension-testing-engine.md`.

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
- Phase write-up: `docs/phases/phase-4.1-file-upload-request-analyzer.md`.

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
- Phase write-up: `docs/phases/phase-2.2-url-encoding-engine.md`.

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
