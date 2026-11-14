# Phase 2.1 — Absolute Path Payload Engine

**Date:** 2026-11-13  
**Status:** Implemented and verified in automation (PortSwigger lab not solved in this environment)

## Original limitation

The Path Traversal / LFI generator produced relative file names (for example `etc/passwd`) and traversal-prefixed variants (for example `../../etc/passwd`). It did **not** emit bare absolute filesystem paths such as `/etc/passwd` without a traversal prefix.

Existing “Include absolute webroots” controls document-root absolute paths for upload hunting; they are unrelated to absolute *file* path bypasses.

## Reference lab scenario

PortSwigger Web Security Academy (Practitioner):

**File path traversal, traversal sequences blocked with absolute path bypass**

The application blocks `../` sequences but treats the filename as relative to a default working directory. Supplying an absolute path retrieves `/etc/passwd`:

```http
GET /image?filename=/etc/passwd HTTP/1.1
```

## Technical analysis

- Payload construction lives in `IntruderPayloadGenerator` for Intruder types `path` / `mark`.
- Encodings use shared `dots` / `slashes` / `backslashes` tables.
- Terminators are applied after the logical payload list is built.
- Smallest change: optional post-phase that appends validated absolute paths (with the same encoding indexes) without prepending traversals or webroots.

## Implementation design

| Piece | Role |
| --- | --- |
| `AbsolutePathEngine` | Validation, defaults, encoding, dedupe, strategy labels |
| `psychoPayload.strategy` | Distinguishes `STANDARD_TRAVERSAL` vs absolute strategies |
| `PsychoPanel2` Absolute paths tab | Enable checkbox, Linux / Windows / Custom targets, list editing, status |
| `IntruderPayloadGenerator.appendAbsolutePathPayloads` | Additive phase before terminators |

Absolute generation is **off by default** so existing payload sets remain unchanged until enabled.

## New payload strategies

- `STANDARD_TRAVERSAL` (existing)
- `ABSOLUTE_LINUX`
- `ABSOLUTE_WINDOWS`
- `ABSOLUTE_CUSTOM`

## Configuration

Tab: **Absolute paths**

- **Enable Absolute Path Payloads**
- Targets: Linux / Windows / Custom Paths
- Editable list with Add / Remove / Clear / Reset defaults
- Status line for validation errors

Default Linux list includes `/etc/passwd`, `/etc/hosts`, `/etc/hostname`, `/etc/issue`, `/proc/self/environ`, `/proc/version`.  
Default Windows list includes `C:\Windows\win.ini` and `C:\Windows\System32\drivers\etc\hosts`.

## Encoding behavior

For each enabled encoding index, separators and dots in the logical absolute path are substituted once from the existing tables (raw / URL / double-URL / UTF variants). Absolute encoding does not re-run the traversal pipeline and does not double-apply encoding to an already-encoded string.

## Automated tests

`tools/smoke/AbsolutePathSmoke.java` — **33/33 pass**  
Existing `Smoke` — **29/29 pass**  
`LoadCheck` 11/11, `ProveBlock` Pass, `ScanLikeBurp` 1 candidate

## Remaining limitations

- Absolute paths are not a separate Intruder generator name; they augment Path traversal when enabled.
- `file:///` prefixes remain unimplemented (`addFilePrefix` unused).
- Burp GUI and PortSwigger lab were not executed in this phase.

## Manual PortSwigger lab validation procedure

1. Open the lab in an authorized PortSwigger Academy session.
2. Browse until the image request with a `filename` (or equivalent) parameter appears.
3. Intercept with Burp Proxy; send the request to Intruder.
4. Clear positions; mark only the filename parameter value as a payload position.
5. Attack type: Sniper (or Pitchfork if also using Payload marker).
6. In PathHunter → **Absolute paths**: enable Absolute Path Payloads; select **Linux**; ensure `/etc/passwd` is listed.
7. Optionally disable evasive/terminator noise for a smaller set, or leave defaults.
8. Intruder → Payloads → Payload type → Extension-generated → **Path traversal**.
9. Start attack; look for a 200 response whose body contains `root:x:` (or similar passwd content).
10. Confirm the winning payload is the bare absolute path `/etc/passwd` (or its enabled encoding variant).

**Lab solved in this phase?** No — not tested against the live Academy lab here.
