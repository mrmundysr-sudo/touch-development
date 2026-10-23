# Touch-One: making version 1.4.0 the canonical source (plan only)

Status: **plan for owner review. Nothing has been changed in Touch-One.**
Prepared 2026-10-23 from read-only inspection of `mrmundysr-sudo/Touch-One`.

## Findings

### Where each version lives
| Location | Version | Form |
|---|---|---|
| `main` (`f65c7fd`) `source/` | 1.3.31 (versionCode 38) | normal Gradle tree |
| `main` `.touch-uploads/69fee5c1…/00000-00093` + `files (4).zip.touchmanifest` (commit `eeab942`, 2026-10-15 00:30 -0400) | contains 1.4.0 | 94 chunks, 49,125,889 bytes |
| Branches `claude-pass` (`77116e6`), `grok-premium-pass` (`33e0687`), `touch-one-claude` (`ae0b441`) | all 1.3.31 in `source/` | `touch-one-claude` adds the workflow that unpacks the nested ZIP |

### Provenance of 1.4.0 (verified)
1. The chunks reassemble to `files (4).zip`, SHA-256 `69fee5c18b06df00a522b725f27282a9be9ed8933be5c82ef23cb47871850b99`, matching the manifest.
2. That ZIP holds `Touch-One-1.4.0.zip` (SHA-256 `dcd10275444ed706ae7d76d7304799c365c02a9918ba1b44a1d81bb321d5ca72`, 120 entries, entry dates 2026-10-04 to 2026-10-15) and `preview.png`.
3. The inner archive is a snapshot of the repository itself (`Touch-One-main/`) plus the 1.4.0 changes, with `MILESTONE-1.4.0.md` ("premium pass") describing them.
4. A build of exactly this source succeeded on `touch-one-claude`: run 38682588277 (2026-10-15). Main's workflow fails on 1.4.0: run 39060219211.
5. Not verified: who authored the 1.4.0 changes beyond the "Touch Developer push" commits, and whether the 1.4.0 APK was ever tested on a device. The owner should confirm 1.4.0 is the version they want.

### 1.3.31 vs 1.4.0 differences
- `app/build.gradle`: only `versionCode 38 to 39` and `versionName "1.3.31" to "1.4.0"`. `AndroidManifest.xml` and `settings.gradle` identical (same package `com.touchone.game`, same icon and theme).
- `MainActivity.java`: 1070 lines to 64 lines. Rendering and game flow move into the new `TableView.java` (2264 lines), plus `CardArt.java` (322), `Gfx.java` (72), `Sfx.java` (71), `Theme.java` (69). `Engine.java` 242 to 265 lines. **This is a substantial rewrite, not a patch**, even though the milestone note says rules, artwork and opponents are unchanged.
- New assets: Outfit Bold font (`assets/fonts/`, SIL OFL licence included), new sounds (`sfx_confirm`, `sfx_deal`, `sfx_error`, `sfx_hold`, `sfx_tick`, `sfx_turn`, `sfx_whoosh`), regenerated versions of the other sound effects, `scripts/make_sounds.py`, `scripts/voice_originals/`.
- Removed: `sfx_opp.wav` (3,130 bytes; no reference in the 1.4.0 code).
- Unchanged: all image assets, `drawable-nodpi` (23 files each), `scripts/restore_visual_assets.py`.

## Recommended migration (preserves both versions and all history)
All steps happen on development branches; steps marked (A) need owner approval.

0. (A) Preserve 1.3.31: create an immutable tag `v1.3.31` at `main` `f65c7fd` using the same verify-then-create workflow pattern used for the hub tags (sessions cannot push tags directly).
1. Dev branch `claude/touch-one-1.4.0-source` from `main`:
   - Unpack `Touch-One-1.4.0.zip` over the repository root (overlay `source/`, `scripts/`, `MILESTONE-1.4.0.md`); delete only `source/app/src/main/res/raw/sfx_opp.wav`.
   - Keep `.touch-uploads/` and the manifest unchanged as the provenance record; record the three SHA-256 values above in the commit message.
   - Add the AutoBuild caller (`project_dir: source`, Java 17, wrapper present). Note `app/build.gradle` redirects the build directory to `/tmp/touch-one-app-build`, so the APK is not under `source/app/build`; the caller's `apk_path` must point at the redirected location (`../../../tmp/...` is not allowed), so either set `apk_path` to an absolute path after confirming the hub accepts it, or override with a `-PtouchOneBuildDir` property in a reviewed change to `build.gradle`. Resolve this on the dev branch.
2. Validate: AutoBuild build, `lintDebug` if configured, APK validation, delivery, and a package identity check (`com.touchone.game`, versionCode 39).
3. (A) Owner reviews the draft PR and tests the APK on a device (game flow, sound, win/lose screens, rotation, back button). Because `MainActivity` was rewritten, a device check is the real regression test.
4. (A) Owner approves the merge. 1.3.31 stays reachable at tag `v1.3.31` and in git history. Main's current ZIP-based workflow is replaced by the AutoBuild caller only after the new build matches.
5. Later: retire the chunked ZIP only with explicit approval; it is history and can stay.

## Risks
- Large rewrite (about 2,800 new lines) not covered by automated tests.
- Performance on low-end devices (cached bitmaps, one game clock) untested here.
- The build directory redirect to `/tmp` is fragile; fix as part of step 1.
- ~47 MB of source will be added to git history (private/public status of Touch-One: public).
