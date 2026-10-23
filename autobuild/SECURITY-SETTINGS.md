# Recommended GitHub settings before portfolio-wide unattended development

These are owner-side settings. Claude does not change repository settings and does not run the apply script.

## What the rules do, and what they cannot do

Claude Code Cloud pushes and opens PRs **with your GitHub identity**, so a repository rule cannot tell your actions from Claude's. Consequences:

| Protection | Effective against | Not effective against |
|---|---|---|
| Ruleset on the default branch: PR required, no force-push, no deletion (`main-protection.json`) | accidental direct pushes and history rewrites to `main`, by anyone | a PR merged with your own identity (including by Claude) |
| Tag ruleset: `v*.*.*` and `apk-*` cannot be updated, moved or deleted (`stable-tags.json`) | moved or deleted stable tags, by anyone including Claude | creating a **new** tag |
| Claude's instructions (`AUTOBUILD.md`, per-repo `CLAUDE.md`) and the session permission mode | merges, tags, releases, secrets | nothing technical: they are rules Claude follows |
| A required approving review from a **second** identity (another GitHub account, or a GitHub App you control) | merges of PRs authored by your identity | needs that second identity; with `required_approving_review_count: 1` and a single account, nothing could ever merge |

So the shipped rulesets set the approving-review count to 0: they force changes through a PR (visible, reviewable, revertable) and make stable tags immutable. If you want a technical merge gate, add a second reviewer identity and raise the count to 1.

The immutable `v*.*.*` rule means `v1.0.0` can never be moved. The moving `v1` pointer is not covered by the pattern, so moving it still needs only your approval.

## Findings from the 2026-10-23 dry run (all 22 repositories)

* **Plan:** the GitHub API answers `Upgrade to GitHub Pro or make this repository public` for rulesets and branch protection on every private repository, so the account is on a plan without them. Rulesets can be created on the **9 public** repositories (touch-development, TouchV, Touch-Toolbox, Touch-Parchis, Touch-Developer-, Touch_Reader, Touch-One, fresh-start-touch-wipe-out, Touch-Modules). The **13 private** repositories cannot be protected by rulesets at all until the plan changes or they are made public.
* **Existing settings:** no rulesets and no classic branch protection exist on any public repository; nothing to merge with or replace.
* **Conflict with how work lands today:** commits reach `main` directly, not through PRs: the Touch Developer app ("Touch Developer push": TouchV, Touch_Reader, Touch-One) and earlier automation. `main-protection.json` requires a pull request for every change with no bypass, so those direct pushes would be rejected. Before applying it to a repository whose `main` is fed by the app, either switch that flow to a branch plus PR, or add a bypass for the repository admin role (which also lets any session using your identity push directly).
* **Tag rules** (`stable-tags.json`) have no such conflict: existing workflows only create `apk-*` tags and never update or delete them.

## Plan limits (verify on your billing page)

* Public repositories: branch rules and rulesets are available on every plan.
* Private repositories: GitHub only enforces rulesets and branch protection on paid plans (Pro or above). On a Free plan the same rules can be created for private repos but are not enforced. If you are on Free, either upgrade or treat private repos as protected by instructions only.

## Applying (you run this)

```bash
# dry run (default): shows what would be created
autobuild/security/apply-rulesets.sh mrmundysr-sudo/touch-development mrmundysr-sudo/touch-clean
# apply
autobuild/security/apply-rulesets.sh --apply mrmundysr-sudo/touch-development
```

Suggested order: the hub first, then each repository as it is onboarded. Enabling a PR-required ruleset on a repo means direct pushes to its `main` start failing: existing `main` workflows are unaffected (they only trigger on pushes that arrive via merged PRs).

## Other settings worth setting

* Settings > Actions > General > Workflow permissions: **Read repository contents** by default (AutoBuild callers declare their own `permissions:`).
* Settings > Actions > General > Actions permissions: allow actions from GitHub-owned and verified creators only, if you do not need others.
* Settings > Code security > Dependabot **version updates** for `github-actions`: keeps workflow action versions current and bumps SHA-pinned reusable workflows (`@<sha> # v1.0.0`) through reviewable PRs.
* Pin third-party actions to commit SHAs (the shared workflow uses `actions/*@v4` and `gradle/actions/setup-gradle@v4` tags today; Dependabot can convert and maintain them).
* Fine-grained limits for the Claude GitHub App: Contents, Pull requests, Actions, Workflows read/write on the chosen repos; nothing for Secrets, Administration or Settings.
