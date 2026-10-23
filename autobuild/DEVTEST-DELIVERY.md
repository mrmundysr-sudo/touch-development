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

## Requirements

Callers must grant `permissions: contents: write` (the templates and onboarding callers do). Without it the fallback fails and the run reports the normal `infrastructure / manual intervention` result.

## Cleanup policy

* Keep at most **3** `devtest-*` releases per repository, and none older than **7 days**.
* Deletion removes real files, so until the owner approves automation, Claude **lists** candidates in its report and deletes only on explicit approval. After approval, Claude deletes only releases whose tag starts with `devtest-` and that are marked prerelease, together with their tag:

  ```bash
  gh release list --repo OWNER/REPO --limit 100 | grep devtest-
  gh release delete devtest-<run id>-<attempt> --repo OWNER/REPO --cleanup-tag --yes
  ```
* Releases are not Actions artifact storage, so they do not consume the artifact allowance; they should still be pruned to keep the release list readable.
* Retire the fallback when artifact uploads are reliable for private repositories (billing resolved): remove the step from the workflow in a new minor version.

## Caveats

* A release publishes a tag, which can trigger workflows that listen to `release` or `create` events. None of the current repositories do; check before onboarding a repository that does.
* The APK is debug-signed; installing over a differently signed debug build requires uninstalling first.
