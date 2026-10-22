# Touch AutoBuild versioning and change control

Callers never track a development branch. They pin the reusable workflow to a reviewed release tag:

```yaml
uses: mrmundysr-sudo/touch-development/.github/workflows/android-build.yml@v1
```

## Tags

| Tag | Meaning | Moves? |
|---|---|---|
| `v1.0.0`, `v1.1.0`, ... | Immutable release of the workflow | Never |
| `v1` | Latest reviewed release in major version 1 | Only to a newer reviewed `v1.x.y`, with the owner's approval |
| `v2`, ... | Breaking input/output changes | New major; callers opt in per repo |

Breaking changes (renamed or removed inputs, different outputs, different artifact naming) require a new major version.
Additive changes (new optional inputs, better failure classification) are minor releases.

## Release procedure (owner approves each step)

1. Change the workflow on a `claude/` branch in the hub. Test it from a pilot caller that points at that branch.
2. Open a PR to `main`; the owner reviews and merges.
3. Tag the merge commit `v1.x.y`, then move `v1` to it. Creating or moving tags needs the owner's approval.
4. Callers pick the change up automatically through `@v1`.

## Rollback

* Bad `v1.x.y`: move `v1` back to the previous good tag (owner approval). Callers recover on their next run, no per-repo change needed.
* Bad hub merge before tagging: revert the merge commit; callers are unaffected because `v1` has not moved.
* A caller that must not follow `v1` (for example during an incident) can pin a full tag such as `@v1.0.0` or a commit SHA.

## Protection recommended (owner settings)

* Branch protection or a ruleset on the hub's `main` (PR required).
* A tag ruleset that blocks deleting or force-moving `v*.*.*` tags.
