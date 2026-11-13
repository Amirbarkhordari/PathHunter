# GitHub fork detachment

## Status (Phase 1.1 — 2026-11-13)

| Item | Value |
| --- | --- |
| Repository | https://github.com/Amirbarkhordari/PathHunter |
| Detachment completed? | **No** |
| Verified `fork` | `true` (still) |
| Verified `parent` | `ewilded/psychoPATH` |
| API attempt | GraphQL `detachFork` → **`Field 'detachFork' doesn't exist on type 'Mutation'`** |
| Browser Settings attempt | Not authenticated in automation browser (Settings returned 404 / Sign in) |
| Manual UI required | **Yes** |

Do not treat this document as evidence that Leave fork network has succeeded. Re-verify with the API after the owner completes the UI steps.

### Eligibility (still met)

- Public: yes
- Size &lt; 1 GB: yes (~10 MB)
- Child forks: 0
- Admin: yes

### What was completed without detachment

- Repository description updated to PathHunter text
- Topics set (`burp-suite`, `burp-extension`, `web-security`, `penetration-testing`, `path-traversal`, `local-file-inclusion`, `lfi`, `security-testing`, `java`, `bug-bounty`)
- Visibility unchanged (public)

---

## Official procedure (manual — required)

Documented by GitHub: [Detaching a fork](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/detaching-a-fork).

1. Sign in as the repository owner on GitHub.
2. Open https://github.com/Amirbarkhordari/PathHunter
3. **Settings** → **General** → scroll to **Danger Zone**
4. Click **Leave fork network**
5. Confirm warnings → type `PathHunter` → confirm
6. Wait until operations are available again
7. Verify the “forked from …” label is gone
8. Confirm with:

```text
gh api repos/Amirbarkhordari/PathHunter --jq "{fork, parent: (.parent.full_name // null)}"
```

Expected after success:

```text
{"fork":false,"parent":null}
```

### Consequences (GitHub docs)

- Permanent; cannot rejoin the fork network
- Git commit history preserved
- Issues, PRs, wikis, stars, watchers, comments, child forks, and related metadata are not retained

### If the button is missing

Contact [GitHub Support](https://support.github.com). Do not delete/recreate the repository while Leave fork network is available.

---

## Local remotes after detachment

| Remote | URL | Advice |
| --- | --- | --- |
| `origin` | `https://github.com/Amirbarkhordari/PathHunter.git` | Keep |
| `upstream` | `https://github.com/ewilded/psychoPATH.git` | Optional keep for historical fetch |

Keeping `upstream` does not re-attach the GitHub fork network.
