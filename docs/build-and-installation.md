# PathHunter build and installation

## Why the original build script was not used

`Java/build.xml` imports `nbproject/build-impl.xml`. That file is not in the repository. Ant is not installed. Maven and Gradle files do not exist, and none were created. The sources are compiled with `javac` from the installed JDK.

The Burp API jar is not in the repository. Maven Central `burp-extender-api` 2.3 does not declare `IScanIssue.getHost`, `getPort`, or `getProtocol`. `BinaryPayloadIssue` overrides those methods, and Burp 2026.4.3 declares them. The compile classpath is therefore the installed desktop jar.

## Package layout

All extension **implementation** classes live in `package uk.co.pentest.psychoPATH` under `Java/src/uk/co/pentest/`. They import allowlisted `burp.*` **API interfaces** from Burp at runtime. No project class remains in `package burp`. See `docs/burp-loading-fix.md`.

The Java package name under `uk.co.pentest.*` is a **technical identifier** retained for compatibility. The user-facing product name is **PathHunter**.

## Build commands

Working directory `D:\Projects\PathHunter`. JDK `C:\Program Files\Java\jdk-24\bin\javac.exe`.

```text
javac --release 8 -encoding UTF-8 -Xlint:-options ^
  -cp C:\Users\Amir\Documents\Burp.Suite.Professional.2026.4.3\burpsuite_desktop_v2026.4.3.jar ^
  -d build\classes ^
  Java\src\uk\co\pentest\*.java ^
  Java\src\uk\co\pentest\fileupload\*.java

jar --create --file dist\PathHunter.jar --manifest build\MANIFEST.MF -C build\classes .
```

Phase 4.1 added the `fileupload` subpackage; both globs are required.

Compile exit code 0. Notes only: deprecated API and unchecked operations.

`javap` on `uk.co.pentest.psychoPATH.BurpExtender` reports major version **52**.

## Artifact

| Item | Value |
| --- | --- |
| Canonical path | `dist/PathHunter.jar` |
| Size (Phase 0.1 rebuild) | 73696 bytes |
| Entry class | `uk.co.pentest.psychoPATH.BurpExtender` |
| Implements | `burp.IBurpExtender`, `burp.IExtensionStateListener` |
| Bundled Burp API classes | None |
| Extension display name | PathHunter |
| Suite tab | PathHunter |
| Historical jar (unchanged) | `Java/psychoPATH.jar` (filename on disk; not the PathHunter distribution artifact) |

## Install in Burp Suite

1. Burp → **Extensions** → **Add** → type **Java**.
2. Select `D:\Projects\PathHunter\dist\PathHunter.jar` (or your rebuilt copy).
3. Prefer a single PathHunter load; avoid loading a second historical jar unless comparing behaviour.
4. Expect extension name **PathHunter**, tab **PathHunter**, and Intruder generators Byte / Directory checker / Path traversal / Payload marker.
5. Read the extension Output and Errors tabs.

**GUI load should be confirmed manually** after each rebuild. Automation verified jar structure and a local smoke harness, not a full Burp desktop session in every phase.

Details of the Phase 0 load failure and fix: `docs/burp-loading-fix.md`.
