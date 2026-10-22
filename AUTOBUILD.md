# Touch AutoBuild: standing instructions

Applies to every Android repository owned by mrmundysr-sudo. The registry is
`autobuild/projects.yml`; the shared build is `.github/workflows/android-build.yml`.

## The assignment loop

1. **Resolve** the app in `autobuild/projects.yml`. If it is missing, `status: blocked`
   or `type: zip`, stop and tell the owner.
2. **Branch** from the default branch as `claude/<short-slug>`. Never commit to `main`.
3. **Change** only what the assignment needs. Keep existing build setup, dependencies
   and signing configuration as they are.
4. **Push.** The repo's `autobuild.yml` caller builds every `claude/**` push. Check the
   run started (otherwise dispatch it once).
5. **Monitor** the run, then read failing job logs.
6. **Repair**, at most **3 repair attempts** per assignment. One attempt is one
   diagnose-fix-push cycle. Fix root causes; never skip, disable or delete tests or checks
   to get green. Same error after two attempts means change approach or stop.
7. **Deliver** when the run is green: run URL, artifact name, commit SHA, SHA-256 from the
   run summary, and a short summary of changes.
   **Classify before repairing.** Only failures caused by the code or build config in the
   repo consume a repair: compile errors, test failures, lint, bad dependency versions,
   Gradle config. Infrastructure failures do not: artifact storage quota ("Artifact storage
   quota has been hit"), runner/network outages, expired tokens, GitHub 5xx. Never edit code
   for those. Report them, retry a failed job at most once, and stop if it persists.
8. **If still failing after 3 repairs:** stop, push nothing further, and report the failing
   step, the error text, what was tried, and a suggested next step.

## Needs the owner's explicit approval

Merging to `main`, publishing or editing releases, anything touching signing keys,
keystores or secrets, deleting branches/repos/files at scale, major architecture changes,
adding AndroidX or other large dependencies, and changing repository settings.

## Rules

- Debug builds only. The shared workflow publishes no releases and calls no paid APIs.
- Never print, log, commit or request credential values.
- Do not duplicate working workflows; extend the shared one instead.
- Do not report success unless the run confirmed it.
- Final report always states: outcome, repairs used (0-3), run link, files changed.

## Report format

```
Result: SUCCESS | FAILED after N repairs
App / branch / commit:
APK artifact: <run url> (name, expires)
Changes: <bullets>
Repairs: <what failed, what fixed it>
Unresolved: <none | error + suggestion>
```
