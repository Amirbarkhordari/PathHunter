# PathHunter project overview

PathHunter is an independently maintained Burp Suite extension for path traversal, LFI, and file-upload path testing. It is a **GPL-3.0 derivative** of psychoPATH by Julian Horoszkiewicz.

This document describes the checked-out history at `e29eb6b108db004c5e301459233ea2343ba5b19d` on `master`, plus local Phase 0 / 0.1 / 1 work that is **not yet committed**.

## Identity

| Item | Verified value |
| --- | --- |
| Project name | **PathHunter** |
| Maintainer | Amir Barkhordari |
| Personal repository | https://github.com/Amirbarkhordari/PathHunter |
| Original project | psychoPATH — https://github.com/ewilded/psychoPATH |
| Original author | Julian Horoszkiewicz (`ewilded`) |
| GitHub fork relationship | Still a fork of `ewilded/psychoPATH` until Leave fork network is performed manually (see `docs/github-fork-detachment.md`) |
| License | GPL-3.0 (`LICENSE`); derivative notice in `NOTICE` |
| Primary language | Java |
| Host product | Burp Suite, legacy Extender API (`burp.IBurpExtender`) |
| Extension display name | **PathHunter** (`BurpExtender.setExtensionName`) |
| Suite tab caption | **PathHunter** (`PsychoTab.getTabCaption`) |
| Java package (technical) | `uk.co.pentest.psychoPATH` — retained; not user-facing branding |
| Distribution artifact | `dist/PathHunter.jar` |
| Entry class | `uk.co.pentest.psychoPATH.BurpExtender` |
| Original author jar (untouched) | `Java/psychoPATH.jar` |

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
| `README.md` | PathHunter project identity, features, install, roadmap |
| `NOTICE` | Derivative attribution (psychoPATH → PathHunter) |
| `LICENSE` | GPL-3.0 |
| `TODO` | Original author's unfinished work (2018) |
| `BappDescription.html` | Historical Burp App Store description for psychoPATH |
| `Java/src/uk/co/pentest/` | Extension source (`package uk.co.pentest.psychoPATH`) |
| `Java/build.xml` | NetBeans Ant wrapper (imports missing `nbproject/`) |
| `Java/psychoPATH.jar` | Prebuilt extension from the original author (left untouched) |
| `perl/` | Unmaintained predecessor |
| `test_cases/` | Vulnerable sample fixtures |
| `screenshots/` | UI images from the original project |
| `usage_examples.pdf` | Original walkthrough |
| `docs/` | PathHunter assessment and maintenance docs |

Generated / local (uncommitted at Phase 1):

| Path | Role |
| --- | --- |
| `dist/PathHunter.jar` | Rebuild from current sources |
| `build/` | Class files, compile log, smoke harness |
| `docs/` | Assessment and Phase 1 identity docs |

## Intruder payload generators

`BurpExtender.registerExtenderCallbacks` registers four factories:

| Factory key | Name shown in Intruder | Implementation |
| --- | --- | --- |
| `byte` | Byte | `IntruderPayloadGeneratorByte` |
| `check` | Directory checker | `IntruderPayloadGenerator` with `payloadType=check` |
| `path` | Path traversal | `IntruderPayloadGenerator` with `payloadType=path` |
| `mark` | Payload marker | `IntruderPayloadGenerator` with `payloadType=mark` |

## Default behaviour when the UI is created

Taken from `PsychoPanel2()` and confirmed by the smoke harness where noted:

- LFI mode is on. Web-root lists are not applied until LFI mode is turned off.
- Longest-traversal optimisation for LFI is on.
- Evasive list starts as `....//`, `...//`, `.....///`, `.{BREAK}.{BREAK}/`.
- Break-up sequence starts as hex `20` (a space).
- RAW and URL encodings are on. Double URL, UTF-16, and both overlong UTF ranges start off.
- Mixed encodings are on.
- Terminators are on. The literal NUL byte starts off, so the default terminator is the characters `%00`.
- Intruder file name is `etc/passwd`. Scanner names are `etc/passwd` (nix) and `win.ini` (win).
- Scanner match rules are `root:x:`, `root:$`, and `[mci extensions]`.
- Target OS combo is `all`, `win`, `nix`, with `all` first.
- Directory separator combo is `all`, then Windows, then nix. The field `slashesToUse` starts as `all`.
- One target is preloaded: `example.org`.
- Document roots preloaded are the universal set plus part of the Apache set.
- The relative-directory list (`suffixesList`) is created empty.
- Windows drive list starts as `C`.
- Maximum traversal repeat count is 18.

## Phase boundaries

- **Phase 0:** Inspect, rebuild, document, smoke-test. No new detection features.
- **Phase 0.1:** Move implementation classes out of `package burp` so modern Burp can load the jar; brand extension/tab as PathHunter; artifact `dist/PathHunter.jar`.
- **Phase 1:** Independent project identity, documentation, license/attribution review, fork-detachment *guidance* only. No commit/push unless explicitly approved. No Leave fork network automation.
