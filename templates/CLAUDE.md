# Claude instructions (Touch AutoBuild)

This repository follows the shared Touch AutoBuild process. Read
https://github.com/mrmundysr-sudo/touch-development/blob/main/AUTOBUILD.md first.

Short version: work only on a `claude/<slug>` branch, push to trigger the
AutoBuild workflow, read failures, repair up to 3 times, then report. Never merge
to `main`, publish releases, touch signing material or secrets, or delete anything
without the owner's explicit approval.
