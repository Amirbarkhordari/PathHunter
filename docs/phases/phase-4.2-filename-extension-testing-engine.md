# Phase 4.2 — Filename & Extension Testing Engine

**Date:** 2026-11-14  
**Status:** Implemented (planning only). PortSwigger Academy live validation: **NOT RUN**. Burp GUI: **NOT RUN**.

## Objectives

Extend the Phase 4.1 File Upload module with:

- Structured filename analysis
- Multi-segment extension extraction
- Controlled filename/extension test-candidate generation
- Structured `UploadTestPlan` / `UploadTestCase` population
- GUI for strategy selection, extension configuration, and plan preview

**Does not** send HTTP, upload files, or mark candidates as confirmed bypasses.

## Architecture analysis

Phase 4.1 provides `UploadRequestResult`, multipart parts, raw/parsed filenames, and original request bytes. Context menu → `FileUploadPanel.acceptRawRequest`. Phase 4.2 consumes that analysis via `FilenameTestingEngine.generatePlan(analysis, partIndex, config)`.

Path Traversal / AbsolutePath / UrlEncoding engines are unchanged. Path-separator filename indicators note overlap and defer traversal payloads to the existing Path Traversal engine.

## Components

| Class | Role |
| --- | --- |
| `FilenameAnalyzer` | Raw-preserving filename characteristics |
| `FilenameAnalysisResult` | Structured analysis model |
| `FilenameTestStrategy` | Strategy IDs/categories |
| `FilenameTestingConfig` | Strategy toggles, extension lists, limits |
| `FilenameVariantGenerator` | Deterministic candidate generation + dedupe |
| `FilenameVariant` | Candidate + strategy metadata |
| `FilenameTestingEngine` | Validation + plan assembly |
| `UploadTestCase` / `UploadTestPlan` | Extended planning models (`NOT_EXECUTED`) |

## Filename analysis

Preserves the exact raw filename. Exposes base, final extension, all segments, whitespace/dot/encoding/separator indicators, and informational warnings. Unusual traits are **not** vulnerabilities.

### Extension extraction examples

| Filename | Segments | Final |
| --- | --- | --- |
| `image.jpg` | `[jpg]` | jpg |
| `archive.tar.gz` | `[tar, gz]` | gz |
| `test.php.jpg` | `[php, jpg]` | jpg |
| `.htaccess` | `[]` | (hidden) |
| `filename.` | `[""]` | empty |
| `filename` | `[]` | (none) |

## Strategies

1. Multiple Extension Testing  
2. Extension Case Variations  
3. Trailing Character Testing  
4. Filename Encoding Variations (independent of `UrlEncodingEngine`)  
5. Whitespace / Special Characters  
6. Filename Parameter Variations (`filename` / `filename*`)  
7. Path Separator Indicators (overlap noted; no second traversal engine)

## Limits & deduplication

- Default max candidates: **120** (hard max **400**)
- Encoding depth default **1** (hard max **2**)
- Extension list max **64**
- Dedup merges alternate strategies into `alsoProducedBy` metadata
- Deterministic ordering for identical inputs/config

## GUI

File Upload tab → **Filename Testing** sub-tab:

- Selected request / part summary
- Filename analysis panel
- Strategy checkboxes
- Allowed / denied / custom extensions
- Generate / Clear / Reset / Select All / Deselect All
- Plan table with selection checkboxes
- Explicit notice: cases are `NOT_EXECUTED`; no Execute button

## Future executor compatibility

Each case retains analysis id, plan id, part index, original/generated filenames, strategy, transformation, and `NOT_EXECUTED`. Future Phase 4.5 can replace the filename in the preserved multipart part without redesigning these contracts.

## Automated tests

| Suite | Result |
| --- | --- |
| FilenameTestingSmoke | **80 pass / 0 fail / 2 skipped** |
| FileUploadAnalyzerSmoke | **80/80** (+2 SKIP) |
| Smoke | **29/29** |
| AbsolutePathSmoke | **33/33** |
| UrlEncodingSmoke | **31/31** |
| LoadCheck | **11/11** |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

## PortSwigger Academy

**NOT RUN** in this environment.

Manual procedure: load jar → capture lab upload → analyze → Filename Testing → generate plan → compare candidates manually in Repeater. Do not claim a lab is solved from generation alone. Do not generate operational web shells.

## Not implemented

MIME testing, content validation, polyglots, managed executor, automatic uploads, response verification, race conditions, confirmed findings from candidates, Phase 4.3+.
