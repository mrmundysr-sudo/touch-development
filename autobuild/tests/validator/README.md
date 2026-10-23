# Validator tests

`run-tests.sh` builds small signed APKs with the runner's Android SDK, extracts the `validate` step from
`.github/workflows/android-build.yml`, and runs it against each fixture. The `Validator tests` workflow runs it on every
change to the shared workflow or these files.

Must pass: a valid APK with its own icon, and a valid APK whose manifest references a framework icon (which makes
`aapt dump badging` exit non-zero although the APK is fine).
Must fail: truncated, byte-corrupted, tampered-after-signing, unsigned, empty and missing files; a missing
`AndroidManifest.xml` or `classes.dex`; and any `aapt` error other than the one known framework-reference message.
The v1.0.0 validator is run too, to document the bug being fixed.
