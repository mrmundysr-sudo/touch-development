# Touch Development

Private master infrastructure repository for the Touch app family.

This repository is the durable source of truth for development workflow shared across Touch projects. Individual app repositories contain app-specific source and specifications; universal development behavior belongs here.

## Required workflow

Before working on a Touch project, read `CHATGPT_DEVELOPMENT_WORKFLOW.md` and `TOUCH_DEVELOPMENT_STANDARD.md`.

GitHub is the source of truth for source code. GitHub Actions is the standard Android build path. OpenHands/DeepSeek is an optional alternate development path chosen by the owner, not an automatic fallback.

## Project model

Touch Development (this repository)
- universal development rules
- GitHub/ChatGPT workflow
- Android CI/build templates
- handoff standards
- reusable standards and components

Individual repositories
- app-specific source
- app-specific assets
- app-specific specifications
- a pointer back to this master repository
