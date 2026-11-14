# Phase 4.4 — File Content Validation Engine

**Date:** 2026-11-14  
**Status:** Implemented (planning only). PortSwigger Academy: **NOT RUN**. Burp GUI: **NOT RUN**.

## Objectives

Analyze uploaded multipart file bytes with bounded structural checks (PNG / JPEG / GIF / PDF), compute SHA-256 integrity hashes, compare content vs filename/MIME/signature metadata, and generate controlled in-memory content candidates for structured test plans.

**Does not** send HTTP, upload files, execute content, write candidates to disk, generate webshells/polyglots, or treat structural/metadata inconsistency as a confirmed vulnerability.

## Architecture

Reuses Phase 4.1 `UploadPart.getContent()` defensive copies and `FileSignatureInspector`, Phase 4.2/4.3 `UploadTestCase` / `UploadTestPlan` (extended with `CATEGORY_CONTENT`, `TARGET_PART_CONTENT_BYTES`, content hash/length fields, `FileContentAnalysisResult`).

| Component | Role |
| --- | --- |
| `FileContentHasher` | SHA-256 of original and candidates; does not mutate input |
| `FileStructureValidator` / `StructureValidationResult` | Bounded PNG/JPEG/GIF/PDF structural checks |
| `FileContentAnalyzer` / `FileContentAnalysisResult` | Orchestrates hash + structure + consistency |
| `ContentConsistencyResult` | Extension / MIME / signature / structure relationship |
| `ContentVariantGenerator` / `ContentVariant` | Copy-only mutations; dedupe; limits |
| `ContentValidationConfig` / `ContentTestStrategy` | Strategies and resource limits |
| `FileContentValidationEngine` | Analyze + plan + in-memory artifacts map |

### Original content preservation

- Never mutates the multipart part’s stored bytes.
- Analysis and candidates use defensive copies.
- Original SHA-256 is recorded before generation; engine asserts hash stability after analysis and generation.
- Candidates expose independent clones via `getCandidateContent()`.

### Structural validation scope (partial only)

| Format | Verified | Not verified |
| --- | --- | --- |
| PNG | Signature, IHDR placement/length, chunk walk bounds, IEND, optional CRC on inspected chunks | Full decode, IDAT inflate, pixel validity |
| JPEG | SOI, segment length bounds, EOI | Entropy decode, Huffman tables |
| GIF | GIF87a/89a, LSD presence, trailer `0x3B` | LZW decode, full block graph |
| PDF | `%PDF-` header, `%%EOF`, optional obj hints | Interpreter, xref, JS, streams |

Statuses include `VALID_BASIC_STRUCTURE`, `INVALID_SIGNATURE`, `MISSING_REQUIRED_MARKER`, `TRUNCATED_CONTENT`, `INVALID_SEGMENT_LENGTH`, `UNSUPPORTED_FORMAT`, `ANALYSIS_LIMIT_REACHED`, etc. Confidence: `HIGH` / `MEDIUM` / `LOW` / `INCONCLUSIVE` (descriptive only).

### Strategies

Truncated content, required marker, signature/structure consistency, content length boundary, malformed structural metadata. Unsupported formats skip format-specific mutations (length-boundary only).

Limits (defaults / hard caps): max content 512 KiB / 2 MiB; max candidates 40 / 120; aggregate candidate bytes 2 MiB / 8 MiB; inspect depth aligned with structure validator.

## Future executor contract

Content cases set `testCategory=CONTENT` and `targetProperty=PART_CONTENT_BYTES`. Plans use `plan-content-…` IDs. Artifacts map (`candidateBytesByTestId`) stays in memory for a future Managed Test Executor. Filename cases still target `FILENAME`; MIME cases still target `PART_CONTENT_TYPE`.

## GUI

File Upload → **Content Testing**: selected part metadata (length, SHA-256), structure panel, consistency panel, strategies, plan table (select/deselect). Notice: Phase 4.4 generates controlled content-validation test plans only; HTTP execution deferred. No Execute control.

## Automated tests

| Suite | Result |
| --- | --- |
| FileContentValidationSmoke | **91 pass / 0 fail / 2 skipped** |
| MimeValidationSmoke | **78/78** (+2 SKIP) |
| FilenameTestingSmoke | **80/80** (+2 SKIP) |
| FileUploadAnalyzerSmoke | **80/80** (+2 SKIP) |
| Smoke / AbsolutePath / UrlEncoding | **29/29**, **33/33**, **31/31** |
| LoadCheck / ProveBlock / ScanLikeBurp | **11/11**, Pass, 1 candidate |

## PortSwigger

**NOT RUN.** Manual: capture upload → PathHunter File Upload → Content Testing → analyze structure/hash → generate candidates → review hashes/lengths in plan → optional Repeater only in authorized Academy labs. Do not claim a lab solved from candidate generation alone. Do not upload webshells or polyglots from this engine.

## Not implemented (at Phase 4.4 close)

HTTP execution, automatic uploads, content execution, webshell/polyglot generation, archive extraction, full image/PDF decode, response-based verification. Managed request preparation was delivered later as Phase 4.5.1 (still no HTTP send).
