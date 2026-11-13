# GitHub fork detachment

## Status (verified 2026-11-13, Phase 1.2)

| Item | Value |
| --- | --- |
| Repository | https://github.com/Amirbarkhordari/PathHunter |
| Detachment completed? | **Yes** (verified via API) |
| Verified `fork` | `false` |
| Verified `parent` | `null` / absent |
| Method | Owner completed **Leave fork network** in GitHub Settings (UI). GraphQL `detachFork` was unavailable during Phase 1.1 automation. |

Evidence command:

```text
gh api repos/Amirbarkhordari/PathHunter --jq "{fork, parent: (.parent.full_name // null)}"
```

Expected (and observed):

```text
{"fork":false,"parent":null}
```

Git commit history was preserved. Local remotes:

| Remote | URL | Notes |
| --- | --- | --- |
| `origin` | `https://github.com/Amirbarkhordari/PathHunter.git` | Primary |
| `upstream` | historical source remote | Optional; does not re-attach the fork network |

---

## Historical note (Phase 1 / 1.1)

Before detachment, the repository was a fork of `ewilded/psychoPATH`. Self-serve Leave fork network eligibility was met (public, &lt; 1 GB, no child forks). Official docs: [Detaching a fork](https://docs.github.com/en/pull-requests/how-tos/work-with-forks/detaching-a-fork).
