# Hub release record

| Release | Reviewed commit SHA (full) | Date | Tag status | Notes |
|---|---|---|---|---|
| v1.0.0 | `1926b52b24140572a299004a023f692f9357b83e` | 2026-10-23 | `v1.0.0` and `v1` created 2026-10-23 (see below) | Reusable Android debug build, failure classification, least-privilege jobs, DEV/TEST prerelease fallback and cleanup, 3-day retention. The merged tree is byte-identical to the reviewed PR head `c2bf931` (tree `a5cd8b5...`). |
| v1.0.1 | `97ce9ff9e6e178052c3ac64f8a7a3ed1fdb1bf5c` | 2026-10-23 | `v1.0.1` created 2026-10-23 (workflow run 39903868634, branch `claude/create-v101-tag`) | Shared APK validator fix (hub PR #5): framework-icon APKs accepted, explicit integrity/structure/package checks, fixture tests. Tree `f0ef3152...`. `v1` still points at v1.0.0. |

Callers pin the SHA, not the tag. Current pin:
`uses: mrmundysr-sudo/touch-development/.github/workflows/android-build.yml@97ce9ff9e6e178052c3ac64f8a7a3ed1fdb1bf5c # v1.0.1`
(`v1.0.0` = `1926b52b24140572a299004a023f692f9357b83e`; `v1` has not moved.)

## Tags (created by workflow)

Claude Code Cloud sessions cannot push tags or create refs, so both tags were created by the dev-branch workflow `create-v1-tags.yml` (branch `claude/create-v1-tags`, Actions run 39897615496). It verified the commit and tree, refused to overwrite an existing tag, created `v1.0.0` and `v1` at `1926b52b24140572a299004a023f692f9357b83e`, and verified both. `v1.0.0` is immutable; `v1` moves only with owner approval.

## Callers pinned to v1.0.1 (merged 2026-10-23)

| Repo | Bump PR | Merge commit |
|---|---|---|
| touch-clean | #7 | `93abfefd80c686ad7984b6c20b204f7d8dfd81c0` |
| TouchV | #2 | `f233a66fb06ecc71629f4373f09946a06960ab0c` |
| Touch-Parchis | #4 | `8d5e5b847ee5eefcf0507e9151655200e36f936b` |
| Touch-Toolbox | #3 | `0325561bf93653a6b74e61d0a7ea54c8c04e7408` |
| Pictureshop | #2 | `81c4608925bf5bc7740822c339d09f7c9e23cbf2` |
| touch-cast | #3 | `dd2d71d08a40621c2844b5e90a10b73e07dcb21c` |
| fresh-start-touch-wipe-out | #3 | `c6bfa457e02612dc7bccfe2c84d93836325ad677` |
| Touch_Schatzi | #1 (migration + caller) | `5656a9f02733bca310fcab539d60663f422d3344` |
| app-visual-builder | #1 (migration + caller) | `fa536d9efd78045ce6755a9dcca4c6ff9e99aac7` |

Earlier v1.0.0 caller merges: touch-clean #6 `9172cc0`, TouchV #1 `c4d0978`, Touch-Parchis #3 `9e27d82`, Touch-Toolbox #2 `8bba2ee`, Pictureshop #1 `16947a8`, touch-cast #1 `436e58e`, fresh-start #1 `dd8e550`.
Application repairs merged with owner approval: touch-cast #2 `c6179b1`, fresh-start #2 `06e96dd`.
