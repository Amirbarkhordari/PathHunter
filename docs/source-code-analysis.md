# Source code analysis

Fifteen Java files, one NetBeans form, one Perl script, and the fixture tree. Line references match the checkout at `e29eb6b`.

## Java inventory

### `Java/src/burp/BurpExtender.java`

Purpose: Burp load and unload entry.

Responsibilities: store callbacks, name the extension, create the tab, register four payload factories, the active scanner check, and the context menu.

Dependencies: `IBurpExtender`, `IExtensionStateListener`, `IBurpExtenderCallbacks`, `PsychoPATH`, `PsychoTab`, `PayloadFactory`, `DirectScannerCheck`, `Menu`.

Relationship: everything else is reached from this class. `getBurpCallbacks()` is how the generators obtain helpers after construction.

### `Java/src/uk/co/pentest/PsychoPATH.java`

Purpose: two static fields, `callbacks` and `PsychoTab`.

Responsibilities: process-wide access to Burp and to the panel. No generation logic.

Dependencies: `IBurpExtenderCallbacks`, `PsychoTab`.

Relationship: set by `BurpExtender` before the panel constructor runs. The panel constructor calls `PsychoPATH.callbacks.getStdout()`, so the order in `registerExtenderCallbacks` is required.

### `Java/src/uk/co/pentest/PsychoTab.java`

Purpose: Burp suite tab.

Responsibilities: caption `psychoPATH`, return `PsychoPanel2` as the UI component, and recolor the tab after `findTab()`. `findTab()` and `alertTab()` are not called from the load path.

Dependencies: `ITab`, `PsychoPanel2`.

Relationship: created once and stored on `PsychoPATH.PsychoTab`. Factories and the scanner keep this reference and read `psychoPanel`.

### `Java/src/uk/co/pentest/PsychoPanel2.java` and `PsychoPanel2.form`

Purpose: the whole configuration UI. The `.form` file is the NetBeans designer source. The `.java` file contains the generated `initComponents` method plus the hand-written handlers. The form is not compiled.

Responsibilities:

- Hold every option the generators read.
- Built-in document-root arrays: `universalDocroots`, `nginxDocroots`, `apacheDocroots`, `tomcatDocroots`, `windowsDocroots`.
- Built-in evasion units: `nonRecurrentEvasiveTraversals`, `mixedSlashEvasiveTraversals`, `breakupHolderTraversals`.
- `updateScope` for the context-menu action.
- `generatePayloads`, `saveToFileButtonActionPerformed`, and `copyToClipboardButtonActionPerformed` for export.
- List add, remove, load, paste, and clear handlers.

Dependencies: Swing, `IHttpRequestResponse`, `IScanIssue` (imported, not used as a type that changes control flow), `PsychoPATH.callbacks`.

Relationship: the generators and `DirectScannerCheck` read this object directly. There is no copy of the settings into a dedicated config class.

`payloadCount()` at line 307 returns 0.

`updateScope` at line 169 splits the hostname with `split(".")`. In Java that is a regular expression matching every character. The smoke harness called `updateScope("https", "www.example.org")` with an empty site map. The target list contained `www.example.org` and did not contain `www` or `example`.

`generatePayloads` at line 2013 constructs `new IntruderPayloadGenerator("path", PsychoPATH.PsychoTab, true)`. The `true` selects scanner file names (`winFileName` / `nixFileName`), not `fileNameField`.

### `Java/src/uk/co/pentest/PayloadFactory.java`

Purpose: one class, four registered instances, distinguished by `payloadType`.

Responsibilities: `getGeneratorName` maps `path`, `mark`, `check`, and `byte` to the Intruder labels. `createNewInstance` builds `IntruderPayloadGeneratorByte` only when `payloadType=="byte"` (reference equality). Every other type, including an unexpected string, builds `IntruderPayloadGenerator`.

Dependencies: `IIntruderPayloadGeneratorFactory`, `IIntruderAttack`, `PsychoTab`.

Relationship: created in `BurpExtender` lines 23-26. The `IIntruderAttack` argument is ignored.

### `Java/src/uk/co/pentest/IntruderPayloadGenerator.java`

Purpose: path, marker, and directory-checker engine. About 990 lines. The constructor is the product.

Responsibilities: the phases listed in `docs/architecture.md`. Public Intruder methods are `hasMorePayloads`, `getNextPayload`, and `reset`.

Dependencies: `IIntruderPayloadGenerator`, `BurpExtender.getBurpCallbacks()`, `PsychoTab`, `psychoPayload`, Swing `ListModel`.

Relationship: used by `PayloadFactory`, by `PsychoPanel2.generatePayloads`, and by `DirectScannerCheck`.

Notable methods:

| Method | Lines | Behaviour |
| --- | --- | --- |
| `addWindows` | 62-72 | Adds `..\` to `basicTraversals`. The loop adds a backslash spelling to `slashesToUse` only when that spelling is already in the list. A new list therefore never gains those spellings from this method. |
| `addUnix` | 74-84 | Adds each forward-slash spelling that is not already present, and adds `../`. |
| `getSlashes`, `getDots`, `getNullbytes` | 87-163 | Pick spellings that already occur inside a payload. `getNullbytes` is not called. Terminators are chosen later from the `psychoPayload` encoding fields. |
| constructor | 167-940 | Builds `psychopaths` or, for `check`, aliases `genericSuffixes`. |
| `byteToString` | 941-946 | One byte through `helpers.bytesToString`. |
| `hasMorePayloads` | 948-959 | Index compared with the active list. |
| `getNextPayload` | 961-983 | Marker, path, or directory string. |
| `reset` | 985-988 | Sets `payloadIndex` to 0. |

### `Java/src/uk/co/pentest/IntruderPayloadGeneratorByte.java`

Purpose: single-byte Intruder generator.

Responsibilities: fill `bytes_raw` from `PsychoPanel2.byteGeneratorRange`, convert each byte with `helpers.bytesToString`, and serve them. The encoding loop that would have applied the global output encodings is commented out at lines 173-179.

Ranges implemented:

| `byteGeneratorRange` | UI label | Bytes |
| --- | --- | --- |
| `non-alpha` | Non-alphanumeric | 0-47, 58-64, 91-96, 123-255 (194) |
| `alpha` | Alphanumeric | 48-57, 65-90, 97-122 (62). Harness count was 62, first byte `0`. |
| `non-alpha-print` | Non-alphanumeric printable | 33-46, 58-64, 91-96, 123-126 (31) |
| `non-alpha-non-print` | Non-alphanumeric non-printable | 0-31 and 127-255 (161) |
| `non-alpha-non-print-low` | Non-alphanumeric non-printable < 128 | 0-31 and 127 (33) |
| `all` | All | 0-255 (256) |

The constructor default in the panel is `non-alpha`. An unrecognised range leaves `bytes_raw` null. The harness observed `NullPointerException`.

Dependencies: `IIntruderPayloadGenerator`, callbacks, `PsychoTab`. `URLEncoder` and `StandardCharsets` are imported and unused.

### `Java/src/uk/co/pentest/psychoPayload.java`

Purpose: a payload string plus two encoding indexes.

Responsibilities: store `payload`, `encoding1`, and `encoding2`. Index 0 means raw. The terminator phase reads the indexes to decide which extra terminator spellings to append.

Dependencies: none beyond the JDK.

Relationship: built inside `IntruderPayloadGenerator` and then flattened to `List<String> psychopaths`.

### `Java/src/uk/co/pentest/SimpleDocumentListener.java`

Purpose: `DocumentListener` that forwards insert and remove to `changedUpdate`.

Responsibilities: that forwarding only. No generator reads it. A search of the other Java files shows no subclass and no reference. It is unused by the current load path.

### `Java/src/burp/DirectScannerCheck.java`

Purpose: active scan for the LFI mode.

Responsibilities: if the checkbox is selected, generate path payloads in scanner mode, send each with `callbacks.makeHttpRequest`, and on the first match against `scannerMatchRules` report `BinaryPayloadIssue`.

Dependencies: `PsychoPATHScannerCheck`, `IntruderPayloadGenerator`, `BuildUnencodedRequest`, `PsychoTab`.

Relationship: registered once from `BurpExtender`. `collabClient` is unused. Protocol is compared with `url.getProtocol()=="https"` (reference equality) at line 57.

### `Java/src/burp/PsychoPATHScannerCheck.java`

Purpose: shared scanner base.

Responsibilities: hold callbacks and helpers, build or reuse `checkHttpService`, return null from `doPassiveScan`, and leave active scan and deduplication abstract.

Dependencies: `IScannerCheck`, `IntruderPayloadGenerator` (imported and held, assigned by the subclass).

### `Java/src/burp/PsychoPATHScannerIssue.java`

Purpose: base `IScanIssue`.

Responsibilities: issue name `Arbitrary file read (psychoPATH)`, severity High, confidence Certain, issue type 0, remediation text `Don't trust user input.` Background starts as `Arbitrary file read` and appends the constructor `details` argument. The scanner currently passes an empty string.

Dependencies: `IScanIssue`, helpers, the attack `IHttpRequestResponse`.

`getHost`, `getPort`, and `getProtocol` are not implemented here. They exist on the Burp 2026 `IScanIssue` and are left abstract, so only a concrete subclass can be instantiated.

### `Java/src/burp/BinaryPayloadIssue.java`

Purpose: the concrete issue raised by the scanner.

Responsibilities: issue detail `The target is vulnerable to Arbitrary file read (also improperly called LFI)`, plus `appendIssueDetail`. `getHost`, `getPort`, and `getProtocol` are overridden and throw `UnsupportedOperationException`. Those three methods are deprecated on the Burp 2026 API. `javac -Xlint:deprecation` reported that.

### `Java/src/burp/BuildUnencodedRequest.java`

Purpose: put raw payload bytes into a scanner insertion point.

Responsibilities: build a same-length alphanumeric canary, ask the insertion point to build a request, find exactly one canary with `helpers.indexOf`, and copy the payload over it.

Dependencies: `IExtensionHelpers`, `IScannerInsertionPoint`, `Random`.

Relationship: used only by `DirectScannerCheck`. The comment cites a PortSwigger support snippet for avoiding automatic encoding.

### `Java/src/burp/Menu.java`

Purpose: context menu item `Propagate to psychoPATH`.

Responsibilities: on mouse release, read host and protocol from the first selected message and call `updateScope`. Click, press, enter, and exit are empty.

Dependencies: `IContextMenuFactory`, `PsychoPATH`.

Relationship: this is the only bridge from the site map into target names and directory suffixes.

## Build and description files

| File | What it actually contains |
| --- | --- |
| `Java/build.xml` | NetBeans project `psychoPATH`. The only real target behaviour is `<import file="nbproject/build-impl.xml"/>`. That file is absent, so Ant cannot run this script. |
| `Java/manifest.mf` | `Manifest-Version: 1.0` and a comment that `Main-Class` will be added by the build. |
| `Java/psychoPATH.jar` | Original binary. Manifest `Created-By: 1.8.0_171`, `Ant-Version: Apache Ant 1.9.6`, `Main-Class: uk.co.pentest.psychoPATH.PsychoPATH`, `Class-Path: lib/burpsuite_pro.jar lib/beansbinding-1.2.1.jar`. Class version 52. 70 classes, including `PsychoPanel2$47` through `$54`, which the current source does not emit. |
| `BappDescription.html` | Short description of upload and LFI cases and a link to the original repository. |

The source does not import `org.jdesktop.beansbinding`. The `Class-Path` entry is a leftover from the NetBeans project. The `lib/` directory is not in the repository.

## Perl predecessor

| File | Role |
| --- | --- |
| `perl/README` | States the script is no longer maintained. |
| `perl/get_docroots.pl` | Prints document-root guesses. Configuration is hard-coded: filename `test.png`, targets `uploadbare` and `localhost`, traversals on, evasive units on. Prefix lists match the Java arrays for universal, nginx, Apache, and Tomcat paths. Suffixes include `html`, `htdocs`, `httpdocs`, `php`, `public`, `src`, `site`, `build`, `web`, `data`, `sites/all`, `www/build`, plus each target. |
| `perl/sample_results.txt` | Sample stdout from that script. 415605 bytes. Not parsed by the Java extension. |

The Java UI does not preload those Perl suffixes. `suffixesList` starts empty. Targets are copied into the suffix set at generation time, and the empty suffix is always added.

## Fixtures

`test_cases/LFI/` holds eleven PHP scripts. They are examples of filters, not a test runner. PHP is not installed on this machine, so they were not executed.

| File | Filter the source implements |
| --- | --- |
| `one.php` | No filter. Reads `./` plus `$_GET['file']`. |
| `two.php` | Removes `./`, then reads. |
| `three.php` | Removes `../`. |
| `four.php` | Removes `../` and `..\`. |
| `eleven.php` | Removes `../`, then `./`. |
| `five.php` | Removes `../`, then URL-decodes. |
| `six.php` | Removes `./`, then URL-decodes. |
| `ten.php` | Removes `..`, then URL-decodes. |
| `seven.php` | Loop intended to be recursive `../` removal, then URL-decode. The condition is `stripos($fname,"../"!=FALSE)`, which does not search for `../` inside the file name. |
| `eight.php` | Appends `.txt`, splits on spaces, and reads `../` plus the original string. |
| `nine.php` | Removes `..`, then spaces. |

`test_cases/uploadbare_traversal_outside_webroot/` is a small Tomcat application. `UploadServlet` writes to `/tmp` plus the client file name. `do_war.sh` builds the war. A `.war` and two `.class` files are already in the tree.

`test_cases/uploadbare_traversal_inside_webroot/UploadServlet.java` uses save directory `/var/lib/tomcat8/webapps/uploadbare_traversal_inside_webroot/nowaytofindme/tmp/`.

`test_cases/no_traversal_unusual_webroot/UploadServlet.java` uses `/var/lib/tomcat8/webapps/no_traversal_unusual_webroot/logs/tmp`. The `@WebServlet` value in that file is still `/uploadbare_traversal_outside_webroot`.

## UI controls that do not change generator state

Confirmed by reading `PsychoPanel2.java`:

- `useAbsoluteWebrootsBox` has no action listener. `useAbsoluteWebroots` stays `true` unless some other code assigns it. The smoke harness assigned the field directly.
- `jButton16` (Remove) and `jButton17` (Clear) on the drive list have no action listeners.
- `jCheckBox10ActionPerformed` is empty.
- `SimpleDocumentListener` has no callers.

## Encoding tables

When `urlEncodeUtf` is not selected, index 2 of the dot table is `bytesToString(0x25) + bytesToString(0x32) + bytesToString(0x65)`, which is the three characters `%2e`. Index 1 is already the literal `%2e`. The harness produced `%2e%2e%2f...` and did not produce `%25` in that mode.

When `urlEncodeUtf` is selected, index 2 is the literal `%25%32%65`. The harness produced `%25%32%65%25%32%65%25%32%66%25%32%65%25%32%65%25%32%66etc%25%32%66passwd`.

Index meanings in both tables are: 0 raw, 1 URL, 2 double URL, 3 UTF-16 style (`%u002e` / `%u2215` / `%u2216`), 4 overlong-2, 5 overlong-3.
