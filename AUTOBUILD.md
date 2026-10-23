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

## Reading the result: code vs infrastructure

Every run ends, in the `result` job (job name `<caller job> / result`), with a log line `AUTOBUILD-RESULT class=<none|code|infrastructure> manual_intervention=<true|false> ...`
(also in the run summary and as the `failure_class` / `manual_intervention` / `delivery` / `delivery_url` workflow outputs).

| class | meaning | what Claude does |
|---|---|---|
| `none`, delivery=artifact | built, validated, stored as an Actions artifact | deliver the artifact link |
| `none`, delivery=devtest-release | built, validated; artifact upload failed so the APK was published as a **DEV/TEST prerelease** (see `autobuild/DEVTEST-DELIVERY.md`) | deliver the `DELIVERY-URL`, say it is a DEV/TEST prerelease, mention cleanup |
| `code` | compile/test/lint/config error, or APK failed validation | diagnose and repair (max 3) |
| `infrastructure`, manual=false | network/repo/disk error that survived one automatic retry, or job setup failed | re-run failed jobs once; if it fails again, report; never edit code |
| `infrastructure`, manual=true | APK built but could not be stored (usually the artifact storage allowance; applies to **private** repos only, public repos are not metered) | stop, report **MANUAL INTERVENTION REQUIRED**, schedule one re-check, never edit code |

The shared workflow already retries infrastructure-looking build errors once and the artifact
upload once, so a re-run by Claude is the second retry, not the first.

### Manual-intervention report

When `manual_intervention=true`, or an infrastructure failure persists after the re-run, the
final report starts with:

```
MANUAL INTERVENTION REQUIRED
What is blocked: <app / branch / run link>
Cause: <one line, from the AUTOBUILD-RESULT reason>
What only you can do: <e.g. free Actions artifact storage in Settings > Billing, or wait 6-12 h>
What I already did: <retries, code state, validated APK built but not stored>
What I will do next: <scheduled re-check time | nothing until you reply>
```

### DEV/TEST prereleases

When `delivery=devtest-release`, the APK is a DEV/TEST prerelease (see `autobuild/DEVTEST-DELIVERY.md`). The `housekeeping` job prunes expired ones automatically (7 days; newest 3 kept; owner holds via `prune_keep_tags`). Claude never edits or deletes releases itself and never touches production releases or stable tags.

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
