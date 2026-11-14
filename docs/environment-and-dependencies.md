> **Canonical documentation:** [PATHHUNTER_MASTER_DOCUMENTATION.md](PATHHUNTER_MASTER_DOCUMENTATION.md).
> This file is supplemental/historical. Prefer the master document if details differ.

# Environment and dependencies

## Phase 0.1 note

Burp Suite 2026.4.3 still ships urp.IBurpExtender (class version 65 / Java 21 bytecode for the interface). Legacy extensions remain supported, but the extension classloader (urp.Zv_d / urp.Zzq3) parent-first-loads urp.* and only allowlists API **interfaces**. PathHunter implementation classes were moved to uk.co.pentest.psychoPATH so they are not subject to that parent block. Full write-up: docs/burp-loading-fix.md.
Facts below were read from this machine during the assessment. Items marked as not installed were checked with `Get-Command` and were absent from `PATH`.

## Machine

| Item | Value |
| --- | --- |
| Operating system | Microsoft Windows 11 Enterprise, version 10.0.22631, 64-bit |
| Workspace | `D:\Projects\PathHunter` |
| Git | 2.47.1.windows.1 at `C:\Program Files\Git\cmd\git.exe` |
| JDK | Oracle Java 24.0.2, `C:\Program Files\Java\jdk-24`. `java` and `javac` on `PATH` resolve through `C:\Program Files\Common Files\Oracle\Java\javapath`. `jar.exe` is not on `PATH`; it is at `C:\Program Files\Java\jdk-24\bin\jar.exe`. |
| Other JDKs | Only `jdk-24` was present under `C:\Program Files\Java`. |
| Apache Ant | Not installed. |
| Apache Maven | Not installed. |
| Gradle | Not installed. |
| Perl | Not installed. `perl/get_docroots.pl` was not executed. |
| PHP | Not installed. The `test_cases/LFI` scripts were not executed. |
| Python | 3.12 at `C:\Users\Amir\AppData\Local\Programs\Python\Python312\python.exe`. Used only to locate classes inside the Burp jar. |

## Java version the project requires

The current sources use Java 8 language features (lambdas in `PsychoTab`) and no newer syntax (`var`, records, text blocks, or modules were not found).

The checked-in `Java/psychoPATH.jar` was built with JDK `1.8.0_171` and Ant 1.9.6. `javap -verbose` reports major version 52 for `burp.BurpExtender`.

`Java/build.xml` does not set `source` or `target`. Those properties would have lived in the missing NetBeans `nbproject/project.properties`.

This assessment compiled with `javac --release 8` from JDK 24. JDK 24 still accepts `--release 8` and warns that source and target 8 are obsolete. The rebuilt `burp.BurpExtender` is major version 52.

## Build tools the repository expects

`Java/build.xml` is a NetBeans Ant script. Its only functional line imports `nbproject/build-impl.xml`. That directory is not in the git tree, so the script cannot run even after Ant is installed.

No `pom.xml` and no Gradle build file exist. None was added.

## Compile-time dependencies

| Dependency | Required by | Present in the repo |
| --- | --- | --- |
| JDK `java.*` and `javax.swing.*` | UI and generators | Provided by the JDK |
| Legacy Burp interfaces in package `burp` | Every integration class | Not in the repo. The original manifest points at `lib/burpsuite_pro.jar`, and that `lib` directory is absent. |
| `beansbinding-1.2.1.jar` | Named on the original jar `Class-Path` | Absent. No source file imports `org.jdesktop.beansbinding`. Not required to compile the current sources. |

### Which Burp API

The extension uses the legacy Extender API:

- `burp.IBurpExtender`
- `burp.IBurpExtenderCallbacks`
- `burp.ITab`
- `burp.IIntruderPayloadGenerator` and `IIntruderPayloadGeneratorFactory`
- `burp.IScannerCheck` and `IScanIssue`
- `burp.IContextMenuFactory`

It does not use `burp.api.montoya`. This phase did not migrate it.

Two API jars were compared:

| API | `IScanIssue.getHost` / `getPort` / `getProtocol` |
| --- | --- |
| Maven `net.portswigger.burp.extender:burp-extender-api:2.3` (20139 bytes, latest on Maven Central for that artifact) | Absent |
| `burpsuite_desktop_v2026.4.3.jar` on this machine | Present, and `javac -Xlint:deprecation` reports them as deprecated |

`BinaryPayloadIssue` overrides those three methods. Compiling against Maven 2.3 fails those `@Override` methods. The rebuild used the installed Burp 2026.4.3 desktop jar as the compile classpath so the overrides match the API that jar actually contains. The Burp classes are not packaged into `dist/psychoPATH.jar`. Burp supplies them at load time.

## Burp Suite on this machine

| Item | Value |
| --- | --- |
| Folder | `C:\Users\Amir\Documents\Burp.Suite.Professional.2026.4.3` |
| Product jar | `burpsuite_desktop_v2026.4.3.jar` (690858222 bytes) |
| Desktop shortcut | `C:\Users\Amir\Desktop\burp.lnk` points at `burp.cmd` in that folder |
| `burp.cmd` | Runs `java -jar BurpLoaderKeygen117.jar` |
| `BurpSuitePro.vmoptions` | Includes `-javaagent:BurpLoaderKeygen117.jar`, `-noverify`, and `--add-opens` flags. It does not pin a private JRE. The folder listing has no bundled `jre` directory. |
| User config | `C:\Users\Amir\AppData\Roaming\BurpSuite` had no files when listed. |

The launcher was not started. Loading the extension inside that GUI was therefore not verified. See `docs/build-and-installation.md` for the manual Extender steps.

Burp 2026 still contains `burp/IBurpExtender.class` and `burp/IScanIssue.class`, so this build of Burp still hosts the legacy API the extension implements.

## Runtime coupling

The generators call `IExtensionHelpers.bytesToString` and `stringToBytes` while building payloads. They cannot run without a Burp callbacks object, or a stand-in that implements those two methods. The smoke harness used `java.lang.reflect.Proxy` for that stand-in and converted bytes with ISO-8859-1, which is the historical Burp helpers mapping for a single byte.
