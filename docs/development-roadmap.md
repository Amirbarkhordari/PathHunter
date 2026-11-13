# PathHunter development roadmap

Recommendations for later phases. Items marked **planned** are not implemented. Work should stay on the legacy `IBurpExtender` API until a Montoya migration is explicitly approved.

PathHunter is GPL-3.0. Later changes must keep that license, original copyright / `@author` notices on inherited files, and a clear record that the work is a modified derivative of psychoPATH (see `NOTICE`).

## Completed / in progress (identity)

| Phase | Scope | Status |
| --- | --- | --- |
| 0 | Inspect, rebuild, document, smoke harness | Complete (committed in Phase 1.1) |
| 0.1 | Load fix (non-`burp` package), `dist/PathHunter.jar`, PathHunter UI name | Complete (committed in Phase 1.1) |
| 1 | Independent branding, docs, attribution, fork-detach guidance | Complete (committed in Phase 1.1) |
| 1.1 | GitHub description/topics, tests, commit, push; Leave fork network | Description/topics/tests/commit/push done; **Leave fork network still manual** |

## Planned product directions (Phase 1+; not implemented)

Clearly marked as future work in the README:

- Advanced Path Traversal Payload Engine
- Absolute Path Testing
- Advanced Encoding and Traversal Variants
- Linux and Windows Path Strategies (completeness / hardening)
- Improved LFI detection
- Response Analysis
- Better Intruder integration
- Improved user interface

## 1. Make the build repeatable

The sources compile with `javac --release 8` against a Burp jar that still contains the deprecated `IScanIssue` host methods. A later change can record that command in a script that does not replace `Java/build.xml` and does not pretend the missing NetBeans `nbproject` tree is present.

Keep `dist/` and `build/` out of git unless a commit is requested. Leave `Java/psychoPATH.jar` as the original author's binary.

## 2. Fix defects that change payloads, before adding techniques

Each item is already evidenced in `docs/known-limitations.md`.

1. `updateScope` label splitting (`split(".")`) and the null site-map dereference.
2. Directory checker reading `genericSuffixes` while the operator edits `suffixesList`.
3. Double-URL table producing `%2e` unless "URL-encode UTF sequences" is selected.
4. `addWindows` condition that never inserts encoded backslashes into an empty slash list.
5. Byte-generator `NullPointerException` on an unknown range, and the commented-out encoding loop.
6. `useAbsoluteWebrootsBox` and the drive Remove/Clear buttons having no effect.
7. `BinaryPayloadIssue.getHost`, `getPort`, and `getProtocol` throwing. Burp 2026 still calls the deprecated methods on some views.
8. Export constructing the generator in scanner mode, so the Intruder file-name field is ignored.
9. Empty-suffix joining that produced `..//var/www/app//a.jpg` in the harness.

These are behaviour fixes. They should be covered by the existing harness style, still outside the extension jar, and re-run on the same cases as `docs/test-results.md`.

## 3. Tests worth adding with those fixes

- Nix, Windows, and `all` slash settings at max traversal 1 and 2.
- Each encoding index, including UTF-16 and both overlong ranges, with the UTF checkbox both on and off.
- `{TARGET}` replacement and drive-letter expansion.
- Directory checker after `updateScope` with a fake site map that includes a 404 and a real path.
- `PayloadFactory.createNewInstance` for `byte`, including the `==` comparison.
- Scanner issue methods returning host, port, and protocol without throwing.

Still do not point the scanner at any host other than a local fixture the operator starts.

## 4. Later product work, only after the fixes

Taken from `TODO` and from gaps between documentation and the code:

- Scanner redirect handling.
- LFI match patterns that suit container images better than `/etc/passwd`.
- Wire `addFilePrefix` or remove the unused field.
- Apply the global encodings to the byte generator.
- Optional Perl-equivalent suffix list, kept separate from the Apache list so it can be cleared.
- A real count for `payloadCount`.
- Decide whether the checked-in jar's extra `PsychoPanel2$47`–`$54` classes represent lost UI handlers.

Do not start ZIP-slip, extension-filter bypass, or a new upload scanner in the same change as the defect fixes.

## 5. Repository independence (owner actions)

- Manually run **Leave fork network** when ready (`docs/github-fork-detachment.md`).
- Update GitHub repository description / topics to PathHunter.
- Optionally enable Issues.
- Commit and push Phase 0–1 local work only after explicit approval.
- Decide whether to keep the local `upstream` remote after detachment.

## Stop line

Phase 1 stops at branding, documentation, attribution, validation, and fork-detachment *instructions*. Do not implement new path-traversal payload engines or redesign the Burp UI in this phase. Wait for maintainer review before the next development phase.
