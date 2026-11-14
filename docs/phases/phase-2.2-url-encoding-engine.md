# Phase 2.2 — URL Encoding & Decode-Order Bypass Engine

**Date:** 2026-11-13  
**Status:** Implemented and verified in automation (PortSwigger lab not live-tested)

## Why this engine was needed

Some applications strip literal traversal sequences such as `../` and then URL-decode the input (sometimes after a framework has already decoded once). Ordinary traversal and absolute-path strategies do not systematically emit the single-/double-/triple-encoded and mixed forms required to test that class of filter.

Reference Academy lab (authorized testing only):

**File path traversal, traversal sequences stripped with superfluous URL-decode**

Typical candidate family includes double-encoded units such as `%252e%252e%252f` repeated before `etc/passwd`.

## Strategies

| Strategy ID | Behavior |
| --- | --- |
| `URL_ENCODED_TRAVERSAL` | Depth-1 encode of traversal units |
| `DOUBLE_URL_ENCODED_TRAVERSAL` | Depth-2 (`%252e%252e%252f` style) |
| `TRIPLE_URL_ENCODED_TRAVERSAL` | Depth-3 |
| `MIXED_URL_ENCODED_TRAVERSAL` | Encode dots and/or separators selectively |
| `MIXED_SEPARATOR_TRAVERSAL` | `/`, `\`, and encoded mixes |
| `ENCODING_CASE_VARIANT` | Uppercase hex (`%2E` / `%2F` / `%5C`) |
| `COMBINED_ENCODING_TRAVERSAL` | Depths above 3 within the configured max |

Encoding passes only transform `.`, `/`, `\`, and `%` so repeated depth yields `%252e` rather than encoding hex digits.

## Encoding depth

- Default max depth: **2**
- Hard cap: **5**
- Traversal repeats: 1–8 (default 3)
- Payload hard cap: **400** unique strings

## Transformation simulator

`UrlEncodingEngine.simulateDecodeStages` / `urlDecodeOnce`:

- Deterministic local URL-decoding
- Invalid `%` sequences left unchanged
- No network I/O
- Preview shows `decode[0]…decode[n]` stages

Generation and verification remain separate: a generated payload is a **test candidate**, not a confirmed bypass.

## GUI

Tab: **URL Encoding**

- Enable URL Encoding Payloads (default **off**)
- Strategy checkboxes
- Max encoding depth / traversal repeats
- Target OS: Linux / Windows / Custom
- Target path (default `/etc/passwd`)
- Generate preview / Clear / Reset to Defaults
- Status line for validation

Preview runs off the EDT.

## Intruder usage

1. Enable URL Encoding Payloads and preferred strategies.
2. Intruder → Extension-generated → **Path traversal**.
3. Review responses; do not treat generation as proof of vulnerability.

## Automated tests

| Suite | Result |
| --- | --- |
| UrlEncodingSmoke | **31/31** |
| AbsolutePathSmoke | **33/33** |
| Smoke | **29/29** |
| LoadCheck | **11/11** |
| ProveBlock | Pass |
| ScanLikeBurp | 1 candidate |

## PortSwigger lab

**Not solved in this environment.** Manual procedure mirrors Phase 2.1 docs: enable URL Encoding, Linux target `/etc/passwd`, Path traversal generator, inspect for `root:` content. Lab success requires live Academy verification by the operator.

## Known limitations

- Independent of the legacy Evasive “encodingsEnabled” pipeline (by design).
- Not a separate Intruder factory name.
- Persistence across Burp restarts is not added (same as Absolute paths tab).
- Does not claim decode-order of a remote target without response analysis.
