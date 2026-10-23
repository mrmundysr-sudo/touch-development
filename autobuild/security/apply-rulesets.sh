#!/usr/bin/env bash
# Applies the AutoBuild rulesets to repositories YOU choose. Run it yourself with your own
# authenticated `gh` (an admin token). Claude Code Cloud does not run it.
# Usage:  apply-rulesets.sh [--apply] owner/repo [owner/repo ...]
# Without --apply it only prints what it would do (dry run) and lists existing rulesets.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
APPLY=false
if [ "${1:-}" = "--apply" ]; then APPLY=true; shift; fi
[ "$#" -gt 0 ] || { echo "usage: $0 [--apply] owner/repo [owner/repo ...]"; exit 2; }
for repo in "$@"; do
  echo "== $repo ($(gh api "repos/$repo" --jq .visibility))"
  gh api "repos/$repo/rulesets" --jq '.[] | "   existing: \(.name) [\(.target), \(.enforcement)]"' || true
  for f in main-protection stable-tags; do
    name="$(jq -r .name "$DIR/$f.json")"
    if gh api "repos/$repo/rulesets" --jq '.[].name' | grep -qxF "$name"; then
      echo "   skip $f (already present)"; continue
    fi
    if $APPLY; then
      gh api -X POST "repos/$repo/rulesets" --input "$DIR/$f.json" --jq '"   created \(.name) (id \(.id))"'
    else
      echo "   would create: $name"
    fi
  done
done
