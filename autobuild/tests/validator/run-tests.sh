#!/usr/bin/env bash
# Tests the APK validation step of .github/workflows/android-build.yml.
# Builds small real APKs with the runner's Android SDK, extracts the shipped "validate" step
# from the workflow file, and runs it against each fixture.
#   PASS cases must be accepted, FAIL cases must be rejected.
# Also runs the v1.0.0 validator (OLD_REF) to document the behaviour that was fixed.
set -uo pipefail

WORKFLOW="${WORKFLOW:-.github/workflows/android-build.yml}"
OLD_REF="${OLD_REF:-1926b52b24140572a299004a023f692f9357b83e}"
: "${ANDROID_HOME:?ANDROID_HOME must point at an Android SDK}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

BT="$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
AJAR="$(find "$ANDROID_HOME/platforms" -name android.jar | sort -V | tail -n 1)"
echo "build-tools: $BT"; echo "android.jar: $AJAR"

extract_step() { # <workflow-file-or-stdin> -> script on stdout
  python3 - "$1" <<'PY'
import sys
try:
    import yaml
except ImportError:
    import subprocess; subprocess.check_call([sys.executable, "-m", "pip", "install", "-q", "pyyaml"]); import yaml
d = yaml.safe_load(open(sys.argv[1]))
for st in d["jobs"]["build"]["steps"]:
    if st.get("id") == "validate":
        print(st["run"]); break
else:
    sys.exit("validate step not found")
PY
}

extract_step "$WORKFLOW" > "$WORK/new.sh" || exit 2
git show "$OLD_REF:$WORKFLOW" > "$WORK/old.yml" 2>/dev/null && extract_step "$WORK/old.yml" > "$WORK/old.sh" || rm -f "$WORK/old.sh"

# ---- fixtures -------------------------------------------------------------------------
KS="$WORK/test.keystore"
keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias t -keyalg RSA -keysize 2048 \
  -validity 3650 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1

build_apk() { # <name> <icon-attr> <with-res-icon yes|no>
  local n=$1 icon=$2 d="$WORK/$1.src"
  mkdir -p "$d/res/drawable" "$d/classes"
  cat > "$d/AndroidManifest.xml" <<M
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.valid">
  <application android:label="Fixture" $icon/>
</manifest>
M
  cat > "$d/res/drawable/ic.xml" <<'X'
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="#FF0000"/></shape>
X
  echo 'public class A { }' > "$d/A.java"
  javac --release 8 -cp "$AJAR" -d "$d/classes" "$d/A.java" 2>/dev/null
  "$BT/d8" --lib "$AJAR" --output "$d" "$d/classes/A.class" >/dev/null 2>&1
  "$BT/aapt2" compile --dir "$d/res" -o "$d/res.zip" >/dev/null
  "$BT/aapt2" link -o "$d/base.apk" -I "$AJAR" --manifest "$d/AndroidManifest.xml" -R "$d/res.zip" \
     --min-sdk-version 24 --target-sdk-version 34 >/dev/null
  ( cd "$d" && zip -q base.apk classes.dex )
  "$BT/zipalign" -f 4 "$d/base.apk" "$d/aligned.apk"
  cp "$d/aligned.apk" "$WORK/$n.unsigned.apk"
  "$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out "$WORK/$n.apk" "$d/aligned.apk"
}
build_apk valid_own_icon 'android:icon="@drawable/ic"'
build_apk valid_framework_icon 'android:icon="@android:drawable/sym_def_app_icon"'

cp "$WORK/valid_own_icon.unsigned.apk" "$WORK/unsigned.apk"
SZ=$(stat -c%s "$WORK/valid_own_icon.apk")
head -c $((SZ / 2)) "$WORK/valid_own_icon.apk" > "$WORK/truncated.apk"
: > "$WORK/empty.apk"
cp "$WORK/valid_own_icon.apk" "$WORK/bitflip.apk"
printf '\xff\xff\xff\xff' | dd of="$WORK/bitflip.apk" bs=1 seek=$((SZ / 3)) conv=notrunc status=none
# tampered after signing: replace classes.dex with different bytes
cp "$WORK/valid_own_icon.apk" "$WORK/tampered.apk"
( cd "$WORK" && mkdir -p tam && cd tam && head -c 4096 /dev/urandom > classes.dex && zip -q ../tampered.apk classes.dex )
# structure cases: signature check bypassed by a shim so the structure checks are what reject
cp "$WORK/valid_own_icon.apk" "$WORK/no_manifest.apk";  zip -q -d "$WORK/no_manifest.apk" AndroidManifest.xml
cp "$WORK/valid_own_icon.apk" "$WORK/no_dex.apk";       zip -q -d "$WORK/no_dex.apk" classes.dex
mkdir -p "$WORK/shim/build-tools/99.0.0"
for t in aapt aapt2 zipalign; do ln -s "$BT/$t" "$WORK/shim/build-tools/99.0.0/$t"; done
printf '#!/bin/sh\necho Verifies\nexit 0\n' > "$WORK/shim/build-tools/99.0.0/apksigner"; chmod +x "$WORK/shim/build-tools/99.0.0/apksigner"
# an aapt error other than the known framework-reference message must still fail
printf '#!/bin/sh\necho "AndroidManifest.xml:3: error: ERROR parsing XML: broken" >&2\nexit 1\n' > "$WORK/shim/aapt-bad"; chmod +x "$WORK/shim/aapt-bad"
mkdir -p "$WORK/shim2/build-tools/99.0.0"
ln -s "$BT/apksigner" "$WORK/shim2/build-tools/99.0.0/apksigner" 2>/dev/null || cp "$BT/apksigner" "$WORK/shim2/build-tools/99.0.0/apksigner"
cp -r "$BT/lib" "$WORK/shim2/build-tools/99.0.0/lib" 2>/dev/null || true
cp "$WORK/shim/aapt-bad" "$WORK/shim2/build-tools/99.0.0/aapt"

# ---- runner ----------------------------------------------------------------------------
FAILS=0
run_case() { # <script> <apk> <expect pass|fail> <label> [android_home]
  local script=$1 apk=$2 expect=$3 label=$4 home=${5:-$ANDROID_HOME}
  local out rc
  out="$(APK="$apk" ART=fixture REF_NAME=test GITHUB_SHA=0000000000000 GITHUB_STEP_SUMMARY="$WORK/summary.md" \
         ANDROID_HOME="$home" bash -c "$(cat "$script")" 2>&1)"; rc=$?
  local got=fail; [ $rc -eq 0 ] && got=pass
  if [ "$got" = "$expect" ]; then echo "ok   $label: $got as expected"; else echo "FAIL $label: expected $expect, got $got"; echo "$out" | tail -n 15 | sed 's/^/     | /'; FAILS=$((FAILS+1)); fi
  LAST_OUT="$out"
}

echo "== candidate validator =="
run_case "$WORK/new.sh" "$WORK/valid_own_icon.apk"       pass "valid APK, own icon"
grep -q "package: name='com.example.valid'" <<<"$LAST_OUT" && echo "ok   package identity reported" || { echo "FAIL package line missing"; FAILS=$((FAILS+1)); }
run_case "$WORK/new.sh" "$WORK/valid_framework_icon.apk" pass "valid APK, framework icon (the app-visual-builder case)"
run_case "$WORK/new.sh" "$WORK/truncated.apk"            fail "truncated APK"
run_case "$WORK/new.sh" "$WORK/bitflip.apk"              fail "corrupted bytes inside APK"
run_case "$WORK/new.sh" "$WORK/tampered.apk"             fail "tampered after signing"
run_case "$WORK/new.sh" "$WORK/unsigned.apk"             fail "unsigned APK"
run_case "$WORK/new.sh" "$WORK/empty.apk"                fail "empty file"
run_case "$WORK/new.sh" "$WORK/missing.apk"              fail "missing file"
run_case "$WORK/new.sh" "$WORK/no_manifest.apk"          fail "no AndroidManifest.xml (signature check bypassed by shim)" "$WORK/shim"
run_case "$WORK/new.sh" "$WORK/no_dex.apk"               fail "no classes.dex (signature check bypassed by shim)" "$WORK/shim"
run_case "$WORK/new.sh" "$WORK/valid_own_icon.apk"       fail "unrelated aapt error still rejected" "$WORK/shim2"

if [ -s "$WORK/old.sh" ]; then
  echo "== v1.0.0 validator (documents the fixed behaviour) =="
  run_case "$WORK/old.sh" "$WORK/valid_own_icon.apk"       pass "v1.0.0 accepts valid APK, own icon"
  run_case "$WORK/old.sh" "$WORK/valid_framework_icon.apk" fail "v1.0.0 rejects framework-icon APK (the bug)"
else
  echo "note: v1.0.0 validator not available (shallow checkout); skipping comparison"
fi

echo; [ "$FAILS" -eq 0 ] && echo "ALL VALIDATOR TESTS PASSED" || echo "$FAILS VALIDATOR TEST(S) FAILED"
exit "$FAILS"
