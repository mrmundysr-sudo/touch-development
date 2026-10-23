# DEV/TEST delivery fallback (GitHub prerelease asset)

Approved by the owner on 2026-10-23 as a **temporary** fallback for development builds only.

## When it runs

Only when **all** of these are true, in the shared workflow's `DEV/TEST delivery fallback` step:

1. The branch is a development branch (`refs/heads/claude/**`). Never `main`, never tags, never PR merge refs.
2. The build succeeded **and the APK passed validation** (`apksigner`, `aapt`, zip integrity).
3. The normal Actions artifact upload failed **and** its one automatic retry failed.

Otherwise nothing is published. If the normal upload works, no release is created.

## What it publishes

* A GitHub **prerelease** (never "latest", never a stable release) in the app's own repository.
* Tag `devtest-<run id>-<attempt>`. The `devtest-` prefix is enforced by the step; it never uses `v*` or any existing tag, so stable tags cannot be moved.
* Title `DEV/TEST BUILD (not a release): <artifact name> - <branch> @ <sha7>`; notes state "NOT A PRODUCTION RELEASE", the branch and full commit, that it is debug-signed, and the cleanup date.
* One asset: `<artifact name>-DEVTEST.apk`.
* The run's result line then reads `class=none delivery=devtest-release` and prints `DELIVERY-URL <link>`; the link is also a workflow warning and the `delivery_url` output.

Private repositories: release assets inherit the repository's visibility, so only collaborators can download them (sign in to GitHub on the phone).

## What it never does

Merge or push to `main`; create or edit a non-`devtest-` tag or release; publish a production release; use secrets or paid APIs; run on another repository.

## Permissions (least privilege)

The caller grants `contents: write` at its level (a called workflow can never exceed what the caller grants), but the shared workflow hands it out per job:

| Job | Permission | Runs repository code? |
|---|---|---|
| `build` (checkout, Gradle, validate, upload) | `contents: read`; checkout uses `persist-credentials: false` | **yes** (Gradle scripts, dependencies), so it has no write access to the repository |
| `fallback` (publish DEV/TEST prerelease) | `contents: write` | no: no checkout, only fixed `gh` commands |
| `housekeeping` (prune expired DEV/TEST prereleases) | `contents: write` | no: no checkout, only fixed `gh` commands |
| `result` (classify) | none | no |

The validated APK reaches the `fallback` job through the Actions **cache** (`actions/cache` save/restore, key `autobuild-apk-<run id>-<attempt>`), which is separate from artifact storage, so it still works when the artifact allowance is exhausted. Both write jobs also refuse to run unless the ref is `refs/heads/claude/*`.

## Cleanup (automatic, owner-approved 2026-10-23)

The `housekeeping` job runs on every `claude/**` build and deletes DEV/TEST prereleases that match **all** of: tag starts with `devtest-`, marked prerelease, not a draft, title starts with `DEV/TEST BUILD`, created by `github-actions[bot]`. Anything else (production releases, `apk-*`, `v*` and every other tag) can never match.

A matching release is deleted (with its `devtest-` tag only) when:

* it is **7 or more days old**, or
* it is beyond the **newest 3** DEV/TEST releases of that repository **and** at least 48 hours old (a fresh, untested APK is never removed by the count rule).

It is skipped when it is on an owner hold: the caller input `prune_keep_tags` (space-separated tags, reviewable in the repo) or a `[KEEP]` marker in the release title or notes. Inputs `prune_dry_run: true` (list only) and `prune_max_age_days` exist for testing; the minimum for a real run is 7 days.

Each run logs `AUTOBUILD-PRUNE dry_run=... deleted=... kept=...`. Manual commands, for reference:

```bash
gh release list --repo OWNER/REPO --limit 100 | grep devtest-
gh release delete devtest-<run id>-<attempt> --repo OWNER/REPO --cleanup-tag --yes
```

Claude's own Claude Code Cloud sessions cannot create, edit or delete releases (HTTP 403 "not permitted for this session type"); the workflow's own token does it. To hold a release, add its tag to `prune_keep_tags` in that repo's caller.

* Releases are not Actions artifact storage, so they do not use the artifact allowance.
* Retire the fallback when artifact uploads are reliable for private repositories (billing resolved): remove the `fallback` job in a new minor version.

## Caveats

* A release publishes a tag, which can trigger workflows that listen to `release` or `create` events. None of the current repositories do; check before onboarding a repository that does.
* The APK is debug-signed; installing over a differently signed debug build requires uninstalling first.
