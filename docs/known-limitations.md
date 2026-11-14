# Known limitations

These are properties of the current source. They were not changed in this phase. "Observed" means the smoke harness hit them. "Source" means they were read in the code and not executed as a separate case.

## Build and binary drift

- `nbproject/` is missing, so `Java/build.xml` cannot build the project.
- `lib/burpsuite_pro.jar` and `lib/beansbinding-1.2.1.jar` from the original manifest are missing. Beans binding is not referenced by the source.
- `Java/psychoPATH.jar` contains `PsychoPanel2$47` through `$54`. The current `PsychoPanel2.java` compiles to `$1` through `$46` only. The checked-in jar is not a build of the checked-in source.
- Maven `burp-extender-api` 2.3 is missing `IScanIssue.getHost`, `getPort`, and `getProtocol`, which `BinaryPayloadIssue` overrides. A compile against that jar fails. The Burp 2026 desktop jar has the methods and marks them deprecated.

## API and installation

- The extension is the legacy IBurpExtender API. Entry point: uk.co.pentest.psychoPATH.BurpExtender (not urp.BurpExtender).
- Burp 2026.4.3 blocks non-allowlisted types in package burp at the extension parent classloader. See docs/burp-loading-fix.md.
- extensionUnloaded is empty.
- BinaryPayloadIssue.getHost, getPort, and getProtocol throw UnsupportedOperationException.
- IBurpCollaboratorClientContext collabClient is unused.
- Burp GUI loading of dist/PathHunter.jar still requires manual verification.

## Payload correctness

- `updateScope` uses `hostname.split(".")`. The harness kept `www.example.org` and did not add `www` or `example`. A null site map throws `NullPointerException` at line 190 (observed).
- `addWindows` adds a backslash spelling to `slashesToUse` only when that spelling is already present (source, lines 64-70). Literal `..\` is still added to `basicTraversals`. The harness observed `..\win.ini`.
- With "URL-encode UTF sequences" off, the double-URL slot is the characters `%2e`, not `%25%32%65` (observed). The `%25...` form appears when that checkbox is on (observed).
- The byte generator's encoding loop is commented out (source, lines 173-179). An unknown `byteGeneratorRange` throws `NullPointerException` (observed).
- `PayloadFactory.createNewInstance` uses `payloadType=="byte"` (source). The four factories are constructed with string literals, and `getGeneratorName` was observed. The `==` dispatch itself was not called by the harness.
- `getNullbytes` is unused. Terminators are chosen from `%00`, an optional literal NUL, and the encoding indexes stored on `psychoPayload` (source).
- Directory payloads for the checker come from `genericSuffixes`, not from `suffixesList` (observed). `TODO` describes a related stale-list bug.
- `getNextPayload` on an empty checker throws `IndexOutOfBoundsException` (observed).
- Export uses `new IntruderPayloadGenerator("path", tab, true)`, so it uses scanner file names rather than `fileNameField` (source).
- `useAbsoluteWebrootsBox` has no listener. The boolean `useAbsoluteWebroots` stays true (source). The harness had to assign the field to turn absolute roots off.
- Drive list Remove and Clear (`jButton16`, `jButton17`) have no listeners (source).
- `payloadCount()` returns 0 (source).
- `DirectScannerCheck` compares the protocol with `== "https"` (source).
- Web-root payloads include an empty suffix, so the harness sample contained `..//var/www/app//a.jpg` (observed).
- Windows LFI output contained `..\win.ini` twice in a three-item list (observed).
- `SimpleDocumentListener` has no callers (source).
- `addFilePrefix` is never read, so `file:///` prefixes from the TODO are not generated (source).
- Absolute path payloads (Phase 2.1) are opt-in via the Absolute paths tab. They are not a separate Intruder generator factory; they augment Path traversal when enabled. PortSwigger Academy absolute-path lab success was not verified in automation.

## Scanner

- The active check sends one request per path payload and stops at the first literal match. It does not follow redirects (`TODO`).
- Match rules are raw substrings. Defaults are `root:x:`, `root:$`, and `[mci extensions]`.
- `BuildUnencodedRequest` throws if the canary is missing or duplicated. The scanner catches that, logs it, and can then call `makeHttpRequest` with a null request (source).
- Issue type is 0. Severity is hard-coded High and confidence Certain.

## Fixtures

- `test_cases/LFI/seven.php` uses `stripos($fname,"../"!=FALSE)`, which does not search the file name for `../`.
- `test_cases/no_traversal_unusual_webroot/UploadServlet.java` is annotated `@WebServlet("/uploadbare_traversal_outside_webroot")` while its save directory is the no-traversal path.
- The Perl script and the Java UI have drifted. The Perl suffix list is not the Java default list.

## Product scope that the README states and the code does not automate

- Detecting a successful upload still requires a separate Intruder position for the marker and a later directory-checker pass. Nothing in the scanner stores an uploaded file and then fetches it.
- ZIP slip, extension-filter bypass, and automatic identification of the winning payload are listed in `TODO` and are not in the generator.
