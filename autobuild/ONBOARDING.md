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
