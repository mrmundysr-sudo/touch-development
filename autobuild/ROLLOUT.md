# Touch AutoBuild rollout plan

Status of the pilot: touch-clean builds and validates on `claude/**` branches; artifact storage
was the only blocker. Roll out in waves. Each repo gets a caller (`.github/workflows/autobuild.yml`)
and a `CLAUDE.md` pointer through a one-time onboarding PR that the owner merges. Existing
`main` workflows stay unchanged until the replacement is verified (owner's rule).

Prerequisite for every wave: hub merged, tag `v1` created, callers pinned to `@v1`.

## Wave 1: plain Gradle repos with a wrapper (low risk)

| Repo | Inputs | Notes |
|---|---|---|
| touch-clean | `java_version: 17` | Pilot. Existing `build-debug-apk.yml` stays. |
| TouchV | `java_version: 17`, `tasks: :app:assembleDebug` | Wrapper 8.7, AGP 8.5.2. Existing `android.yml` and `claude-assignment.yml` stay until retired. Uses AndroidX (appcompat). |
| Touch-Toolbox | `java_version: 21`, `tasks: ":app:testDebugUnitTest lintDebug assembleDebug"` | Kotlin 1.9.24, wrapper 8.9, minSdk 24. Two overlapping workflows (`android-debug-apk.yml`, `build.yml`) both upload and pre-release on `main`; consolidate to one before onboarding (owner approval). |
| Touch-Parchis | `project_dir: TouchParchis`, `java_version: 17`, `tasks: "clean test lintDebug assembleDebug"` | Kotlin, compileSdk 35, unit tests in `app/src/test`. |
| Touch-Developer- | none, or use hub workflow | Already builds `claude/**` in its own workflow with tests, lint and APK validation. Optional: migrate to the shared workflow later; no duplicate needed now. |

## Wave 2: Gradle repos without a wrapper

| Repo | Inputs | Notes |
|---|---|---|
| touch-cast | `java_version: 17`, `gradle_version: "8.9"` | compileSdk 35, AndroidX mediarouter. Adding `gradlew` is out of scope; the workflow installs Gradle. |
| Pictureshop | `java_version: 17`, `gradle_version: "8.7"` | compileSdk 35. Existing workflow reuses the debug keystore across builds (cache). Decide whether to keep that cache (stable debug signature for updates on phone) or accept a changing debug signature on dev branches. |
| fresh-start-touch-wipe-out | `java_version: 17` | Has `gradlew`, no workflow yet. Needs one trial build to learn its tasks. |

## Wave 3: non-standard builds

| Repo | Plan |
|---|---|
| Touch_7_Come_11 (Capacitor) | Needs `npm test`, `npx cap add/sync android`, a Python patch step, then Gradle in `android/` on Java 21. Add a `capacitor` reusable workflow (or a `pre_build` hook input) in the hub. Existing `android.yml` stays. |
| arcade-touch | Source is reassembled from `prototypes/touch-pocket/source.part.*` (base64 tarball, SHA-256 checked) and needs `platforms;android-35`, `build-tools;35.0.0`, `cmake;3.22.1` (native code). Reusable workflow supports `sdk_packages` for that; the source needs a normal tree first (migration list below). |

## ZIP-source migration (after the pilot is verified; one repo at a time; owner approves each merge)

Repos whose source lives in archives and is unpacked in CI: Touch-One, Touch_Schatzi, app-visual-builder, arcade-touch.

Per repo:
1. Branch `claude/migrate-source-tree` from the default branch.
2. Reproduce the current build locally in CI from the existing path first and record the working APK SHA-256 and version (baseline).
3. Unpack the source into a normal tree (for example `app/`, `gradle/`, `settings.gradle`), preserving file modes (`gradlew`), line endings and any `gradle-wrapper.jar`.
4. Keep the originals as backups under `backup/` (ZIP, `.touch-uploads` chunks, `source.part.*`) so the previous working version stays recoverable; do not delete them.
5. Switch the existing workflow's build step to the tree on the branch only, run it, and compare version/manifest and APK size to the baseline.
6. Add the AutoBuild caller and `CLAUDE.md`, open the PR for the owner. Only after merge and one green `main` build is the old unpack step retired (separate approval).

| Repo | Archive | Specific risk |
|---|---|---|
| Touch-One | `.touch-uploads/<hash>/00000..` (94 chunks) reassembled by a script fetched from `Touch-Developer-/main`, then `files (4).zip` | Workflow depends on a raw URL in another repo; the migration removes that coupling. Branch `touch-one-claude` is the active dev branch. |
| Touch_Schatzi | `.touch-uploads/<hash>/` (79 chunks) plus two `.touchmanifest` files | Two archives (source and visual assets); keep assets as a separate directory or release asset, not mixed into source. Release assets already exist (`apk-build-4`). |
| app-visual-builder | `AppVisualBuilder.zip` (and a starter zip) | Smallest migration; the workflow already checks for `settings.gradle.kts` and unsafe paths. Keep the same checks as a one-off script in the migration PR. |
| arcade-touch | `prototypes/touch-pocket/source.part.*` base64 tarball with pinned SHA-256 | Has native build (`cmake`); parts are not on the default branch's file list, so confirm which branch holds the source before touching anything. |

## Repos with no app code yet

Couchcast, ScoobertsHangman, Touch-Find-A-Word (assets and uploads), Touch-Modules, Touch-Rewards,
Touch-Word-Bomb, Touch_Reader (art zip on `main`; app lives on another branch), touch-sports-guide,
touch-development (this hub). They get the onboarding template only when an Android project appears.
Touch_Reader's active branch has its own multi-workflow setup (builds on push, pull_request and
dispatch with 14-day retention and a pre-release step); do not touch it without the owner's approval,
then trim it to the shared workflow and 3-day retention.

## Per-wave acceptance checklist

- Caller builds a green `claude/test` push and uploads an artifact; the artifact name and retention (3 days) look right.
- A deliberate compile error produces `AUTOBUILD-RESULT class=code`; the repair restores green.
- No release was created; `main` unchanged except the approved onboarding PR.
- Registry entry set to `active`.
