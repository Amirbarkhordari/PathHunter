# PathHunter

**Independently maintained Burp Suite extension for path traversal, LFI, and file-upload security testing.**

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Burp Suite Extension](https://img.shields.io/badge/Burp%20Suite-Extension-orange.svg)](#installation)
[![Status: Active development](https://img.shields.io/badge/Status-Active%20development-brightgreen.svg)](#roadmap)

PathHunter helps security testers probe **path traversal**, **LFI / arbitrary file read**, and **file-upload validation** gaps using configurable Intruder generators and a File Upload analysis / planning / request-preparation module.

**Maintainer:** Amir Barkhordari — https://github.com/Amirbarkhordari/PathHunter

**Full technical documentation:** [`docs/PATHHUNTER_MASTER_DOCUMENTATION.md`](docs/PATHHUNTER_MASTER_DOCUMENTATION.md)

---

## Introduction

PathHunter loads as a legacy Burp Extender (`IBurpExtender`) Java extension and provides:

- A **PathHunter** suite tab for payload and upload configuration
- Intruder generators: **Path traversal**, **Directory checker**, **Payload marker**, **Byte**
- Optional active Scanner checks for common LFI indicators
- File Upload Analyzer with Filename / MIME / Content test planning and in-memory managed request preparation

Authorized assessments only (including systems you own and PortSwigger Academy labs).

---

## Current features

| Area | Status | Notes |
| --- | --- | --- |
| Path traversal / LFI payloads | Implemented | Roots, targets, depth, evasive patterns |
| Absolute path payloads | Implemented | Opt-in Absolute paths tab (off by default) |
| URL encoding bypass payloads | Implemented | Opt-in URL Encoding tab (off by default) |
| Active Scanner LFI check | Implemented | Optional; not run against remotes in automation |
| File Upload Analyzer | Implemented | Passive multipart/binary analysis |
| Filename / MIME / Content testing | Implemented | Structured plans; preparation does not send |
| Managed prepare + controlled execute | Implemented | Auth-gated Burp send; disabled by default |
| Response analysis / evidence / reports | Implemented | Behavioral analysis; JSON/HTML export |
| Directory checker / encodings / byte gen | Partial | See master docs — known limitations |

Details, architecture, and limitations: [`docs/PATHHUNTER_MASTER_DOCUMENTATION.md`](docs/PATHHUNTER_MASTER_DOCUMENTATION.md).

---

## Installation

**Artifact:** `dist/PathHunter.jar`  
**Entry class:** `uk.co.pentest.psychoPATH.BurpExtender`

1. Burp → **Extensions** → **Add** → type **Java**
2. Select `dist/PathHunter.jar`
3. Confirm name **PathHunter**, tab **PathHunter**, and Intruder generators above

Rebuild: see master documentation §3 (Installation and Configuration). Prefer `dist/PathHunter.jar` over historical `Java/psychoPATH.jar`.

---

## Basic usage

1. Configure Path Traversal / Absolute Paths / URL Encoding in the PathHunter tab; use Intruder **Path traversal** as needed.
2. For uploads: prefer context menu **Send to PathHunter File Upload Analyzer** (keeps target service + baseline response).
3. Generate Filename / MIME / Content plans; review candidates.
4. Managed Testing: Load → Validate → **Prepare** (in memory, never sends).
5. Enable execution, **Authorize Target**, confirm, then **Execute Selected Prepared** (second confirmation). Review **Results** and export JSON/HTML if needed.

Preparation never transmits HTTP. Execution is disabled by default and requires explicit authorization.

---

## Compatibility

| Item | Value |
| --- | --- |
| API | Legacy Burp Extender (not Montoya) |
| Bytecode | Java 8 (`--release 8`, major version 52) |
| Artifact | `dist/PathHunter.jar` (no bundled `burp/` API classes) |

---

## Roadmap (planned — not implemented)

- Further path / encoding / LFI / UI hardening
- Defect fixes documented in the master document
- Manual Burp / PortSwigger Academy validation of File Upload execution

---

## Documentation

| Document | Purpose |
| --- | --- |
| [`docs/PATHHUNTER_MASTER_DOCUMENTATION.md`](docs/PATHHUNTER_MASTER_DOCUMENTATION.md) | **Canonical** architecture, features, tests, roadmap, history |
| [`CHANGELOG.md`](CHANGELOG.md) | Concise version history |
| [`NOTICE`](NOTICE) / [`LICENSE`](LICENSE) | GPL-3.0 provenance and license |

Supplemental historical notes (non-canonical if they conflict with the master doc): `docs/burp-loading-fix.md`, `docs/identity-audit.md`, `docs/github-fork-detachment.md`.

Regression harness: `tools/smoke/` (`Smoke`, `AbsolutePathSmoke`, `UrlEncodingSmoke`, `FileUploadAnalyzerSmoke`, `FilenameTestingSmoke`, `MimeValidationSmoke`, `FileContentValidationSmoke`, `ManagedRequestBuilderSmoke`, `LoadCheck`, `ProveBlock`, `ScanLikeBurp`).

---

## License and Legal Information

PathHunter is licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE).

This repository contains modified GPL-3.0-licensed material. It does **not** claim that all historical source was authored by the current maintainer. Required provenance, copyright, and attribution details are recorded in [`NOTICE`](NOTICE) and in applicable source-file headers / `@author` tags. Those legal notices must be preserved.
