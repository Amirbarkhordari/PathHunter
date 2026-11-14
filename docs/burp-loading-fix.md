> **Canonical documentation:** [PATHHUNTER_MASTER_DOCUMENTATION.md](PATHHUNTER_MASTER_DOCUMENTATION.md).
> This file is supplemental/historical. Prefer the master document if details differ.

# Burp loading failure – Phase 0.1

## Original error

When loading `dist\psychoPATH.jar` (Phase 0 artifact) in Burp Suite Professional 2026.4.3:

```text
java.lang.Exception: Extension class is not a recognized type
    at burp.Zojj.ZN(Unknown Source)
    at burp.Zojj.Zi(Unknown Source)
    at burp.Zb3q.ZU(Unknown Source)
    at burp.Zvwh.ZQ(Unknown Source)
    at burp.Zlmo.lambda$load$2(Unknown Source)
```

## Root cause

Burp 2026.4.3 discovers Java extensions by scanning the JAR (`burp.Zojj.Zk`) and asking `burp.Zut_.Zp` whether each concrete class is assignable to:

- `burp.api.montoya.BurpExtension`, or
- `burp.IBurpExtender`

If none qualify, `Zojj.ZN` throws **Extension class is not a recognized type**.

The extension classloader is `burp.Zv_d` (a `URLClassLoader`) with parent `burp.Zzq3`. Decrypted policy strings from those classes:

| Rule | Value |
| --- | --- |
| Scan only `*.class` | `.class` |
| Skip | names containing `module-info` or `META-INF` |
| Parent-first prefixes (`Zv_d.Zu`) | includes `burp.`, `java.`, `javax.`, … |
| Parent block (`Zzq3.Zr`) | any `burp.*` name that does **not** start with `burp.api.montoya` and whose suffix after `burp.` is **not** in allowlist `Zv_d.ZA` |
| Allowlist `ZA` | only legacy API **interfaces** (`IBurpExtender`, `ITab`, `IScannerCheck`, …) — 42 names |

So for a class named `burp.BurpExtender` (and `burp.Menu`, `burp.DirectScannerCheck`, …):

1. `Zv_d` treats `burp.*` as parent-first.
2. `Zzq3` **rejects** non-allowlisted `burp.*` implementation types with `ClassNotFoundException`.
3. Discovery then depends on `URLClassLoader` falling through to `findClass` on the extension JAR after that parent failure.

That is fragile under Burp 2026’s isolation model: implementation types are not supposed to live in `package burp`. Only the API interfaces are allowlisted.

Evidence gathered on this machine:

- `DumpSets` printed `Zu`, `ZA`, and `Zzq3.Zr("burp.BurpExtender") == true` (parent block).
- `DecryptBurp` recovered the exact error string and filters above from `burp.Zojj`.
- Legacy API is still present and still checked (`IBurpExtender` exists; PortSwigger docs still describe legacy Extender examples). A Montoya migration was **not** required.

## Fix applied

Relocate **all project implementation classes** out of `package burp` into `package uk.co.pentest.psychoPATH`, while continuing to **import** the allowlisted `burp.*` API interfaces from Burp at runtime.

| Before | After |
| --- | --- |
| `burp.BurpExtender` | `uk.co.pentest.psychoPATH.BurpExtender` |
| `burp.Menu` | `uk.co.pentest.psychoPATH.Menu` |
| `burp.DirectScannerCheck` | `uk.co.pentest.psychoPATH.DirectScannerCheck` |
| `burp.BinaryPayloadIssue` | `uk.co.pentest.psychoPATH.BinaryPayloadIssue` |
| `burp.BuildUnencodedRequest` | `uk.co.pentest.psychoPATH.BuildUnencodedRequest` |
| `burp.PsychoPATHScannerCheck` | `uk.co.pentest.psychoPATH.PsychoPATHScannerCheck` |
| `burp.PsychoPATHScannerIssue` | `uk.co.pentest.psychoPATH.PsychoPATHScannerIssue` |

Burp’s scanner finds `uk.co.pentest.psychoPATH.BurpExtender` because it walks every `.class` entry, not only `burp.BurpExtender`.

Also set:

- Extension name: `PathHunter`
- Suite tab caption: `PathHunter`

No Montoya rewrite. No Burp API classes packaged into the JAR. `Java/psychoPATH.jar` was not overwritten.

## Files modified / created

Modified / moved under `Java/src`:

- Moved 7 types from `Java/src/burp/` → `Java/src/uk/co/pentest/` with package + API imports updated
- `IntruderPayloadGenerator.java`, `IntruderPayloadGeneratorByte.java` — drop `import burp.BurpExtender`
- `PsychoPanel2.java` — drop obsolete `burp.*` implementation imports
- `PsychoTab.java` — tab caption `PathHunter`
- Removed empty `Java/src/burp/` directory

Build / test / docs:

- `dist/PathHunter.jar` (new final artifact)
- `build/` (classes, smoke harness updates, logs)
- `docs/burp-loading-fix.md` (this file)
- updates to `docs/build-and-installation.md`, `docs/environment-and-dependencies.md`, `docs/test-results.md`, `docs/known-limitations.md`

## Build commands

```text
javac --release 8 -encoding UTF-8 -Xlint:-options ^
  -cp C:\Users\Amir\Documents\Burp.Suite.Professional.2026.4.3\burpsuite_desktop_v2026.4.3.jar ^
  -d build\classes ^
  Java\src\uk\co\pentest\*.java

jar --create --file dist\PathHunter.jar --manifest build\MANIFEST.MF -C build\classes .
```

Compile exit code: **0**. Artifact class version: **52** (Java 8).

## Test results

| Suite | Result |
| --- | --- |
| Compilation | Pass (notes only: deprecation / unchecked) |
| JAR integrity / listing | Pass — entry `uk/co/pentest/psychoPATH/BurpExtender.class`; no `burp/I*.class`; no Montoya API classes |
| `LoadCheck` (parent Burp + child extension) | Pass — 11/11 |
| `ProveBlock` (real `burp.Zv_d`) | Pass — old `burp.BurpExtender` blocked/absent; new entry loads and is `IBurpExtender` |
| `ScanLikeBurp` (same filters as `Zojj.Zk`) | Pass — exactly one candidate, instantiable |
| Phase 0 `Smoke` payload harness | Pass — 29/29 |
| Burp GUI load | **Not verified in this phase** (manual step below) |

## Remaining limitations

- Burp GUI loading was not executed here (launcher uses a third-party keygen JAR; not started).
- Do not unload an existing psychoPATH / BApp copy unless you choose to.
- Payload / `updateScope` defects from Phase 0 are unchanged.
- `BinaryPayloadIssue.getHost/getPort/getProtocol` still throw.

## Manual verification

1. Open Burp Suite Professional 2026.4.3.
2. Extensions → Add → Java.
3. Select `D:\Projects\PathHunter\dist\PathHunter.jar`.
4. Leave any existing psychoPATH extension alone unless you intend to replace it.
5. Confirm the extension appears as **PathHunter** with no error dialog.
6. Confirm a suite tab **PathHunter**.
7. Confirm Intruder → Extension-generated payloads: Byte, Directory checker, Path traversal, Payload marker.
8. Read the extension Output and Errors tabs for initialization exceptions.
