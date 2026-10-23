# Hub release record

| Release | Reviewed commit SHA (full) | Date | Tag status | Notes |
|---|---|---|---|---|
| v1.0.0 | `1926b52b24140572a299004a023f692f9357b83e` | 2026-10-23 | `v1.0.0` and `v1` created 2026-10-23 (see below) | Reusable Android debug build, failure classification, least-privilege jobs, DEV/TEST prerelease fallback and cleanup, 3-day retention. The merged tree is byte-identical to the reviewed PR head `c2bf931` (tree `a5cd8b5...`). |

Callers pin the SHA, not the tag:
`uses: mrmundysr-sudo/touch-development/.github/workflows/android-build.yml@1926b52b24140572a299004a023f692f9357b83e # v1.0.0`

## Tags (created by workflow)

Claude Code Cloud sessions cannot push tags or create refs, so both tags were created by the dev-branch workflow `create-v1-tags.yml` (branch `claude/create-v1-tags`, Actions run 39897615496). It verified the commit and tree, refused to overwrite an existing tag, created `v1.0.0` and `v1` at `1926b52b24140572a299004a023f692f9357b83e`, and verified both. `v1.0.0` is immutable; `v1` moves only with owner approval.

## Callers pinned to v1.0.0 (merged 2026-10-23)

| Repo | PR | Merge commit |
|---|---|---|
| touch-clean | #6 | `9172cc021b62ba361d1376ca53dbf90e78c3f494` |
| TouchV | #1 | `c4d09789573449bed0439354b523ba3a3d36af33` |
| Touch-Parchis | #3 | `9e27d82390016ffde45a76289182b13e0b8b8502` |
| Touch-Toolbox | #2 | `8bba2ee950e85a64c5fa14875d62d81aa9cdf1c5` |
