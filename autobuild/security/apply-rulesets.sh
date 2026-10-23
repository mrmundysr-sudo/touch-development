#!/usr/bin/env bash
# Applies the AutoBuild rulesets to repositories YOU choose. Run it yourself with your own
# authenticated `gh` (an admin token). Claude Code Cloud does not run it with --apply.
# Usage:  apply-rulesets.sh [--apply] owner/repo [owner/repo ...]
# Without --apply it only reports what it would do (dry run). It also reports, per repository:
#   * whether rulesets are available on the current GitHub plan (private repos need GitHub Pro),
#   * existing rulesets and classic branch protection on the default branch.
set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
APPLY=false
if [ "${1:-}" = "--apply" ]; then APPLY=true; shift; fi
[ "$#" -gt 0 ] || { echo "usage: $0 [--apply] owner/repo [owner/repo ...]"; exit 2; }
for repo in "$@"; do
  vis="$(gh api "repos/$repo" --jq .visibility 2>/dev/null || echo unknown)"
  branch="$(gh api "repos/$repo" --jq .default_branch 2>/dev/null || echo main)"
  echo "== $repo ($vis, default branch: $branch)"
  if ! out="$(gh api "repos/$repo/rulesets" 2>&1)"; then
    if grep -q "Upgrade to GitHub Pro" <<<"$out"; then
      echo "   NOT AVAILABLE: rulesets need GitHub Pro for private repositories (or make the repository public). Nothing to apply."
    else
      echo "   cannot read rulesets: $(head -c 160 <<<"$out")"
    fi
    continue
  fi
  jq -r '.[] | "   existing ruleset: \(.name) [\(.target), \(.enforcement)]"' <<<"$out"
  if prot="$(gh api "repos/$repo/branches/$branch/protection" 2>&1)"; then
    echo "   NOTE: classic branch protection already exists on $branch; review for overlap before applying."
  fi
  echo "   NOTE: main-protection requires a pull request for every change to $branch, so direct pushes stop working,"
  echo "         including pushes from the Touch Developer app and from older automation that committed straight to $branch."
  for f in main-protection stable-tags; do
    name="$(jq -r .name "$DIR/$f.json")"
    if jq -e --arg n "$name" 'any(.[]; .name == $n)' <<<"$out" >/dev/null; then
      echo "   skip $f (already present)"; continue
    fi
    if $APPLY; then
      gh api -X POST "repos/$repo/rulesets" --input "$DIR/$f.json" --jq '"   created \(.name) (id \(.id))"'
    else
      echo "   would create: $name"
    fi
  done
done
