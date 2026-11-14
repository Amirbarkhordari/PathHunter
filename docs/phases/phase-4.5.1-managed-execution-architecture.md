# Phase 4.5.1 — Managed Execution Architecture & Multipart Request Builder

**Date:** 2026-11-14  
**Status:** Implemented (prepare/validate/preview only). HTTP send: **not implemented**. Burp GUI: **NOT RUN**.

## Objectives

Introduce managed execution foundation: test-case lifecycle, validation, byte-safe multipart reconstruction, request integrity/diff, sensitive-header redaction, authorization policy defaults, and Managed Testing GUI — all without sending HTTP.

## Architecture

Package: `uk.co.pentest.psychoPATH.fileupload.execution`

| Component | Role |
| --- | --- |
| `ManagedExecutionManager` | Batch validate/prepare; content artifact store; never sends |
| `ExecutionStateManager` | Lifecycle (NOT_EXECUTED → SELECTED → VALIDATING → READY_FOR_EXECUTION / PREPARATION_FAILED / CANCELLED) |
| `MultipartRequestBuilder` | Exactly one mutation: filename / part Content-Type / part bytes |
| `HttpRequestFrame` | HTTP/1.x split/rebuild; rejects HTTP/2 pseudo-headers |
| `CandidateContentStore` | Verified in-memory content candidates (SHA-256 + length) |
| `ExecutionAuthorizationPolicy` | Defaults **disabled**; foundation for Phase 4.5.2 |
| `SensitiveHeaderRedactor` | Cookie / Authorization / CSRF redaction for previews |
| `RequestDiff` / `PreparedRequestArtifact` | Safe diff + immutable prepared request |

Integrates with Phases 4.1–4.4 via `UploadRequestResult.getOriginalRequestBytes()`, `UploadTestCase` targets, and content plan artifacts.

## Supported targets

- `TARGET_FILENAME` — Content-Disposition filename only  
- `TARGET_PART_CONTENT_TYPE` — part Content-Type only (or remove if `MimeVariant.MISSING_SENTINEL`)  
- `TARGET_PART_CONTENT_BYTES` — part body only (hash-verified artifact)

Original request bytes and unrelated parts/headers are never mutated. Prepared cases remain `READY_FOR_EXECUTION` / `NOT_EXECUTED` — never `EXECUTED`.

## GUI

File Upload → **Managed Testing**: load selected cases from Filename/MIME/Content plans, validate, prepare in memory, preview (redacted), diff, clear/cancel. **No Execute button.**

Notice: Phase 4.5.1 prepares and validates modified multipart requests in memory only. No HTTP requests are sent.

## Automated tests

| Suite | Result |
| --- | --- |
| ManagedRequestBuilderSmoke | **73 pass / 0 fail / 2 skipped** |
| FileContentValidationSmoke | **91/91** (+2 SKIP) |
| MimeValidationSmoke | **78/78** (+2 SKIP) |
| FilenameTestingSmoke | **80/80** (+2 SKIP) |
| FileUploadAnalyzerSmoke | **80/80** (+2 SKIP) |
| Smoke / AbsolutePath / UrlEncoding | **29/29**, **33/33**, **31/31** |
| LoadCheck / ProveBlock / ScanLikeBurp | **11/11**, Pass, 1 candidate |

## Not implemented

HTTP transmission, Burp request send, redirects, response analysis, evidence reporting, vulnerability confirmation, Phase 4.5.2+.

## Future contracts

- 4.5.2: send prepared artifacts under explicit authorization/scope  
- 4.5.3: response analysis  
- 4.5.4: evidence / results  
