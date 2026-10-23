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

## Optional prebuild step (Capacitor, native cores)

Some projects cannot run Gradle straight after checkout: a Capacitor app has to create its `android/` folder first, and a
native app may have to compile a core with the NDK. The shared workflow supports this without arbitrary commands:

| Input | Meaning |
|---|---|
| `prebuild_script` | Path, relative to the repository root, of a script **committed in the repository being built** (convention: `.autobuild/prebuild.sh`). Never a command string. |
| `node_version` | Optional Node.js version, installed before the script. |
| `prebuild_timeout_minutes` | Time limit (default 20). |
| `project_dir` | Where Gradle runs afterwards; the script may create it. |
| `sdk_packages` | Already supported; list `ndk;...` and `cmake;...` here. |

Safeguards (enforced and tested by `autobuild/tests/prebuild/`):
- The path must be a plain relative path, contain no `..`, not be a symlink, resolve inside the workspace and be tracked in the checked-out commit. Anything else fails before any code runs.
- The script runs with an allow-listed environment (PATH, HOME, Java and Android variables, runner temp/workspace). `GITHUB_TOKEN`, `ACTIONS_*`, `GITHUB_ENV` and `GITHUB_OUTPUT` are not passed, so it cannot read credentials or alter later steps. The job has `contents: read`, no persisted credentials and receives no caller secrets.
- Its SHA-256 is logged on every run. A change to the prebuild script is a build-infrastructure change and is reviewed like one.
- A failing script is reported as `class=code` (`prebuild=failure` in the result line); network-looking failures are flagged as infrastructure. Callers that set no `prebuild_script` behave exactly as before.

Examples that pass today: `Touch_7_Come_11` (Capacitor: `npm test`, `npm install`, `npx cap add android`, patch, `npx cap sync`; `java_version: "21"`, `node_version: "20"`, `project_dir: android`) and `arcade-touch` (reconstruct packed source, verify its checksum, build the pinned mGBA core with the NDK).
