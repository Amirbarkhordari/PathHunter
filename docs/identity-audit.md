# PathHunter identity audit (Phase 1)

Classification of references to psychoPATH / original authorship found in the tree (excluding `build/` class outputs). Do **not** blindly rename every `psychoPATH` string.

## Classification legend

1. **Branding** — should present as PathHunter (user-facing names, docs titles, JAR name, GitHub description).
2. **Historical attribution** — must remain (author names, original repo URLs in attribution sections, derivative acknowledgements).
3. **License / copyright** — must remain unchanged unless a later approved license change occurs (`LICENSE`, FSF header text, inherited copyright/`@author` lines).
4. **Technical identifiers** — do not rename in Phase 1 without a compatibility plan (Java packages, class names, Intruder factory keys, prebuilt `Java/psychoPATH.jar`).

---

## Branding → PathHunter

| Location | Finding | Phase 1 action |
| --- | --- | --- |
| `README.md` | Entire document was psychoPATH-branded | Replaced with PathHunter README + attribution section |
| Extension display name | Was `psychoPATH Extension`; now `PathHunter` in `BurpExtender` | Keep PathHunter |
| Suite tab caption | Was `psychoPATH`; now `PathHunter` in `PsychoTab` | Keep PathHunter |
| Context menu item | Was `Propagate to psychoPATH`; now `Propagate to PathHunter` | Keep PathHunter |
| Scanner issue name | Was `Arbitrary file read (psychoPATH)`; now `(PathHunter)` | Keep PathHunter |
| Distribution JAR | `dist/PathHunter.jar` | Canonical artifact name |
| Docs under `docs/` | Mixed Phase 0 text still said psychoPATH as program name | Updated to PathHunter with attribution |
| GitHub repository description | Still psychoPATH marketing text via fork inherit | **Manual** Settings update (not done automatically) |
| `BappDescription.html` | Points only at ewilded/psychoPATH | Left as historical App Store text; update when republishing under PathHunter |

## Historical attribution → keep

| Location | Finding |
| --- | --- |
| `NOTICE` | Explicit psychoPATH → PathHunter derivative statement |
| `README.md` attribution section | Julian Horoszkiewicz, ewilded/psychoPATH, GPL-3.0 |
| Git `@author` / historical README authorship | ewilded / julian |
| PDFs / screenshots / logo | Original project materials; keep |
| `docs/*` historical notes | Technical history of the fork and Phase 0/0.1 work |

## License / copyright → unchanged

| Location | Finding |
| --- | --- |
| `LICENSE` | Full GPL-3.0 text preserved |
| GitHub `license.spdx_id` | `GPL-3.0` |
| Source `@author ewilded` / `@author julian` | Preserved on inherited files |
| No wholesale author rewrite | PathHunter maintainer named only in README / NOTICE / docs |

## Technical identifiers → do not rename (Phase 1)

| Identifier | Why keep |
| --- | --- |
| Package `uk.co.pentest.psychoPATH` | Load entry, class files, Intruder registration; rename is a broad refactor |
| Classes `PsychoPATH`, `PsychoTab`, `PsychoPanel2`, `PsychoPATHScanner*`, etc. | Internal API; high risk / no user benefit in Phase 1 |
| Intruder factory type keys `byte` / `check` / `path` / `mark` | Behaviour contract |
| `Java/psychoPATH.jar` | Original author binary kept untouched |
| `Java/build.xml` project name | NetBeans leftover; unused for current javac build |
| Source paths under `Java/src/uk/co/pentest/` | Physical layout; packages remain psychoPATH |

## Old URLs

| URL | Classification |
| --- | --- |
| https://github.com/ewilded/psychoPATH | Attribution / upstream remote — keep |
| https://github.com/Amirbarkhordari/PathHunter | Current project — primary |
| App Store / Bapp link to ewilded | Historical until BApp republish |

## Summary decision

PathHunter is the **user-facing and documentation identity**. psychoPATH remains in **legal attribution**, **history**, and **Java technical namespaces**. GPL-3.0 and original authorship are not replaced.
