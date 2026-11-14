# Phase 4.3 — MIME Validation Testing Engine

**Date:** 2026-11-14  
**Status:** Implemented (planning only). PortSwigger Academy: **NOT RUN**. Burp GUI: **NOT RUN**.

## Objectives

Analyze declared MIME metadata relative to filename extension and detected file signature; generate controlled MIME Content-Type test candidates; produce structured plans for future managed execution.

**Does not** send HTTP, mutate binary content, or treat mismatches as confirmed vulnerabilities.

## Architecture

Reuses Phase 4.1 `UploadPart` / `FileSignatureInspector` and Phase 4.2 `UploadTestCase` / `UploadTestPlan` (extended with `CATEGORY_MIME`, `TARGET_PART_CONTENT_TYPE`, MIME fields, `MimeComparisonResult`).

| Component | Role |
| --- | --- |
| `MimeAnalyzer` / `MimeParseResult` | Parse/normalize Content-Type; preserve raw |
| `MimeExtensionMapper` | Known/custom extension↔MIME associations |
| `MimeSignatureComparator` / `MimeComparisonResult` | Metadata relationship status + confidence |
| `MimeVariantGenerator` / `MimeVariant` | Candidate generation + dedupe |
| `MimeTestingConfig` | Strategies, custom MIME, limits |
| `MimeValidationEngine` | Validation + plan assembly |

## Comparison statuses

`CONSISTENT`, `MIME_EXTENSION_MISMATCH`, `MIME_SIGNATURE_MISMATCH`, `EXTENSION_SIGNATURE_MISMATCH`, `MULTIPLE_INCONSISTENCIES`, `UNKNOWN_SIGNATURE`, `UNKNOWN_MIME`, `MISSING_MIME`, `INCONCLUSIVE`.

Confidence: `HIGH` / `MEDIUM` / `LOW` / `INCONCLUSIVE` (descriptive only).

`application/octet-stream` is treated as a special generic that does not alone force a signature mismatch.

## Strategies

Declared variations, case variations, parameter variations, missing/empty (distinct), extension mismatch, custom MIME.

Limits: default max **80** (hard **300**); custom MIME max **32**.

## Future executor contract

MIME cases set `targetProperty=PART_CONTENT_TYPE` and keep `originalFilename` unchanged. Filename cases set `targetProperty=FILENAME`. Plans use distinct IDs (`plan-mime-…` vs `plan-fn-…`).

## GUI

File Upload → **MIME Testing**: selected part, MIME analysis, comparison, strategies, custom MIME, plan table (no Execute).

## Automated tests

| Suite | Result |
| --- | --- |
| MimeValidationSmoke | **78 pass / 0 fail / 2 skipped** |
| FilenameTestingSmoke | **80/80** (+2 SKIP) |
| FileUploadAnalyzerSmoke | **80/80** (+2 SKIP) |
| Smoke / AbsolutePath / UrlEncoding | **29/29**, **33/33**, **31/31** |
| LoadCheck / ProveBlock / ScanLikeBurp | **11/11**, Pass, 1 candidate |

## PortSwigger

**NOT RUN.** Manual: Content-Type restriction bypass lab → analyze → MIME Testing → generate plan → compare candidates in Repeater. Do not claim solved from generation alone.

## Not implemented

HTTP execution, content validation, polyglots, response verification, Phase 4.4+.
