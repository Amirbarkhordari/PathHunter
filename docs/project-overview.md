# PathHunter project overview

PathHunter is an independently maintained Burp Suite extension for path traversal, LFI, and file-upload path testing. Maintainer: **Amir Barkhordari**. License: **GPL-3.0**.

Required provenance and copyright details for this GPL-3.0 derivative work are in `NOTICE` and in applicable source headers. This overview focuses on PathHunter’s current identity and layout.

## Identity

| Item | Verified value |
| --- | --- |
| Project name | **PathHunter** |
| Maintainer | Amir Barkhordari |
| Repository | https://github.com/Amirbarkhordari/PathHunter |
| License | GPL-3.0 (`LICENSE`); provenance in `NOTICE` |
| Primary language | Java |
| Host product | Burp Suite, legacy Extender API (`burp.IBurpExtender`) |
| Extension display name | **PathHunter** |
| Suite tab caption | **PathHunter** |
| Context menu | **Propagate to PathHunter** |
| Distribution artifact | `dist/PathHunter.jar` |
| Entry class | `uk.co.pentest.psychoPATH.BurpExtender` (technical package name retained) |
| Historical binary (untouched) | `Java/psychoPATH.jar` |

## What the program is

PathHunter generates path-traversal payloads and feeds them to Intruder. A second generator emits a fixed-width numeric marker so a successful upload can be tied back to the payload index. A third generator replays directories learned from Burp's site map. A fourth generator emits single bytes for generic fuzzing.

The same path generator is also used by an active Scanner check that looks for readable files such as `etc/passwd` and `win.ini`. The check is off unless the "Enable Scanner checks" box is selected. That box is selected by default when the UI is constructed.

Hunting situations supported by payload generation (not a separate upload engine):

- Local file inclusion or arbitrary file read, including non-recursive filters.
- Upload into a directory outside the document root, reached by an absolute traversal.
- Upload into a hidden directory inside the document root, reached by a relative traversal such as `../../a.jpg`.
- Upload into a hidden directory with no traversal, found by requesting known web-root directories afterwards.

## Repository layout

| Path | Role |
| --- | --- |
| `README.md` | PathHunter public identity, features, install, roadmap |
| `NOTICE` | Required GPL provenance / attribution |
| `LICENSE` | GPL-3.0 |
| `CHANGELOG.md` | Versioned changes |
| `BappDescription.html` | Burp App Store style description (PathHunter) |
| `Java/src/uk/co/pentest/` | Extension source |
| `Java/build.xml` | NetBeans Ant wrapper (imports missing `nbproject/`) |
| `Java/psychoPATH.jar` | Historical prebuilt jar (left untouched) |
| `dist/PathHunter.jar` | PathHunter distribution artifact |
| `tools/smoke/` | Regression harness sources |
| `docs/` | Technical and process documentation |
| `perl/`, `test_cases/`, `screenshots/` | Historical fixtures and media |

## Intruder payload generators

| Factory key | Name shown in Intruder | Implementation |
| --- | --- | --- |
| `byte` | Byte | `IntruderPayloadGeneratorByte` |
| `check` | Directory checker | `IntruderPayloadGenerator` with `payloadType=check` |
| `path` | Path traversal | `IntruderPayloadGenerator` with `payloadType=path` |
| `mark` | Payload marker | `IntruderPayloadGenerator` with `payloadType=mark` |

## Default behaviour when the UI is created

- LFI mode is on. Web-root lists are not applied until LFI mode is turned off.
- Longest-traversal optimisation for LFI is on.
- Evasive list starts as `....//`, `...//`, `.....///`, `.{BREAK}.{BREAK}/`.
- Break-up sequence starts as hex `20` (a space).
- RAW and URL encodings are on. Double URL, UTF-16, and both overlong UTF ranges start off.
- Mixed encodings are on.
- Terminators are on. Default terminator characters are `%00` (literal NUL off by default).
- Intruder file name is `etc/passwd`. Scanner names are `etc/passwd` (nix) and `win.ini` (win).
- Scanner match rules are `root:x:`, `root:$`, and `[mci extensions]`.
- Target OS combo is `all`, `win`, `nix`.
- Directory separator combo starts as `all`.
- One target is preloaded: `example.org`.
- Maximum traversal repeat count is 18.

## Phase summary

| Phase | Focus |
| --- | --- |
| 0 | Audit, rebuild, document, smoke harness |
| 0.1 | Burp load fix; `dist/PathHunter.jar` |
| 1 | Independent identity preparation |
| 1.1 | GitHub finalization, commit, push |
| 1.2 | Public branding presentation; concise legal section |
