# Phase 4.1 — File Upload Request Analyzer

**Date:** 2026-11-14  
**Status:** Implemented (passive analysis). PortSwigger Academy live validation: **NOT RUN**. Burp GUI load: **NOT RUN** in this environment.

## Objectives

Establish PathHunter’s File Upload Security Engine foundation:

- Detect multipart and other upload-shaped HTTP requests
- Parse `multipart/form-data` with byte-safe binary preservation
- Extract file metadata (filename, MIME, signatures)
- Present results in a dedicated **File Upload** tab
- Accept requests from Burp via context menu
- Keep architecture ready for future managed testing **without** implementing active upload tests

## Architecture analysis (pre-implementation)

| Area | Finding |
| --- | --- |
| Entry | `uk.co.pentest.psychoPATH.BurpExtender` (legacy Extender API) |
| GUI | `PsychoTab` → `PsychoPanel2` tabbed pane |
| Context menu | `Menu` already registered; extended with upload analyzer action |
| Path engines | `AbsolutePathEngine` / `UrlEncodingEngine` left unchanged |
| API | No Montoya; compile against desktop Burp jar |

Integration choice: extend existing `IContextMenuFactory` rather than Proxy listeners, so analysis is explicit and does not inject traffic.

## Module structure

```text
uk.co.pentest.psychoPATH.fileupload
  FileUploadRequestAnalyzer
  MultipartParser
  FileSignatureInspector
  FileUploadPanel
  UploadRequestResult / UploadPart / UploadFileMetadata / UploadAnalysisWarning
  UploadRequestClassification / UploadPartClassification / UploadWarningSeverity
  UploadTestPlan / UploadTestCase   (empty-plan helpers only)
```

Sources live under `Java/src/uk/co/pentest/fileupload/` (package `…psychoPATH.fileupload`).

## Request detection

| Classification | Criteria (observable only) |
| --- | --- |
| `CONFIRMED_MULTIPART_UPLOAD` | `multipart/form-data` with ≥1 filename-bearing part |
| `POTENTIAL_BINARY_UPLOAD` | PUT/POST with binary-like Content-Type or opaque PUT body |
| `POTENTIAL_FILE_UPLOAD` | Upload-shaped URL/form clues, or multipart with text fields only |
| `ORDINARY_REQUEST` | No upload characteristics |
| `MALFORMED_UPLOAD_REQUEST` | Declares multipart but missing/invalid boundary or fatal parse |

Parsing alone never asserts a vulnerability.

## Multipart parsing & binary safety

- Body searched as bytes; headers decoded as ISO-8859-1 only
- Subsequent delimiters require line-start (`\r\n--boundary` / `\n--boundary`) so in-content lookalikes are not split
- Part bodies returned as defensive byte copies
- Limits: body **5 MiB**, parts **100**, header line **8 KiB**, headers/part **50**

## File signatures

Bounded prefix inspection (≤32 bytes): JPEG, PNG, GIF, PDF, ZIP, GZIP, BMP, WEBP, ELF, PE/MZ, else `UNKNOWN`. Recognition ≠ safety or executability.

## GUI & Burp integration

- Tab: **File Upload**
- Actions: Analyze Request, Clear
- Summary / parts table / selected-part metadata / warnings
- Informational note that managed testing is future work (no fake Execute buttons)
- Context menu: **Send to PathHunter File Upload Analyzer**
- Analysis on `SwingWorker`; EDT updates only
- Duplicate consecutive identical byte submissions skipped

## Future managed testing readiness

`UploadRequestResult` retains original request bytes, part indexes, filenames, MIME, signatures, and analysis id. `UploadTestPlan.emptyFromAnalysis` creates an empty plan referencing that id. **Not implemented:** planner, executor, response analysis, evidence, findings from server behaviour.

## Resource limits (rationale)

| Limit | Value | Why |
| --- | --- | --- |
| Request / body | ~5 MiB (+64 KiB headers) | Bound memory on untrusted input |
| Parts | 100 | Avoid pathological part storms |
| Header line | 8 KiB | Bound header parsing |
| Signature prefix | 32 B | Enough for listed magics |

## Automated tests

| Suite | Result |
| --- | --- |
| FileUploadAnalyzerSmoke | **80 pass / 0 fail / 2 skipped** (Burp GUI / context menu NOT RUN) |
| Smoke | **29/29** |
| AbsolutePathSmoke | **33/33** |
| UrlEncodingSmoke | **31/31** |
| LoadCheck | **11/11** |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

## PortSwigger Academy validation

Labs under [File upload vulnerabilities](https://portswigger.net/web-security/learning-paths/file-upload-vulnerabilities) were **not** opened in this environment.

**Manual reproduction (operator):**

1. Load `dist/PathHunter.jar` in Burp.
2. Capture a lab upload request (multipart).
3. Right-click → **Send to PathHunter File Upload Analyzer**.
4. Confirm classification, boundary, filename, Content-Type, and binary length.
5. Do **not** claim the lab is solved from parsing alone.

## Not implemented (deferred)

Filename/extension/MIME/content manipulation testing, polyglots, managed active uploads, automated request execution, response-based verification, race conditions, automatic vulnerability reporting from server behaviour, Phase 4.2+.

## Build notes

Compile must include `Java\src\uk\co\pentest\fileupload\*.java` in addition to `Java\src\uk\co\pentest\*.java`. Artifact: `dist/PathHunter.jar`, entry `uk.co.pentest.psychoPATH.BurpExtender`, bytecode major **52**, no bundled `burp/` API classes.
