# PathHunter

**Independently maintained Burp Suite extension for path traversal, LFI, and file-upload path testing.**

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Burp Suite Extension](https://img.shields.io/badge/Burp%20Suite-Extension-orange.svg)](#installation)
[![Status: Active development](https://img.shields.io/badge/Status-Active%20development-brightgreen.svg)](#roadmap)

PathHunter is a Java extension for Burp Suite that helps security testers find **file path traversal**, **local file inclusion (LFI) / arbitrary file read**, and **file-upload path** issues using configurable payload generators, encoding options, and Intruder/Scanner integration.

**Maintainer:** Amir Barkhordari — https://github.com/Amirbarkhordari/PathHunter

---

## Introduction

PathHunter generates and applies path-oriented payloads inside Burp Suite so you can probe how an application resolves file paths—especially when filters, encoding, or non-recursive sanitization obscure classic `../` patterns.

**What it is for**

- Web application security testing (authorized assessments only)
- Blind or weakly filtered path traversal and LFI hunting
- File-upload workflows where the upload directory sits inside or outside the document root
- Directory discovery tied to Burp’s site map (“Directory checker”)

**How it fits Burp Suite**

- Loads as a legacy Extender API (`IBurpExtender`) Java extension
- Adds a **PathHunter** suite tab for configuration
- Registers Intruder payload generators (path traversal, directory checker, payload marker, byte)
- Optionally runs an active Scanner check for common LFI indicators

PathHunter is actively maintained and enhanced for modern Burp Suite compatibility. Planned work is listed under [Roadmap](#roadmap); only verified behaviour is described under [Current features](#current-features).

---

## Current features

The following are present in the current sources and have been exercised by the local rebuild / smoke harness (see `docs/feature-inventory.md` and `docs/test-results.md`). Features marked *payload only* generate strings for Intruder; they do not perform HTTP file uploads by themselves.

| Area | Status | Notes |
| --- | --- | --- |
| Path traversal payload generator | Implemented | Configurable roots, targets, suffixes, traversal depth, evasive patterns |
| Absolute filesystem path payloads | Implemented | Optional Absolute paths tab; bare paths such as `/etc/passwd` without `../` (off by default) |
| URL encoding / decode-order bypass | Implemented | Optional URL Encoding tab; single/double/triple/mixed/case variants; decode preview (off by default) |
| LFI / arbitrary file read mode | Implemented | Default LFI mode on; scanner match rules for common file signatures |
| Active Scanner LFI check | Implemented | Gated by “Enable Scanner checks”; not run against remote hosts in automation |
| File-upload path scenarios | Payload only | Intruder-driven; covers in-root / out-of-root / no-traversal discovery patterns |
| Document-root lists (nix / Windows / app servers) | Implemented | Built-in lists; `{TARGET}` substitution (with known edge cases) |
| Directory checker | Partial | Driven from site-map–derived suffixes; UI list wiring has known gaps |
| Linux path strategies | Implemented | Nix slashes, `etc/passwd`-style defaults |
| Windows path strategies | Partial | Win slashes and `win.ini`; some drive / absolute-root UI controls unwired |
| Output encodings | Partial | RAW, URL (default), double-URL / UTF variants with verified quirks |
| Evasive traversals / break-up sequences | Implemented | Configurable evasive list and break bytes |
| Byte generator | Partial | Six ranges; global encoding on byte output not fully wired |
| Payload marker | Implemented | Fixed-width index for correlating uploads |
| Payload export / clipboard | Implemented | Save/copy from the UI |
| Context menu → scope update | Partial | Host propagation works; label/site-map edge cases documented |
| Modern Burp load (non-`burp` package entry) | Implemented | Entry class under the project package; display name **PathHunter** |

For limitations and defects already identified, see `docs/known-limitations.md`.

---

## Installation

**Artifact:** `dist/PathHunter.jar`

1. Open Burp Suite.
2. Go to **Extensions** → **Installed** → **Add**.
3. Extension type: **Java**.
4. Select `dist/PathHunter.jar` from this repository (or your build output).
5. Confirm the extension loads without errors.
6. Expect:
   - Extension name: **PathHunter**
   - Suite tab: **PathHunter**
   - Intruder generators: **Byte**, **Directory checker**, **Path traversal**, **Payload marker**

**Entry class (required by Burp):** `uk.co.pentest.psychoPATH.BurpExtender`

Prefer `dist/PathHunter.jar` for installation. Rebuild instructions: `docs/build-and-installation.md`. Load-failure background: `docs/burp-loading-fix.md`.

---

## Architecture

PathHunter is a single Burp entry class, a Swing configuration tab, and a shared payload engine used by Intruder, export/copy, and the optional Scanner check.

```text
Burp Suite (legacy Extender API)
  └─ BurpExtender (project package)
        ├─ Suite tab / configuration panel
        ├─ PayloadFactory ×4          (Intruder)
        │     ├─ byte generator
        │     └─ path / check / mark generators
        ├─ DirectScannerCheck         (optional active scan)
        └─ Menu                       (context menu → scope)
```

- **User-facing brand:** PathHunter (extension name and tab caption).
- **API surface:** `burp.IBurpExtender` and related legacy interfaces (not Montoya yet).
- Technical Java package and class names are retained for compatibility; see `docs/architecture.md`.

---

## Roadmap

The items below are **planned**, not implemented.

- Advanced Path Traversal Payload Engine (further variants beyond absolute paths)
- Advanced Encoding and Traversal Variants
- Linux and Windows Path Strategies (hardening and completeness)
- Improved LFI detection
- Response Analysis
- Better Intruder integration
- Improved user interface
- Defect fixes listed in `docs/known-limitations.md` and `docs/development-roadmap.md`
- Repeatable build scripting (without claiming NetBeans Ant is present)

---

## Development and contribution

| Item | Value |
| --- | --- |
| Project | PathHunter |
| Maintainer | Amir Barkhordari |
| Repository | https://github.com/Amirbarkhordari/PathHunter |
| License | GPL-3.0 (`LICENSE`) |
| Status | Active development |

Contributions should preserve GPL-3.0, keep copyright / `@author` notices on inherited files, and document substantial changes. Prefer discussing large API migrations (for example Montoya) before large refactors.

| Document | Purpose |
| --- | --- |
| [CHANGELOG.md](CHANGELOG.md) | Versioned change log |
| [docs/development-history.md](docs/development-history.md) | Phase chronology |
| [docs/project-overview.md](docs/project-overview.md) | Project identity and layout |
| [docs/architecture.md](docs/architecture.md) | Burp integration architecture |
| [docs/feature-inventory.md](docs/feature-inventory.md) | Verified feature status |
| [docs/build-and-installation.md](docs/build-and-installation.md) | Build and install |
| [docs/test-results.md](docs/test-results.md) | Automated test results |
| [docs/identity-audit.md](docs/identity-audit.md) | Branding vs attribution audit |
| [docs/github-fork-detachment.md](docs/github-fork-detachment.md) | Fork-network status |
| [docs/burp-loading-fix.md](docs/burp-loading-fix.md) | Phase 0.1 load failure analysis |
| [docs/known-limitations.md](docs/known-limitations.md) | Known defects |
| [docs/development-roadmap.md](docs/development-roadmap.md) | Planned work |
| [docs/phases/phase-2.1-absolute-path-engine.md](docs/phases/phase-2.1-absolute-path-engine.md) | Absolute path engine (Phase 2.1) |
| [docs/phases/phase-2.2-url-encoding-engine.md](docs/phases/phase-2.2-url-encoding-engine.md) | URL encoding engine (Phase 2.2) |

Regression harness sources: `tools/smoke/` (`Smoke`, `AbsolutePathSmoke`, `UrlEncodingSmoke`, LoadCheck, ProveBlock, ScanLikeBurp).

---

## License and Legal Information

PathHunter is licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE).

This repository contains modified GPL-3.0-licensed material. It does **not** claim that all historical source was authored by the current maintainer. Required provenance, copyright, and attribution details are recorded in [`NOTICE`](NOTICE) and in applicable source-file headers / `@author` tags. Those legal notices must be preserved.
