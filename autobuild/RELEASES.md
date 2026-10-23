# Hub release record

| Release | Reviewed commit SHA (full) | Date | Tag status | Notes |
|---|---|---|---|---|
| v1.0.0 | `1926b52b24140572a299004a023f692f9357b83e` | 2026-10-23 | **tag pending** (see below) | Reusable Android debug build, failure classification, least-privilege jobs, DEV/TEST prerelease fallback and cleanup, 3-day retention. The merged tree is byte-identical to the reviewed PR head `c2bf931` (tree `a5cd8b5...`). |

Callers pin the SHA, not the tag, so they are unaffected by the tag being pending:
`uses: mrmundysr-sudo/touch-development/.github/workflows/android-build.yml@1926b52b24140572a299004a023f692f9357b83e # v1.0.0`

## Creating the tags (owner action: Claude Code Cloud sessions cannot push tags or create refs)

Both `git push` of a tag (HTTP 403) and the REST ref-creation endpoint ("Write access to this GitHub API path is not permitted through this proxy") are blocked for these sessions. From a machine signed in as the owner:

```bash
git clone https://github.com/mrmundysr-sudo/touch-development && cd touch-development
git tag v1.0.0 1926b52b24140572a299004a023f692f9357b83e   # immutable, never move
git tag v1      1926b52b24140572a299004a023f692f9357b83e   # convenience pointer, moves only with owner approval
git push origin v1.0.0 v1
git ls-remote --tags origin 'v1*'
```

Verify both resolve to `1926b52b...`. Then ask Claude to mark the tags created here.

## Callers pinned to v1.0.0 (merged 2026-10-23)

| Repo | PR | Merge commit |
|---|---|---|
| touch-clean | #6 | `9172cc021b62ba361d1376ca53dbf90e78c3f494` |
| TouchV | #1 | `c4d09789573449bed0439354b523ba3a3d36af33` |
| Touch-Parchis | #3 | `9e27d82390016ffde45a76289182b13e0b8b8502` |
| Touch-Toolbox | #2 | `8bba2ee950e85a64c5fa14875d62d81aa9cdf1c5` |
