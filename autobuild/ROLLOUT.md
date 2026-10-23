# Touch AutoBuild: portfolio rollout plan (all 22 repositories)

Principles (owner decisions, 2026-10-23): one proven system, not several partial ones; existing working builds and releases stay untouched until a replacement is verified; ZIP-source migrations wait until the shared infrastructure is final; every merge to `main`, every tag and every stable release needs the owner's explicit approval; private repositories use the DEV/TEST prerelease fallback until artifact-storage billing is resolved.

Visibility matters: public repos store Actions artifacts normally; private repos draw on the plan's storage allowance and currently use the fallback (`DEVTEST-DELIVERY.md`).

## Phase 0: finalize the shared system (owner approvals, in this order)

| # | Action | Who | Gate |
|---|---|---|---|
| 0.1 | Merge hub PR touch-development#2 into `main` | owner approves, then merge | explicit approval |
| 0.2 | Create tag `v1.0.0` on the merge commit; record the full SHA in `RELEASES.md` (PR); move/create `v1` | Claude prepares, owner approves | explicit approval (tags) |
| 0.3 | Merge Touch-Toolbox#1 (LFS pointer cleanup) | approved 2026-10-23 after verification | done once verified |
| 0.4 | Apply `security/` rulesets (hub first); verify plan limits for private repos | owner | owner settings |
| 0.5 | Billing: resolve artifact storage for private repos (optional; fallback works meanwhile) | owner | owner |

## Phase 1: callers already prepared (public repos plus the pilot)

Each gets: `.github/workflows/autobuild.yml` pinned to the reviewed hub SHA (`@<sha> # v1.0.0`), a `CLAUDE.md` pointer, and an onboarding PR for the owner to merge. Existing `main` workflows stay unchanged.

| Repo | Visibility | PR / branch | Inputs | Notes |
|---|---|---|---|---|
| touch-clean | private | PR #6 (`claude/autobuild-pilot`) | `java 17`, `assembleDebug` | pilot; fallback delivery; owner hold on `devtest-39879594780-1` until tested |
| TouchV | public | branch `claude/autobuild-onboarding` | `java 17`, `:app:assembleDebug` | keep legacy `claude-assignment.yml` until retired (see Phase 6) |
| Touch-Toolbox | public | branch `claude/autobuild-onboarding` | `java 21`, tests + lint + `assembleDebug` | two overlapping legacy workflows on `main` both publish `apk-*` prereleases; consolidate with owner approval |
| Touch-Parchis | public | branch `claude/autobuild-onboarding` | `project_dir TouchParchis`, `clean test lintDebug assembleDebug` | read `AGENTS.md` first (referenced by its `CLAUDE.md`) |

Acceptance (every repo): a green `claude/` push stores an artifact (or DEV/TEST prerelease in private repos); a deliberate compile error is classified `class=code` and repaired; no release other than a labelled DEV/TEST prerelease is created; `main` unchanged except the approved onboarding PR.

## Phase 2: Gradle repos without a wrapper or without a workflow

| Repo | Visibility | Inputs | Open question |
|---|---|---|---|
| touch-cast | private | `java 17`, `gradle_version 8.9` | AndroidX deps (appcompat, mediarouter), compileSdk 35 |
| Pictureshop | private | `java 17`, `gradle_version 8.7` | existing workflow caches the debug keystore for a stable debug signature; decide whether dev branches keep that |
| fresh-start-touch-wipe-out | public | `java 17` (has `gradlew`) | no workflow yet; one trial build to learn tasks |

## Phase 3: non-standard build

| Repo | Visibility | Plan |
|---|---|---|
| Touch_7_Come_11 (Capacitor) | private | `npm test`, `npx cap add/sync android`, a Python patch step, then Gradle in `android/` on Java 21. Needs a capacitor input or pre-build hook in the hub (minor version, additive). Existing `android.yml` stays. |

## Phase 4: ZIP-source migrations (only after Phases 0 to 3 are proven)

Repos whose source is stored as archives: Touch-One, Touch_Schatzi, app-visual-builder, arcade-touch. One at a time, each by owner-approved PR:

1. Branch `claude/migrate-source-tree` from the default branch.
2. Record a baseline: current working APK version, size and SHA-256 from the existing workflow.
3. Unpack into a normal source tree (preserve file modes such as `gradlew`, line endings, `gradle-wrapper.jar`).
4. Keep every original archive as backup under `backup/` (ZIP, `.touch-uploads` chunks, `source.part.*`). Nothing is deleted.
5. Build the new tree with the existing workflow adapted on the branch only; compare manifest, version and size to the baseline.
6. Add the AutoBuild caller and `CLAUDE.md`; owner reviews and approves the merge.
7. Only after a green `main` build is the old unpack step retired (a separate approval).

| Repo | Visibility | Archive | Specific risk |
|---|---|---|---|
| Touch-One | public | `.touch-uploads/<hash>/` (94 chunks) reassembled by a script fetched from `Touch-Developer-/main` | removes the cross-repo raw-URL dependency; active dev branch `touch-one-claude` |
| Touch_Schatzi | private | `.touch-uploads/<hash>/` (79 chunks), two `.touchmanifest` files | source plus separate visual-assets archive; keep assets out of the source tree |
| app-visual-builder | private | `AppVisualBuilder.zip` (+ starter zip) | smallest; existing workflow already rejects unsafe zip paths |
| arcade-touch | private | `prototypes/touch-pocket/source.part.*` base64 tarball with pinned SHA-256 | native build (`cmake;3.22.1`, `platforms;android-35`, `build-tools;35.0.0`: use `sdk_packages`); confirm which branch holds the parts |

## Phase 5: repository with its own multi-workflow setup

| Repo | Visibility | Plan |
|---|---|---|
| Touch_Reader | public | App lives on branch `claude/ecstatic-feynman-cfayvz` with 8 workflows (build on push/PR/dispatch with 14-day retention and a `debug-N` pre-release, plus probes). Do not touch without owner approval. Proposal for that PR: trim `Build APK` to the shared workflow, 3-day retention, no duplicate dispatch triggers. |

## Repositories with no buildable Android application yet

Onboard only when an Android project appears (use `ONBOARDING.md` and the templates):

| Repo | Visibility | Contents today |
|---|---|---|
| Couchcast | private | README only (a release `apk-1` exists from an earlier session) |
| ScoobertsHangman | private | art assets and docs |
| Touch-Find-A-Word | private | assets, word lists, uploads (a feature branch has a built APK) |
| Touch-Modules | public | README only |
| Touch-Rewards | private | living spec |
| Touch-Word-Bomb | private | README only (a `codex/` branch has a prototype) |
| touch-sports-guide | private | docs (a `codex/` branch has a build) |
| touch-development | public | this hub (docs and workflow; nothing to build) |

Several of these have app code only on non-default branches or in old sessions' releases. Confirm with the owner whether each is meant to be an app before onboarding.

## Phase 6: retire redundant legacy workflows (separate approval each)

* `claude-assignment.yml` in Touch-Developer- and TouchV: GitHub-hosted Claude path that uses `ANTHROPIC_API_KEY` and a per-run cost cap. Keep operational until AutoBuild has handled real assignments for a few weeks, then disable (not delete) with approval.
* Touch-Toolbox: `android-debug-apk.yml` and `build.yml` duplicate each other (both build and publish on `main`). Consolidate to one.
* Touch-Developer- `android-debug-apk.yml`: already builds `claude/**` with tests, lint and validation; optional later move to the shared workflow.

## New and future repositories

1. Create the repo; add `CLAUDE.md` (from `templates/CLAUDE.md`) and `.github/workflows/autobuild.yml` (from `templates/autobuild-caller.yml`) pinned to the current reviewed hub SHA.
2. Add a `projects.yml` entry in the hub (PR).
3. Apply the rulesets in `security/` (owner).
4. First `claude/` push proves the pipeline; set status `active`.

## Operating the unattended loop (what you do and what Claude does)

* You: start a Claude Code session with the app's repo selected (select repos at session start; mid-session attaching can be refused by the permission classifier), choose Auto mode, describe the task.
* Claude: branch `claude/<slug>`, change code, push, read `AUTOBUILD-RESULT`, repair up to 3 times for `class=code`, stop and report for `infrastructure`, deliver the artifact or DEV/TEST prerelease link with commit and digest.
* You: install and test on a device (nothing here runs the app), then approve merges, tags or releases.
