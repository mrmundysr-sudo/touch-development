# Touch AutoBuild versioning and change control

Production callers pin the reusable workflow to a **reviewed commit SHA** and record the release in a
comment. Tags exist for release management and documentation; callers do not follow them.

```yaml
uses: mrmundysr-sudo/touch-development/.github/workflows/android-build.yml@<40-char-sha> # v1.0.0
```

A SHA cannot move, so a caller changes only through a reviewed edit in its own repository.
Callers never track a branch (including `main`) in production.

## Tags (release management)

| Tag | Meaning | Moves? |
|---|---|---|
| `v1.0.0`, `v1.1.0`, ... | Marks the reviewed commit of that release | Never |
| `v1` | Convenience pointer to the latest `v1.x.y`, for people and docs; callers do not use it | Only with owner approval |
| `v2`, ... | Breaking input/output changes | New major; callers opt in per repo |

Breaking changes (renamed or removed inputs, different outputs, different artifact naming) require a new major version.
Additive changes (new optional inputs, better failure classification) are minor releases.

## Release procedure (owner approves each step)

1. Change the workflow on a `claude/` branch in the hub. Test it from a pilot caller that points at that branch.
2. Open a PR to `main`; the owner reviews and merges. The merge commit is the reviewed commit.
3. Tag that commit `v1.x.y` (owner approval) and record its full SHA in `autobuild/RELEASES.md`.
4. Bump callers one repository at a time with a PR that changes the SHA and the `# v1.x.y` comment.
   Dependabot's `github-actions` ecosystem can propose these bumps automatically, because it understands
   `@<sha> # vX.Y.Z` pins. The owner still reviews and merges each bump.

## Rollback

* Bad release: open a PR in each affected caller that restores the previous SHA and comment. Callers on the old SHA are unaffected the whole time.
* Bad hub merge before tagging: revert the merge commit; no caller has adopted it yet.
* Emergency: the owner can disable a caller's `autobuild.yml` without touching the repo's existing `main` build.

## Protection recommended (owner settings)

* Branch protection or a ruleset on the hub's `main` (PR required).
* A tag ruleset that blocks deleting or force-moving `v*.*.*` tags.
* Make sure the hub stays public (or enable "accessible from repositories owned by the user" in its Actions settings), otherwise private callers cannot use it.
