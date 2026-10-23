# Onboarding a repository to Touch AutoBuild

New or existing Android repo:

1. Add an entry to `autobuild/projects.yml`.
2. Copy `templates/autobuild-caller.yml` to `.github/workflows/autobuild.yml`, fill in the
   `with:` values from the registry. Keep the repo's existing `main` workflow unchanged.
3. Copy `templates/CLAUDE.md` to the repo root.
4. Open a one-time onboarding PR; the owner merges it (workflow files must be on `main`
   for dispatch to work). Verify with a throwaway `claude/test` push.
5. Set `status` to `active` in the registry.

Repos with source packed in ZIPs (`type: zip`) are first migrated to a normal source tree:
unpack on a `claude/` branch, keep the ZIP as `backup/`, confirm the old workflow and the
new one produce an equivalent APK, then the owner approves the merge.

## Version pinning

Production callers pin the hub workflow to a reviewed commit SHA with a version comment (`@<sha> # v1.0.0`). Pilot callers may point at a hub dev branch only until the hub is merged and tagged; the onboarding PR must switch them to the SHA before merging. See `VERSIONING.md`.

## Permissions

Callers declare `contents: write` as the ceiling. The shared workflow gives write access only to its `fallback` and `housekeeping` jobs (DEV/TEST prerelease delivery and cleanup, `claude/**` only, no checkout); the job that runs Gradle and repository code is read-only. See `DEVTEST-DELIVERY.md`.

## Older template

`templates/android-debug-apk.yml` is the pre-AutoBuild standalone build template (a full workflow copied into each repo). New and migrated repositories use `templates/autobuild-caller.yml` instead. The old template stays for repositories that have not been onboarded yet and is retired once all buildable repositories use the shared workflow (rollout Phase 6).
