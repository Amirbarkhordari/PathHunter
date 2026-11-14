# Test results

No JUnit suite exists in the repository. PHP and Perl are not installed, so fixtures under `test_cases/` and `perl/` were not executed. No request was sent to an external host. Burp GUI Extender load was **not** opened in automation.

Harness sources: `tools/smoke/` (`LoadCheck`, `ProveBlock`, `ScanLikeBurp`, `Smoke`, `AbsolutePathSmoke`, `UrlEncodingSmoke`, `FileUploadAnalyzerSmoke`, `FilenameTestingSmoke`, `MimeValidationSmoke`, `FileContentValidationSmoke`).

---

## Phase 4.4 — 2026-11-14 (authoritative for current tree)

**Artifact:** `dist/PathHunter.jar` (278623 bytes after clean rebuild)

| Check | Expected | Actual | Status |
| --- | --- | --- | --- |
| Compilation `--release 8` | Exit 0 | Exit 0 | Pass |
| JAR contains `FileContentValidationEngine` | Present | Present | Pass |
| No bundled `burp/` API classes | 0 | 0 | Pass |
| Bytecode major version | 52 | 52 | Pass |
| FileContentValidationSmoke | 91 pass / 0 fail | **91/91** (+2 SKIP) | Pass |
| MimeValidationSmoke | 78/78 | **78/78** (+2 SKIP) | Pass |
| FilenameTestingSmoke | 80/80 | **80/80** (+2 SKIP) | Pass |
| FileUploadAnalyzerSmoke | 80/80 | **80/80** (+2 SKIP) | Pass |
| UrlEncodingSmoke | 31/31 | **31/31** | Pass |
| AbsolutePathSmoke | 33/33 | **33/33** | Pass |
| Smoke | 29/29 | **29/29** | Pass |
| LoadCheck | 11/11 | **11/11** | Pass |
| ProveBlock | Pass | Pass | Pass |
| ScanLikeBurp | 1 candidate | 1 candidate | Pass |
| Burp GUI Content testing | Manual | Not run | NOT RUN |
| PortSwigger file-upload labs | Manual | Not run | NOT RUN |

Evidence: `build/filecontentvalidation-results.txt`.

---

## Phase 4.3 — 2026-11-14 (historical)

**Artifact:** `dist/PathHunter.jar` (231674 bytes after clean rebuild)

| Check | Expected | Actual | Status |
| --- | --- | --- | --- |
| Compilation `--release 8` | Exit 0 | Exit 0 | Pass |
| JAR contains `MimeValidationEngine` | Present | Present | Pass |
| No bundled `burp/` API classes | 0 | 0 | Pass |
| Bytecode major version | 52 | 52 | Pass |
| MimeValidationSmoke | 78 pass / 0 fail | **78/78** (+2 SKIP) | Pass |
| FilenameTestingSmoke | 80/80 | **80/80** (+2 SKIP) | Pass |
| FileUploadAnalyzerSmoke | 80/80 | **80/80** (+2 SKIP) | Pass |
| UrlEncodingSmoke | 31/31 | **31/31** | Pass |
| AbsolutePathSmoke | 33/33 | **33/33** | Pass |
| Smoke | 29/29 | **29/29** | Pass |
| LoadCheck | 11/11 | **11/11** | Pass |
| ProveBlock | Pass | Pass | Pass |
| ScanLikeBurp | 1 candidate | 1 candidate | Pass |
| Burp GUI MIME testing | Manual | Not run | NOT RUN |
| PortSwigger Content-Type lab | Manual | Not run | NOT RUN |

Evidence: `build/mime-validation-smoke-results.txt`.

---

## Phase 4.2 — 2026-11-14 (historical)

**Artifact:** `dist/PathHunter.jar` (185053 bytes after clean rebuild)

| Check | Expected | Actual | Status |
| --- | --- | --- | --- |
| Compilation `--release 8` | Exit 0 | Exit 0 | Pass |
| JAR contains `FilenameTestingEngine` | Present | Present | Pass |
| No bundled `burp/` API classes | 0 | 0 | Pass |
| Bytecode major version | 52 | 52 | Pass |
| FilenameTestingSmoke | 80 pass / 0 fail | **80/80** (+2 SKIP) | Pass |
| FileUploadAnalyzerSmoke | 80/80 | **80/80** (+2 SKIP) | Pass |
| UrlEncodingSmoke | 31/31 | **31/31** | Pass |
| AbsolutePathSmoke | 33/33 | **33/33** | Pass |
| Smoke | 29/29 | **29/29** | Pass |
| LoadCheck | 11/11 | **11/11** | Pass |
| ProveBlock | Pass | Pass | Pass |
| ScanLikeBurp | 1 candidate | 1 candidate | Pass |
| Burp GUI filename testing | Manual | Not run | NOT RUN |
| PortSwigger file-upload labs | Manual | Not run | NOT RUN |

Evidence: `build/filename-testing-smoke-results.txt`.

---

## Phase 4.1 — 2026-11-14 (historical)

**Artifact:** `dist/PathHunter.jar` (141838 bytes after clean rebuild)

| Check | Expected | Actual | Status |
| --- | --- | --- | --- |
| Compilation `--release 8` | Exit 0 | Exit 0 | Pass |
| JAR contains `fileupload.*` | Present | Present | Pass |
| No bundled `burp/` API classes | 0 | 0 | Pass |
| Bytecode major version | 52 | 52 | Pass |
| FileUploadAnalyzerSmoke | 80 pass / 0 fail | **80/80** (+2 SKIP) | Pass |
| UrlEncodingSmoke | 31/31 | **31/31** | Pass |
| AbsolutePathSmoke | 33/33 | **33/33** | Pass |
| Smoke | 29/29 | **29/29** | Pass |
| LoadCheck | 11/11 | **11/11** | Pass |
| ProveBlock | Pass | Pass | Pass |
| ScanLikeBurp | 1 candidate | 1 candidate | Pass |
| Burp GUI / context-menu live | Manual | Not run | NOT RUN |
| PortSwigger file-upload labs | Live parse check | Not run | NOT RUN |

Evidence: `build/fileupload-smoke-results.txt`, `build/loadcheck-results.txt`.

---

## Phase 2.2 — 2026-11-13 (historical)

**Artifact:** `dist/PathHunter.jar` (102394 bytes after clean rebuild)

| Check | Expected | Actual | Status |
| --- | --- | --- | --- |
| Compilation `--release 8` | Exit 0 | Exit 0 | Pass |
| JAR contains `UrlEncodingEngine` | Present | Present | Pass |
| UrlEncodingSmoke | All cases | **31/31** | Pass |
| AbsolutePathSmoke | 33/33 | **33/33** | Pass |
| Smoke | 29/29 | **29/29** | Pass |
| LoadCheck | 11/11 | **11/11** | Pass |
| ProveBlock | Pass | Pass | Pass |
| ScanLikeBurp | 1 candidate | 1 candidate | Pass |
| PortSwigger superfluous URL-decode lab | Live solve | Not run | Manual only |

Evidence: `build/urlencoding-smoke-results.txt`.

---

## Phase 2.1 — 2026-11-13 (historical)

**Artifact:** `dist/PathHunter.jar` (85088 bytes after clean rebuild)

| Check | Expected | Actual | Status |
| --- | --- | --- | --- |
| Compilation `--release 8` | Exit 0 | Exit 0 | Pass |
| JAR contains `AbsolutePathEngine` | Present | Present | Pass |
| Entry `BurpExtender` / no bundled Burp API | Pass | Pass | Pass |
| AbsolutePathSmoke | All absolute-path cases | **33/33** | Pass |
| Smoke regression | 29/29 | **29/29** | Pass |
| LoadCheck | 11/11 | **11/11** | Pass |
| ProveBlock | Pass | Pass | Pass |
| ScanLikeBurp | 1 candidate | 1 candidate | Pass |
| PortSwigger Academy lab | Live solve | Not run | Manual only |

Evidence: `build/absolute-smoke-results.txt`, `build/smoke-results.txt`, `build/loadcheck-results.txt`.

---

## Phase 1.1 — 2026-11-13 (historical)

**Artifact:** `dist/PathHunter.jar` (73631 bytes after clean rebuild)

**Build**

```text
javac --release 8 -encoding UTF-8 -cp <burpsuite_desktop_v2026.4.3.jar> -d build\classes Java\src\uk\co\pentest\*.java
jar --create --file dist\PathHunter.jar --manifest build\MANIFEST.MF -C build\classes .
```

| Check | Expected | Actual | Status |
| --- | --- | --- | --- |
| Compilation `--release 8` | Exit 0 | Exit 0 | Pass |
| Entry class in JAR | `uk/co/pentest/psychoPATH/BurpExtender.class` | Present | Pass |
| Implements legacy API | `burp.IBurpExtender` | Confirmed via `javap` | Pass |
| Class major version | 52 | 52 | Pass |
| No project `burp.BurpExtender` in JAR | Absent | Absent | Pass |
| No bundled Burp API interfaces / Montoya | None | None | Pass |
| `Java/psychoPATH.jar` untouched | Unmodified | Unmodified (git clean) | Pass |
| LoadCheck | 11/11 | **11/11** (`SUMMARY pass=11 fail=0`) | Pass |
| ProveBlock (`burp.Zv_d`) | Old `burp.BurpExtender` blocked; new entry assignable | blocked=true, assignable=true | Pass |
| ScanLikeBurp | Exactly 1 legacy candidate | `uk.co.pentest.psychoPATH.BurpExtender` | Pass |
| Smoke | 29/29 | **29/29** (`SUMMARY pass=29 fail=0`) | Pass |
| Burp GUI Extensions → Add | Loads without recognized-type error | Not run | Manual |

Evidence logs (local, gitignored under `build/`): `build/loadcheck-results.txt`, `build/proveblock-results.txt`, `build/scanlike-results.txt`, `build/smoke-results.txt`, `build/javac.log`.

---

## Phase 0 Smoke baseline (historical)

In-process Smoke harness against rebuilt classes. Summary: `pass=29 fail=0`. Extension name / tab later updated to PathHunter in Phase 0.1; Phase 1.1 reconfirm above.

Known product behaviours recorded as Pass rows that document current defects (null site map NPE, directory-checker wiring, etc.) remain described in `docs/known-limitations.md`.

---

## Not run

| Test | Reason |
| --- | --- |
| Burp Extender UI load | Automation does not drive the Burp desktop GUI in this phase |
| Intruder / active scan against a server | No authorised local target started |
| PHP / Perl fixtures | Interpreters not installed |
| Servlet upload fixtures | Tomcat not started |

---

## Historical Phase 0.1 notes

Load failure root cause and package relocation: `docs/burp-loading-fix.md`.
