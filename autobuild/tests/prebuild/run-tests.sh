#!/usr/bin/env bash
# Tests the optional prebuild step of .github/workflows/android-build.yml: path validation, the
# minimal environment (no secrets), exit-code and infrastructure-error reporting.
set -uo pipefail
WORKFLOW="${WORKFLOW:-.github/workflows/android-build.yml}"
WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
python3 - "$WORKFLOW" > "$WORK/step.sh" <<'PY'
import sys
try:
    import yaml
except ImportError:
    import subprocess; subprocess.check_call([sys.executable, "-m", "pip", "install", "-q", "pyyaml"]); import yaml
d = yaml.safe_load(open(sys.argv[1]))
for st in d["jobs"]["build"]["steps"]:
    if st.get("id") == "prebuild":
        assert st.get("continue-on-error") is True, "prebuild must be continue-on-error so the result job classifies it"
        print(st["run"]); break
else:
    sys.exit("prebuild step not found")
PY
[ -s "$WORK/step.sh" ] || exit 2

WS="$WORK/ws"; mkdir -p "$WS/.autobuild" "$WS/tools" "$WORK/outside"
git -C "$WS" init -q; git -C "$WS" config user.email t@t; git -C "$WS" config user.name t
cat > "$WS/.autobuild/ok.sh" <<'S'
#!/usr/bin/env bash
echo "ran ok in $(pwd) project=$AUTOBUILD_PROJECT_DIR"
mkdir -p "$AUTOBUILD_PROJECT_DIR"; touch "$AUTOBUILD_PROJECT_DIR/created"
S
cat > "$WS/.autobuild/env.sh" <<'S'
#!/usr/bin/env bash
bad=0
for v in FAKE_SECRET GITHUB_TOKEN ACTIONS_RUNTIME_TOKEN ACTIONS_ID_TOKEN_REQUEST_TOKEN GITHUB_ENV GITHUB_OUTPUT GITHUB_STEP_SUMMARY INPUT_TOKEN; do
  if [ -n "${!v-}" ]; then echo "LEAK: $v is visible"; bad=1; fi
done
[ -n "${PATH-}" ] && [ -n "${JAVA_HOME-}" ] && [ -n "${ANDROID_HOME-}" ] || { echo "MISSING required variable"; bad=1; }
exit $bad
S
printf '#!/usr/bin/env bash\necho "ETIMEDOUT while fetching"; exit 1\n' > "$WS/.autobuild/infra.sh"
printf '#!/usr/bin/env bash\necho "compile problem"; exit 1\n' > "$WS/.autobuild/fail.sh"
echo 'echo untracked' > "$WS/.autobuild/untracked.sh"
echo 'echo outside' > "$WORK/outside/evil.sh"
ln -s "$WORK/outside/evil.sh" "$WS/.autobuild/link.sh"
ln -s "$WORK/outside" "$WS/tools/linkdir"
mkdir -p "$WS/.autobuild/dir.sh"; touch "$WS/.autobuild/dir.sh/x"
cp "$WS/.autobuild/ok.sh" "$WS/.autobuild/a b.sh"
cp "$WS/.autobuild/ok.sh" "$WS/.autobuild/semi;touch.sh"
git -C "$WS" add .autobuild/ok.sh .autobuild/env.sh .autobuild/infra.sh .autobuild/fail.sh .autobuild/link.sh tools/linkdir ".autobuild/a b.sh" ".autobuild/semi;touch.sh" .autobuild/dir.sh/x
git -C "$WS" commit -qm fixtures

FAILS=0
run_case() { # <script path> <expect pass|fail> <label> [extra check cmd]
  local path=$1 expect=$2 label=$3 out rc got
  : > "$WORK/gh_output"
  out="$(cd "$WS" && FAKE_SECRET=s3cret GITHUB_TOKEN=ghs_x ACTIONS_RUNTIME_TOKEN=rt ACTIONS_ID_TOKEN_REQUEST_TOKEN=idt \
        GITHUB_ENV="$WORK/gh_env" GITHUB_OUTPUT="$WORK/gh_output" GITHUB_STEP_SUMMARY="$WORK/summary" INPUT_TOKEN=tok \
        JAVA_HOME=${JAVA_HOME:-/opt/jdk} ANDROID_HOME=${ANDROID_HOME:-/opt/android} RUNNER_TEMP="$WORK" GITHUB_WORKSPACE="$WS" \
        PREBUILD_SCRIPT="$path" PROJECT_DIR="generated-app" bash "$WORK/step.sh" 2>&1)"; rc=$?
  got=fail; [ $rc -eq 0 ] && got=pass
  if [ "$got" = "$expect" ]; then echo "ok   $label: $got as expected"; else echo "FAIL $label: expected $expect, got $got"; echo "$out" | tail -n 8 | sed 's/^/     | /'; FAILS=$((FAILS+1)); fi
  LAST_OUT="$out"
}

run_case ".autobuild/ok.sh"        pass "valid tracked script runs"
[ -f "$WS/generated-app/created" ] && echo "ok   script ran in the workspace and created the project dir" || { echo "FAIL script side effect missing"; FAILS=$((FAILS+1)); }
grep -q "sha256" <<<"$LAST_OUT" && echo "ok   script hash logged" || { echo "FAIL no hash logged"; FAILS=$((FAILS+1)); }
run_case ".autobuild/env.sh"       pass "no secrets or runner control variables are visible to the script"
run_case ".autobuild/fail.sh"      fail "failing script fails the step"
grep -q infra_error "$WORK/gh_output" && { echo "FAIL plain failure flagged as infrastructure"; FAILS=$((FAILS+1)); } || echo "ok   plain failure is not flagged as infrastructure"
run_case ".autobuild/infra.sh"     fail "network-looking failure fails the step"
grep -q "infra_error=true" "$WORK/gh_output" && echo "ok   network error flagged as infrastructure" || { echo "FAIL infra_error not set"; FAILS=$((FAILS+1)); }
run_case "../outside/evil.sh"      fail "path traversal rejected"
run_case ".autobuild/../.autobuild/ok.sh" fail "embedded .. rejected"
run_case "/etc/passwd"             fail "absolute path rejected"
run_case ".autobuild/a b.sh"       fail "space in path rejected"
run_case ".autobuild/semi;touch.sh" fail "shell metacharacters rejected"
run_case '$(touch PWNED)'          fail "command substitution rejected"
[ -e "$WS/PWNED" ] && { echo "FAIL command substitution executed"; FAILS=$((FAILS+1)); } || echo "ok   nothing was executed from the input string"
run_case ".autobuild/untracked.sh" fail "untracked file rejected"
run_case ".autobuild/missing.sh"   fail "missing file rejected"
run_case ".autobuild/dir.sh"       fail "directory rejected"
run_case ".autobuild/link.sh"      fail "symlink to a file outside the workspace rejected"
run_case "tools/linkdir/evil.sh"   fail "file reached through a symlinked directory rejected"
run_case ""                        fail "empty path rejected"

echo; [ "$FAILS" -eq 0 ] && echo "ALL PREBUILD TESTS PASSED" || echo "$FAILS PREBUILD TEST(S) FAILED"
exit "$FAILS"
