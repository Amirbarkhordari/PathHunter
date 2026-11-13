# PathHunter identity audit

Phase 1 classified branding vs attribution. **Phase 1.2** updates public presentation so PathHunter is the primary brand, while GPL-required provenance remains in `NOTICE` and source headers.

## Classification legend

1. **User-facing branding** — must present as PathHunter.
2. **Historical documentation** — may name the prior project for chronology / technical history.
3. **Copyright and license attribution** — must remain (`LICENSE`, `NOTICE`, `@author`, copyright headers).
4. **Technical package/class identifiers** — do not rename without a compatibility plan.
5. **Original binary or historical artifacts** — leave file paths/names as stored on disk.

---

## Phase 1.2 user-facing updates

| Location | Change |
| --- | --- |
| `README.md` | Removed prominent “Original project attribution” section; replaced with concise **License and Legal Information**; intro no longer advertises the prior project name |
| GitHub description | PathHunter-focused; removed “Derived from …” marketing phrasing |
| `BappDescription.html` | PathHunter product description; legal pointer to `NOTICE` / `LICENSE` without marketing the prior name |
| Extension / tab / menu / issue title | Already PathHunter (Phase 0.1 / 1) |
| `docs/project-overview.md` | PathHunter-first identity table; legal detail deferred to `NOTICE` |

---

## Remaining occurrences (why they stay)

### Copyright and license attribution (keep)

| Location | Why |
| --- | --- |
| `LICENSE` | Full GPL-3.0 text |
| `NOTICE` | Required derivative provenance (names prior project, Julian Horoszkiewicz / ewilded, URLs) |
| `README.md` → License and Legal Information | Points to `NOTICE` without false authorship claims |
| Java `@author ewilded` / `@author julian` | Inherited source attribution |
| File header comments in inherited sources | Historical / author comments |

### Technical package/class identifiers (keep)

| Identifier | Why |
| --- | --- |
| Package `uk.co.pentest.psychoPATH` | Load entry and class files |
| Classes `PsychoPATH`, `PsychoTab`, `PsychoPanel2`, `PsychoPATHScanner*`, `psychoPayload`, fields like `psychoPATHs` | Internal API; renaming is a broad refactor |
| Intruder factory keys `byte` / `check` / `path` / `mark` | Behaviour contract |
| Entry string in install docs: `uk.co.pentest.psychoPATH.BurpExtender` | Required Burp entry class name |
| `tools/smoke/*` references to those FQCNs | Harness must match runtime types |

### Original binary / historical artifacts (keep)

| Path | Why |
| --- | --- |
| `Java/psychoPATH.jar` | Untouched historical author binary; filename is the artifact name on disk |
| PDFs, screenshots, `TODO`, `perl/` | Original project materials |

### Historical documentation (keep; not marketing)

| Location | Why |
| --- | --- |
| `docs/development-history.md`, `docs/burp-loading-fix.md`, `docs/github-fork-detachment.md`, `CHANGELOG.md` | Accurate chronology of fork → PathHunter |
| `docs/architecture.md`, `docs/build-and-installation.md`, `docs/feature-inventory.md`, etc. | Technical docs that must name packages/classes and prior phases |

### Remote

| Item | Status |
| --- | --- |
| GitHub topics | PathHunter-relevant only (no prior-project topic) |
| GitHub description | PathHunter capabilities (Phase 1.2) |
| Fork network | Detached as of Phase 1.2 verification (`fork: false`) |

---

## Summary

**Public brand:** PathHunter. **Legal provenance:** `NOTICE` + headers. **Technical names:** unchanged.
